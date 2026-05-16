package com.healthagent.config;

import com.healthagent.agent.tool.Tool;
import com.healthagent.agent.tool.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ToolRegistrationRunner implements CommandLineRunner {
    private final ToolRegistry toolRegistry;
    private final List<Tool> tools;

    public ToolRegistrationRunner(ToolRegistry toolRegistry, List<Tool> tools) {
        this.toolRegistry = toolRegistry;
        this.tools = tools;
    }

    @Override
    public void run(String... args) {
        log.info("Registering {} tools...", tools.size());
        for (Tool tool : tools) {
            toolRegistry.registerTool(tool);
            log.info("Registered tool: {}", tool.getName());
        }
    }
}
