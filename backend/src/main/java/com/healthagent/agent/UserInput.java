package com.healthagent.agent;

import lombok.Data;

import java.util.Map;

@Data
public class UserInput {
    private String userId;
    private String content;
    private Map<String, Object> metadata;
}
