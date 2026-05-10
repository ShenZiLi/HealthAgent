package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    private String message;
    
    private String model;
    
    private Long tokens;
    
    private String conversationId;
}
