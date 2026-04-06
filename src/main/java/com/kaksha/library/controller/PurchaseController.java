package com.kaksha.library.controller;

import com.kaksha.library.dto.*;
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
    
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPurchaseHistory(Authentication authentication) {
        // Note: This would need client ID from authentication
        log.info("Get purchase history for: {}", authentication.getName());
        // Implementation would retrieve client ID and fetch payments
        return ResponseEntity.ok(ApiResponse.success("Purchase history retrieved"));
    }
    
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getRecentPurchases() {
        log.info("Get recent purchases");
        List<PaymentResponse> payments = paymentService.getRecentPayments(5);
        return ResponseEntity.ok(ApiResponse.success("Recent purchases retrieved", payments));
    }
}
