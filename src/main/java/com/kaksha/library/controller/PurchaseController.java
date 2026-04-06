package com.kaksha.library.controller;

import com.kaksha.library.dto.*;
import com.kaksha.library.model.entity.Client;
import com.kaksha.library.repository.ClientRepository;
import com.kaksha.library.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
@Slf4j
public class PurchaseController {
    
    private final PaymentService paymentService;
    private final ClientRepository clientRepository;
    
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiatePurchase(
            @RequestBody InitiatePurchaseRequest request,
            Authentication authentication) {
        
        log.info("Initiate purchase for resource {} by {}", request.getResourceId(), authentication.getName());
        Map<String, Object> result = paymentService.initiatePurchase(request.getResourceId(), authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Purchase initiated", result));
    }
    
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkout(
            @RequestParam Long paymentId,
            Authentication authentication) {
        
        log.info("Checkout for payment {} by {}", paymentId, authentication.getName());
        Map<String, Object> result = paymentService.checkout(paymentId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Checkout initiated", result));
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody CampayWebhookRequest webhookRequest) {
        log.info("Received Campay webhook for reference: {}", webhookRequest.getReference());
        paymentService.processWebhook(webhookRequest);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/campay/callback/success")
    public ResponseEntity<Void> handleCampaySuccess(
            @RequestParam(value = "reference", required = false) String reference,
            @RequestParam(value = "token", required = false) String token) {
        log.info("CamPay success callback received. Reference: {}, Token: {}", reference, token);
        // The actual payment status update is handled by webhook
        // This endpoint redirects user back to application
        return ResponseEntity.status(302)
                .header("Location", "/purchases.html?status=success")
                .build();
    }
    
    @GetMapping("/campay/callback/failure")
    public ResponseEntity<Void> handleCampayFailure(
            @RequestParam(value = "reference", required = false) String reference,
            @RequestParam(value = "token", required = false) String token) {
        log.info("CamPay failure callback received. Reference: {}, Token: {}", reference, token);
        return ResponseEntity.status(302)
                .header("Location", "/purchases.html?status=failed")
                .build();
    }
    
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPurchaseHistory(Authentication authentication) {
        log.info("Get purchase history for: {}", authentication.getName());
        
        Client client = clientRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        
        List<PaymentResponse> payments = paymentService.getClientPayments(client.getUserID());
        return ResponseEntity.ok(ApiResponse.success("Purchase history retrieved", payments));
    }
    
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getRecentPurchases() {
        log.info("Get recent purchases");
        List<PaymentResponse> payments = paymentService.getRecentPayments(5);
        return ResponseEntity.ok(ApiResponse.success("Recent purchases retrieved", payments));
    }
}
