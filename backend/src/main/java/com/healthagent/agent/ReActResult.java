package com.healthagent.agent;

import lombok.Data;

import java.util.List;

@Data
public class ReActResult {
    private String finalAnswer;
    private List<ReActStep> steps;
}
