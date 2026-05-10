package com.healthagent.service.chat;

import lombok.extern.slf4j.Slf4j;

/**
 * 聊天客户端工厂
 * 根据配置创建对应的LLM客户端实例
 */
@Slf4j
public class ChatClientFactory {

    private ChatClientFactory() {
    }

    /**
     * 根据模型类型创建聊天客户端
     *
     * @param provider 模型提供商（glm / qwen）
     * @param apiKey   API密钥
     * @param baseUrl  基础URL
     * @param model    模型名称
     * @return 聊天客户端实例
     */
    public static AbstractChatClient createClient(String provider, String apiKey, String baseUrl, String model) {
        if (provider == null) {
            provider = "glm";
        }

        switch (provider.toLowerCase()) {
            case "glm":
                log.info("创建GLM聊天客户端, model={}, baseUrl={}", model, baseUrl);
                return new GlmChatClient(apiKey, baseUrl, model);
            case "qwen":
            case "dashscope":
                log.info("创建通义千问聊天客户端, model={}, baseUrl={}", model, baseUrl);
                return new QwenChatClient(apiKey, baseUrl, model);
            default:
                log.warn("未知的模型提供商: {}, 使用GLM作为默认", provider);
                return new GlmChatClient(apiKey, baseUrl, model);
        }
    }
}
