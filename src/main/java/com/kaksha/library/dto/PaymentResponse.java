package com.kaksha.library.dto;

import com.kaksha.library.model.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    
    private Long paymentID;
    private String paymentDetails;
    private String paymentInvoice;
    private String transactionReference;
    private String paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private String resourceTitle;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
