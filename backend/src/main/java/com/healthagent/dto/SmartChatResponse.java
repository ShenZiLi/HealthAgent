package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartChatResponse {
    
    private String message;
    
    private String intent;
    
    private String action;
    
    private Object data;
    
    private Boolean needsMoreInfo;
    
    private String messageType;
}
