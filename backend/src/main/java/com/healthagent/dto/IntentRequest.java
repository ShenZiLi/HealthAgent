package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentRequest {
    
    private String message;
    
    private String userId;
    
    private String conversationId;
}
