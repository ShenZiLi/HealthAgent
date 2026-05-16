package com.healthagent.agent.tool;

public interface Tool {
    String getName();
    String getDescription();
    ToolSchema getSchema();
    ToolResult execute(ToolInput input);
    default boolean requiresConfirmation() {
        return false;
    }
}
