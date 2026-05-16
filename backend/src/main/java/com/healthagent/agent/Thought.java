package com.healthagent.agent;

import lombok.Data;

import java.util.Map;

@Data
public class Thought {
    private String content;
    private boolean finish;
    private String finishAnswer;
    private String actionName;
    private Map<String, Object> actionParameters;
}
