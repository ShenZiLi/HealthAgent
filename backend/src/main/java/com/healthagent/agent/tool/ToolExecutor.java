package com.healthagent.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ToolExecutor {
    private final ToolRegistry toolRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public ToolResult execute(String toolName, Map<String, Object> parameters) {
        Tool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            log.error("Tool not found: {}", toolName);
            return ToolResult.error("工具不存在: " + toolName);
        }

        try {
            ToolInput input = new ToolInput();
            input.setParameters(parameters);
            log.info("Executing tool: {}, params: {}", toolName, parameters);
            return tool.execute(input);
        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            return ToolResult.error("工具执行失败: " + e.getMessage());
        }
    }
}
