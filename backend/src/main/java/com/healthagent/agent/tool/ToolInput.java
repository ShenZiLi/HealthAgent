package com.healthagent.agent.tool;

import lombok.Data;
import java.util.Map;

@Data
public class ToolInput {
    private Map<String, Object> parameters;
}
