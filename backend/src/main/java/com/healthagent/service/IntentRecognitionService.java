package com.healthagent.service;

import com.healthagent.common.IntentType;
import com.healthagent.service.chat.AbstractChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 意图识别服务
 * 识别用户意图：1.查询保单 2.体检预约 3.健康咨询
 */
@Slf4j
@Service
public class IntentRecognitionService {

    private AbstractChatClient chatClient;

    public IntentRecognitionService(
            @Value("${healthagent.glm.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://open.bigmodel.cn}") String baseUrl,
            @Value("${healthagent.chat.model:glm-4}") String model,
            @Value("${healthagent.chat.provider:glm}") String provider) {
        this.chatClient = com.healthagent.service.chat.ChatClientFactory.createClient(provider, apiKey, baseUrl, model);
    }

    /**
     * 识别用户意图
     *
     * @param userMessage 用户消息
     * @return 意图类型
     */
    public IntentType recognizeIntent(String userMessage) {
        try {
            String result = chatClient.chat(
                    buildIntentPrompt(userMessage),
                    "你是一个意图识别助手，请只返回意图代码，不需要其他解释。可选值：query_policy, book_examination, query_booking, health_consultation, general_conversation",
                    null
            );
            log.info("意图识别结果: {}", result);
            return IntentType.fromCode(result);
        } catch (Exception e) {
            log.error("意图识别失败，使用默认意图", e);
            return IntentType.GENERAL_CONVERSATION;
        }
    }

    private String buildIntentPrompt(String userMessage) {
        return """
            请分析以下用户消息，判断用户的意图。
            
            用户消息: %s
            
            可选意图类型:
            - query_policy: 用户想查询保单信息（包含"保单"、"保险"、"理赔"等关键词）
            - book_examination: 用户想预约体检（包含"体检"、"预约"、"检查"等关键词）
            - query_booking: 用户想查询体检预约记录（包含"预约记录"、"我的预约"、"预约情况"、"预约列表"等关键词）
            - health_consultation: 用户想进行健康咨询（包含"健康"、"症状"、"疾病"、"怎么办"等关键词）
            - general_conversation: 一般对话、闲聊、问候等
            
            请只返回一个意图代码，不需要其他解释。
            """.formatted(userMessage);
    }
}
