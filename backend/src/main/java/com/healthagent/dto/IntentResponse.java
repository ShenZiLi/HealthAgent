package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentResponse {
    
    private String intent;
    
    private String action;
    
    private String parameters;
    
    private Double confidence;
    
    private String message;
}
