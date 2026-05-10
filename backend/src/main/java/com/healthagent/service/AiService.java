package com.healthagent.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI服务 - 使用zai-sdk封装GLM-4.7-Flash模型调用
 */
@Service
@Slf4j
public class AiService {

    private final ZhipuAiClient client;
    private final boolean enabled;
    private final SessionManager sessionManager;

    @Value("${healthagent.chat.model:glm-4.7-flash}")
    private String model;

    @Autowired
    public AiService(@Value("${healthagent.glm.api-key:}") String apiKey, SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GLM_API_KEY 未配置，AI服务将不可用");
            this.client = null;
            this.enabled = false;
        } else {
            this.client = ZhipuAiClient.builder().ofZHIPU()
                    .apiKey(apiKey)
                    .build();
            this.enabled = true;
            log.info("GLM-4.7-Flash AI服务初始化完成，模型: {}", model);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 简单文本问答
     *
     * @param message 用户输入的消息
     * @return AI返回的响应
     */
    public String chat(String message) {
        if (!enabled) {
            throw new IllegalStateException("AI服务未配置，请设置 GLM_API_KEY 环境变量");
        }
        
        log.info("AI聊天请求: {}", message);
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER.value())
                    .content(message)
                    .build());
            
            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model(model)
                    .messages(messages)
                    .build();
            
            ChatCompletionResponse response = client.chat().createChatCompletion(request);
            
            if (response.isSuccess() && response.getData() != null && response.getData().getChoices() != null && !response.getData().getChoices().isEmpty()) {
                String content = (String) response.getData().getChoices().get(0).getMessage().getContent();
                log.info("AI聊天响应: {}", content);
                return content;
            } else {
                String error = response.getMsg();
                log.error("AI聊天请求失败: {}", error);
                throw new RuntimeException("AI服务调用失败: " + error);
            }
        } catch (Exception e) {
            log.error("AI聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带有系统提示词的问答
     *
     * @param systemPrompt 系统提示词，定义AI的角色和行为
     * @param userMessage  用户输入的消息
     * @return AI返回的响应
     */
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        if (!enabled) {
            throw new IllegalStateException("AI服务未配置，请设置 GLM_API_KEY 环境变量");
        }
        
        log.info("AI聊天请求(带系统提示): {}", userMessage);
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM.value())
                    .content(systemPrompt)
                    .build());
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER.value())
                    .content(userMessage)
                    .build());
            
            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model(model)
                    .messages(messages)
                    .build();
            
            ChatCompletionResponse response = client.chat().createChatCompletion(request);
            
            if (response.isSuccess() && response.getData() != null && response.getData().getChoices() != null && !response.getData().getChoices().isEmpty()) {
                String content = (String) response.getData().getChoices().get(0).getMessage().getContent();
                log.info("AI聊天响应: {}", content);
                return content;
            } else {
                String error = response.getMsg();
                log.error("AI聊天请求失败: {}", error);
                throw new RuntimeException("AI服务调用失败: " + error);
            }
        } catch (Exception e) {
            log.error("AI聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 健康助手专用问答 - 使用预设的健康助手系统提示词
     *
     * @param userMessage 用户输入的健康相关问题
     * @return AI返回的响应
     */
    public String healthChat(String userMessage) {
        String systemPrompt = """
            你是一位专业的健康助手AI，擅长回答各类健康相关问题。
            
            你的职责：
            1. 提供准确、科学的健康信息
            2. 解答用户关于疾病、症状、饮食、运动等方面的疑问
            3. 提供健康建议和生活方式指导
            4. 提醒用户在必要时咨询专业医生
            
            注意事项：
            - 你的回答仅供参考，不能替代专业医疗建议
            - 如果用户的问题涉及紧急医疗情况，请立即建议用户就医
            - 保持回答简洁、易懂，避免使用过于专业的术语
            
            请用友好、专业的语气回答用户的问题。
            """;
        
        return chatWithSystemPrompt(systemPrompt, userMessage);
    }

    /**
     * 多轮对话问答
     *
     * @param messages 消息列表，包含历史对话
     * @return AI返回的响应
     */
    public String chatWithHistory(List<ChatMessage> messages) {
        if (!enabled) {
            throw new IllegalStateException("AI服务未配置，请设置 GLM_API_KEY 环境变量");
        }
        
        log.info("AI多轮对话请求: {} 条消息", messages.size());
        try {
            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model(model)
                    .messages(messages)
                    .build();
            
            ChatCompletionResponse response = client.chat().createChatCompletion(request);
            
            if (response.isSuccess() && response.getData() != null && response.getData().getChoices() != null && !response.getData().getChoices().isEmpty()) {
                String content = (String) response.getData().getChoices().get(0).getMessage().getContent();
                log.info("AI多轮对话响应: {}", content);
                return content;
            } else {
                String error = response.getMsg();
                log.error("AI多轮对话请求失败: {}", error);
                throw new RuntimeException("AI服务调用失败: " + error);
            }
        } catch (Exception e) {
            log.error("AI多轮对话请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 会话聊天 - 支持持续对话带上下文
     *
     * @param sessionId 会话ID
     * @param message 用户输入的消息
     * @return AI返回的响应
     */
    public String sessionChat(String sessionId, String message) {
        if (!enabled) {
            throw new IllegalStateException("AI服务未配置，请设置 GLM_API_KEY 环境变量");
        }

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = sessionManager.createSession();
            log.info("自动创建新会话: {}", sessionId);
        }

        log.info("AI会话聊天 [会话ID: {}, 消息数: {}]: {}", sessionId, sessionManager.getMessageCount(sessionId), message);
        
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(message)
                .build();
        sessionManager.addMessage(sessionId, userMessage);

        List<ChatMessage> messages = sessionManager.getSessionHistory(sessionId);
        
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(model)
                .messages(messages)
                .build();

        try {
            ChatCompletionResponse response = client.chat().createChatCompletion(request);
            
            if (response.isSuccess() && response.getData() != null && response.getData().getChoices() != null && !response.getData().getChoices().isEmpty()) {
                String content = (String) response.getData().getChoices().get(0).getMessage().getContent();
                log.info("AI会话响应 [会话ID: {}]: {}", sessionId, content);
                
                ChatMessage assistantMessage = ChatMessage.builder()
                        .role(ChatMessageRole.ASSISTANT.value())
                        .content(content)
                        .build();
                sessionManager.addMessage(sessionId, assistantMessage);
                
                return content;
            } else {
                String error = response.getMsg();
                log.error("AI会话请求失败 [会话ID: {}]: {}", sessionId, error);
                throw new RuntimeException("AI服务调用失败: " + error);
            }
        } catch (Exception e) {
            log.error("AI会话请求失败 [会话ID: {}]: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建新会话
     *
     * @return 会话ID
     */
    public String createSession() {
        return sessionManager.createSession();
    }

    /**
     * 清除会话
     *
     * @param sessionId 会话ID
     */
    public void clearSession(String sessionId) {
        sessionManager.clearSession(sessionId);
    }

    /**
     * 获取会话历史消息数
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    public int getSessionMessageCount(String sessionId) {
        return sessionManager.getMessageCount(sessionId);
    }
}
