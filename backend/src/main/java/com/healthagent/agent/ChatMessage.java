package com.healthagent.agent;

import lombok.Data;

@Data
public class ChatMessage {
    private String role;
    private String content;
    private long timestamp;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }
}
