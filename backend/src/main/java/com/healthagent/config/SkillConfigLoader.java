package com.healthagent.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SkillConfigLoader {

    private final Map<String, SkillConfig> skillCache = new ConcurrentHashMap<>();
    private final Yaml yaml = new Yaml();

    @PostConstruct
    public void init() {
        loadSkill("skills/policy_query_skill.md");
    }

    public void loadSkill(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("Skill配置文件不存在: {}", resourcePath);
                return;
            }
            String content;
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                content = sb.toString();
            }
            SkillConfig config = parseSkillConfig(content, resourcePath);
            skillCache.put(config.getSkillName(), config);
            log.info("成功加载Skill: {} v{}", config.getSkillName(), config.getVersion());
        } catch (IOException e) {
            log.error("加载Skill配置失败: {}", resourcePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private SkillConfig parseSkillConfig(String content, String resourcePath) {
        SkillConfig config = new SkillConfig();

        String fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        config.setFileName(fileName);

        Pattern frontMatterPattern = Pattern.compile("^---\\n([\\s\\S]*?)\\n---", Pattern.MULTILINE);
        Matcher matcher = frontMatterPattern.matcher(content);

        if (matcher.find()) {
            String frontMatter = matcher.group(1);
            Map<String, Object> yamlMap = yaml.load(frontMatter);

            config.setSkillName((String) yamlMap.get("name"));
            config.setDescription((String) yamlMap.get("description"));

            Object triggerObj = yamlMap.get("trigger");
            if (triggerObj instanceof List) {
                config.setTriggers((List<String>) triggerObj);
            } else if (triggerObj instanceof String) {
                config.setTriggers(List.of(((String) triggerObj).split("[,，]")));
            }

            Object versionObj = yamlMap.get("version");
            config.setVersion(versionObj != null ? versionObj.toString() : "1.0.0");
        }

        config.setRawContent(content);

        return config;
    }

    public SkillConfig getSkill(String skillName) {
        return skillCache.get(skillName);
    }

    public SkillConfig getSkillByTrigger(String message) {
        if (message == null) {
            return null;
        }
        String lowerMessage = message.toLowerCase();
        for (SkillConfig config : skillCache.values()) {
            if (config.getTriggers() != null) {
                for (String trigger : config.getTriggers()) {
                    if (lowerMessage.contains(trigger.toLowerCase())) {
                        return config;
                    }
                }
            }
        }
        return null;
    }

    public Map<String, SkillConfig> getAllSkills() {
        return skillCache;
    }

    @Data
    public static class SkillConfig {
        private String fileName;
        private String skillName;
        private String description;
        private String version;
        private List<String> triggers;
        private String rawContent;
    }
}
