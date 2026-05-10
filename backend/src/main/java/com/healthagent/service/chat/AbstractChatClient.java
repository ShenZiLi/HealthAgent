package com.healthagent.service.chat;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM聊天客户端抽象类（模板方法模式）
 * 定义通用的聊天交互流程，子类实现具体的API调用细节
 */
@Slf4j
public abstract class AbstractChatClient {

    protected String apiKey;
    protected String baseUrl;
    protected String model;

    public AbstractChatClient(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    /**
     * 模板方法：执行聊天
     * 定义标准流程：构建消息 → 调用API → 解析响应
     */
    public String chat(String userMessage, String systemPrompt, String userId) {
        log.info("[{}] 发送聊天请求: {}", getModelName(), userMessage);
        try {
            List<ChatMessage> messages = buildMessages(systemPrompt, userMessage, userId);
            String requestBody = buildRequestBody(messages);
            String rawResponse = callApi(requestBody);
            String result = parseResponse(rawResponse);
            log.info("[{}] 聊天响应: {}", getModelName(), result);
            return result;
        } catch (Exception e) {
            log.error("[{}] 聊天请求失败", getModelName(), e);
            return buildErrorMessage(e);
        }
    }

    /**
     * 构建消息列表（可被子类覆盖）
     */
    protected List<ChatMessage> buildMessages(String systemPrompt, String userMessage, String userId) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt != null ? systemPrompt : getDefaultSystemPrompt()));
        if (userId != null && !userId.isEmpty()) {
            messages.add(new ChatMessage("system", "当前用户ID: " + userId));
        }
        messages.add(new ChatMessage("user", userMessage));
        return messages;
    }

    /**
     * 获取模型名称
     */
    public String getModelName() {
        return model;
    }

    /**
     * 获取默认系统提示词
     */
    protected String getDefaultSystemPrompt() {
        return "你是健康助手AI客服，专注于为用户提供健康保险和体检预约相关的咨询和服务。回答要专业、友好、简洁。";
    }

    /**
     * 构建默认错误消息
     */
    protected String buildErrorMessage(Exception e) {
        return "抱歉，我现在无法回答您的问题，请稍后再试。";
    }

    // ==================== 抽象方法（子类必须实现） ====================

    /**
     * 构建请求体JSON字符串
     *
     * @param messages 消息列表
     * @return 请求体JSON字符串
     */
    protected abstract String buildRequestBody(List<ChatMessage> messages);

    /**
     * 调用底层API
     *
     * @param requestBody 请求体JSON字符串
     * @return 原始响应字符串
     * @throws Exception API调用异常
     */
    protected abstract String callApi(String requestBody) throws Exception;

    /**
     * 解析API响应，提取回复内容
     *
     * @param rawResponse 原始响应字符串
     * @return 提取的回复内容
     * @throws Exception 解析异常
     */
    protected abstract String parseResponse(String rawResponse) throws Exception;

    /**
     * 消息封装类
     */
    protected static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
