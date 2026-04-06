package com.kaksha.library.dto;

import lombok.Data;

@Data
public class CampayWebhookRequest {
    
    private String reference;
    private String status;
    private String amount;
    private String currency;
    private String operator;
    private String operator_reference;
    private String signature;
}
