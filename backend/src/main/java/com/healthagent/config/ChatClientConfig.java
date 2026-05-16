package com.healthagent.config;

import com.healthagent.service.chat.AbstractChatClient;
import com.healthagent.service.chat.ChatClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Value("${healthagent.glm.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://open.bigmodel.cn}")
    private String baseUrl;

    @Value("${healthagent.chat.model:glm-4}")
    private String model;

    @Value("${healthagent.chat.provider:glm}")
    private String provider;

    @Bean
    public AbstractChatClient chatClient() {
        return ChatClientFactory.createClient(provider, apiKey, baseUrl, model);
    }
}
