package com.healthagent.agent;

import lombok.Data;

import java.util.Map;

@Data
public class Action {
    private String toolName;
    private Map<String, Object> parameters;
}
