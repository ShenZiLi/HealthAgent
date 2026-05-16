package com.healthagent.config;

import com.healthagent.service.chat.AbstractChatClient;
import com.healthagent.service.chat.ChatClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ChatClientConfig {

    @Value("${healthagent.chat.model:glm-4.6v}")
    private String model;

    @Value("${healthagent.glm.api-key:}")
    private String apiKey;

    @Value("${healthagent.chat.base-url:https://open.bigmodel.cn}")
    private String baseUrl;

    @Value("${healthagent.chat.provider:glm}")
    private String provider;

    @Bean
    public AbstractChatClient chatClient() {
        log.info("Initializing AbstractChatClient: provider={}, model={}", provider, model);
        return ChatClientFactory.createClient(provider, apiKey, baseUrl, model);
    }
}
