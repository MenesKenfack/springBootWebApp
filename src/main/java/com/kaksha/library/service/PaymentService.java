package com.kaksha.library.service;

import com.kaksha.library.config.CampayConfig;
import com.kaksha.library.dto.*;
import com.kaksha.library.exception.BadRequestException;
import com.kaksha.library.exception.PaymentException;
import com.kaksha.library.exception.ResourceNotFoundException;
import com.kaksha.library.model.entity.Client;
import com.kaksha.library.model.entity.LibraryResource;
import com.kaksha.library.model.entity.Payments;
import com.kaksha.library.model.enums.PaymentStatus;
import com.kaksha.library.repository.ClientRepository;
import com.kaksha.library.repository.ClientResourceRepository;
import com.kaksha.library.repository.LibraryResourceRepository;
import com.kaksha.library.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentsRepository paymentsRepository;
    private final ClientRepository clientRepository;
    private final LibraryResourceRepository resourceRepository;
    private final ClientResourceRepository clientResourceRepository;
    private final CampayConfig campayConfig;
    private final MailService mailService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Transactional
    public Map<String, Object> initiatePurchase(Long resourceId, String clientEmail) {
        Objects.requireNonNull(resourceId, "Resource ID cannot be null");
        log.info("Initiating purchase for resource {} by {}", resourceId, clientEmail);
        
        Client client = clientRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Client", 0L));
        
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));
        
        // Check if already purchased
        Optional<Payments> existingPayment = paymentsRepository
                .findSuccessfulPurchaseByClientAndResource(client.getUserID(), resourceId);
        
        if (existingPayment.isPresent()) {
            throw new BadRequestException("You have already purchased this resource");
        }
        
        // Create pending payment
        Payments payment = new Payments();
        payment.setClient(client);
        payment.setResource(resource);
        payment.setAmount(resource.getPrice() != null ? resource.getPrice() : BigDecimal.ZERO);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDetails("Purchase of: " + resource.getTitle());
        payment.setTransactionReference(generateTransactionReference());
        
        Payments savedPayment = paymentsRepository.save(payment);
        
        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", savedPayment.getPaymentID());
        result.put("transactionReference", savedPayment.getTransactionReference());
        result.put("amount", savedPayment.getAmount());
        result.put("status", savedPayment.getStatus());
        result.put("resourceTitle", resource.getTitle());
        
        return result;
    }
    
    @Transactional
    public Map<String, Object> checkout(Long paymentId, String clientEmail) {
        Objects.requireNonNull(paymentId, "Payment ID cannot be null");
        log.info("Processing checkout for payment {} by {}", paymentId, clientEmail);
        
        Payments payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        
        if (!payment.getClient().getEmail().equals(clientEmail)) {
            throw new BadRequestException("Invalid payment access");
        }
        
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment is not in pending state");
        }
        
        try {
            // Call Campay API to create payment session
            Map<String, Object> campayRequest = new HashMap<>();
            campayRequest.put("amount", payment.getAmount().toString());
            campayRequest.put("currency", campayConfig.getCurrency());
            campayRequest.put("description", payment.getPaymentDetails());
            campayRequest.put("external_reference", payment.getTransactionReference());
            campayRequest.put("redirect_url", campayConfig.getCallbackUrl() + "/success");
            campayRequest.put("failure_url", campayConfig.getCallbackUrl() + "/failure");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Token " + campayConfig.getApiKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(campayRequest, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    campayConfig.getBaseUrl() + "/collect/",
                    Objects.requireNonNull(HttpMethod.POST, "HTTP method cannot be null"),
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> campayResponse = Objects.requireNonNull(response.getBody(), "Response body cannot be null");
                
                payment.setCampayPaymentUrl((String) campayResponse.get("payment_url"));
                payment.setCampayToken((String) campayResponse.get("token"));
                paymentsRepository.save(payment);
                
                Map<String, Object> result = new HashMap<>();
                result.put("checkoutUrl", campayResponse.get("payment_url"));
                result.put("token", campayResponse.get("token"));
                result.put("paymentId", payment.getPaymentID());
                
                return result;
            } else {
                throw new PaymentException("Failed to create payment session with Campay");
            }
            
        } catch (Exception e) {
            log.error("Campay checkout error: {}", e.getMessage(), e);
            throw new PaymentException("Payment processing failed: " + e.getMessage());
        }
    }
    
    @Transactional
    public void processWebhook(CampayWebhookRequest webhookRequest) {
        log.info("Processing Campay webhook for reference: {}", webhookRequest.getReference());
        
        // Verify webhook signature
        if (!verifyWebhookSignature(webhookRequest)) {
            log.error("Invalid webhook signature");
            throw new BadRequestException("Invalid webhook signature");
        }
        
        Payments payment = paymentsRepository.findByTransactionReference(webhookRequest.getReference())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for reference: " + webhookRequest.getReference()));
        
        String status = webhookRequest.getStatus();
        
        if ("SUCCESSFUL".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentInvoice("INV-" + payment.getTransactionReference());
            
            // Grant resource access to client
            if (payment.getResource() != null) {
                grantResourceAccess(payment.getClient().getUserID(), payment.getResource().getResourceID());
            }
            
            // Send confirmation email
            try {
                mailService.sendPaymentConfirmationEmail(
                        payment.getClient().getEmail(),
                        payment.getResource() != null ? payment.getResource().getTitle() : "Unknown",
                        payment.getAmount().toString()
                );
            } catch (Exception e) {
                log.error("Failed to send confirmation email: {}", e.getMessage());
            }
            
        } else if ("FAILED".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.FAILED);
        } else {
            payment.setStatus(PaymentStatus.CANCELLED);
        }
        
        paymentsRepository.save(payment);
        log.info("Payment {} updated to status: {}", payment.getPaymentID(), payment.getStatus());
    }
    
    public List<PaymentResponse> getClientPayments(Long clientId) {
        return paymentsRepository.findByClientUserID(clientId)
                .stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }
    
    public List<PaymentResponse> getRecentPayments(int limit) {
        return paymentsRepository.findRecentSuccessfulPayments(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }
    
    private void grantResourceAccess(Long clientId, Long resourceId) {
        try {
            clientResourceRepository.grantAccess(clientId, resourceId);
            log.info("Granted access to resource {} for client {}", resourceId, clientId);
        } catch (Exception e) {
            log.warn("Resource access may already exist for client {} and resource {}", clientId, resourceId);
        }
    }
    
    private boolean verifyWebhookSignature(CampayWebhookRequest webhookRequest) {
        try {
            String payload = webhookRequest.getReference() + webhookRequest.getStatus() + webhookRequest.getAmount();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(campayConfig.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);
            
            return computedSignature.equals(webhookRequest.getSignature());
        } catch (Exception e) {
            log.error("Error verifying webhook signature: {}", e.getMessage());
            return false;
        }
    }
    
    private String generateTransactionReference() {
        return "KAKSHA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
    
    private PaymentResponse mapToPaymentResponse(Payments payment) {
        return PaymentResponse.builder()
                .paymentID(payment.getPaymentID())
                .paymentDetails(payment.getPaymentDetails())
                .paymentInvoice(payment.getPaymentInvoice())
                .transactionReference(payment.getTransactionReference())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .resourceTitle(payment.getResource() != null ? payment.getResource().getTitle() : null)
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
