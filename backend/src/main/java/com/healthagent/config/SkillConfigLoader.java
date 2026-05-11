package com.healthagent.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SkillConfigLoader {

    private final Map<String, SkillConfig> skillCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadSkill("skills/policy_query_skill.json");
    }

    public void loadSkill(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Skill配置文件不存在: {}", resourcePath);
                return;
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            SkillConfig config = parseSkillConfig(content);
            skillCache.put(config.getSkillName(), config);
            log.info("成功加载Skill: {} v{}", config.getSkillName(), config.getVersion());
        } catch (IOException e) {
            log.error("加载Skill配置失败: {}", resourcePath, e);
        }
    }

    private SkillConfig parseSkillConfig(String json) {
        com.alibaba.fastjson2.JSONObject obj = com.alibaba.fastjson2.JSON.parseObject(json);
        SkillConfig config = new SkillConfig();
        config.setSkillName(obj.getString("skillName"));
        config.setDescription(obj.getString("description"));
        config.setVersion(obj.getString("version"));
        config.setTriggers(obj.getJSONArray("triggers").toList(String.class));
        config.setInputSchema(obj.getJSONObject("inputSchema").toJSONString());
        config.setOutputSchema(obj.getJSONObject("outputSchema").toJSONString());
        config.setSensitiveDataConfig(obj.getJSONObject("sensitiveDataConfig").toJSONString());
        config.setResponseFormat(obj.getJSONObject("responseFormat").toJSONString());
        config.setErrorHandling(obj.getJSONObject("errorHandling").toJSONString());
        return config;
    }

    public SkillConfig getSkill(String skillName) {
        return skillCache.get(skillName);
    }

    public Map<String, SkillConfig> getAllSkills() {
        return skillCache;
    }

    @Data
    public static class SkillConfig {
        private String skillName;
        private String description;
        private String version;
        private java.util.List<String> triggers;
        private String inputSchema;
        private String outputSchema;
        private String sensitiveDataConfig;
        private String responseFormat;
        private String errorHandling;
    }
}
