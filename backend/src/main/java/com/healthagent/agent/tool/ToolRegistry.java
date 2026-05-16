package com.healthagent.agent.tool;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public List<ToolDefinition> getToolDefinitions() {
        return tools.values().stream()
                .map(this::toToolDefinition)
                .collect(Collectors.toList());
    }

    public List<Tool> getAllTools() {
        return tools.values().stream().collect(Collectors.toList());
    }

    private ToolDefinition toToolDefinition(Tool tool) {
        ToolDefinition def = new ToolDefinition();
        def.setName(tool.getName());
        def.setDescription(tool.getDescription());
        def.setParameters(tool.getSchema());
        return def;
    }

    @lombok.Data
    public static class ToolDefinition {
        private String name;
        private String description;
        private ToolSchema parameters;
    }
}
