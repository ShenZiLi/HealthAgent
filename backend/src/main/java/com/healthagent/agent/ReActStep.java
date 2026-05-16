package com.healthagent.agent;

import lombok.Data;

@Data
public class ReActStep {
    private StepType type;
    private String content;

    public ReActStep() {}

    public ReActStep(StepType type, String content) {
        this.type = type;
        this.content = content;
    }
}
