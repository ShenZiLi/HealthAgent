package com.healthagent.agent.tool;

import lombok.Data;

@Data
public class ToolResult {
    private boolean success;
    private String output;
    private String error;

    public static ToolResult success(String output) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setOutput(output);
        return result;
    }

    public static ToolResult error(String error) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }
}
