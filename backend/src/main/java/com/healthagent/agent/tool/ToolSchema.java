package com.healthagent.agent.tool;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ToolSchema {
    private String type = "object";
    private Map<String, Property> properties;
    private List<String> required;

    @Data
    public static class Property {
        private String type;
        private String description;
        private List<String> enumValues;
        private Object defaultValue;
    }
}
