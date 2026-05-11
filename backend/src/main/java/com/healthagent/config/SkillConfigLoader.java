package com.healthagent.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;

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
            SkillConfig config = parseMarkdownSkill(content, resourcePath);
            skillCache.put(config.getSkillName(), config);
            log.info("成功加载Skill: {} v{}", config.getSkillName(), config.getVersion());
        } catch (IOException e) {
            log.error("加载Skill配置失败: {}", resourcePath, e);
        }
    }

    private SkillConfig parseMarkdownSkill(String markdown, String resourcePath) {
        SkillConfig config = new SkillConfig();

        String fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        config.setFileName(fileName);

        Pattern namePattern = Pattern.compile("#\\s*Skill:\\s*(.+?)\\n");
        Matcher nameMatcher = namePattern.matcher(markdown);
        if (nameMatcher.find()) {
            config.setSkillName(nameMatcher.group(1).trim());
        }

        Pattern versionPattern = Pattern.compile("-+\\s*\\n.*?版本.*?:\\s*(.+?)\\n", Pattern.DOTALL);
        Matcher versionMatcher = versionPattern.matcher(markdown);
        if (versionMatcher.find()) {
            config.setVersion(versionMatcher.group(1).trim());
        }

        Pattern descPattern = Pattern.compile("描述.*?:\\s*(.+?)\\n");
        Matcher descMatcher = descPattern.matcher(markdown);
        if (descMatcher.find()) {
            config.setDescription(descMatcher.group(1).trim());
        }

        Pattern triggerPattern = Pattern.compile("触发词.*?:\\s*(.+?)\\n");
        Matcher triggerMatcher = triggerPattern.matcher(markdown);
        if (triggerMatcher.find()) {
            String triggers = triggerMatcher.group(1).trim();
            config.setTriggers(parseListItems(triggers));
        }

        config.setInputSchema(parseMarkdownTable(markdown, "输入参数"));
        config.setOutputSchema(parseMarkdownTable(markdown, "输出字段说明"));
        config.setDesensitizationRules(parseMarkdownTable(markdown, "敏感数据脱敏规则"));
        config.setErrorHandling(parseErrorHandling(markdown));
        config.setRawContent(markdown);

        return config;
    }

    private List<String> parseListItems(String line) {
        List<String> items = new ArrayList<>();
        String[] parts = line.split("[,，]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private String parseMarkdownTable(String markdown, String tableTitle) {
        Pattern tablePattern = Pattern.compile(
            tableTitle + "(?:\\s*\\n\\|.*?\\|\\n\\|.*?\\|([\\s\\S]*?))(?=\\n\\w|\\n---|#|$)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = tablePattern.matcher(markdown);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

    private String parseErrorHandling(String markdown) {
        Pattern errorPattern = Pattern.compile(
            "## 错误处理\\s*\\n((?:\\|.*?\\|\\n)+)",
            Pattern.DOTALL
        );
        Matcher matcher = errorPattern.matcher(markdown);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
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
        private String inputSchema;
        private String outputSchema;
        private String desensitizationRules;
        private String errorHandling;
        private String rawContent;
    }
}
