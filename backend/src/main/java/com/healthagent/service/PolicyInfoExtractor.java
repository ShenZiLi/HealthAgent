package com.healthagent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.healthagent.dto.PolicyQueryParamsDTO;
import com.healthagent.service.chat.AbstractChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 保单信息提取服务
 * 使用function calling方式从用户输入中提取保单查询参数（用户ID、身份证号、保单号）
 */
@Slf4j
@Service
public class PolicyInfoExtractor {

    private final AbstractChatClient chatClient;

    @Autowired
    public PolicyInfoExtractor(
            @Value("${healthagent.glm.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://open.bigmodel.cn}") String baseUrl,
            @Value("${healthagent.chat.model:glm-4}") String model,
            @Value("${healthagent.chat.provider:glm}") String provider) {
        this.chatClient = com.healthagent.service.chat.ChatClientFactory.createClient(
                provider, apiKey, baseUrl, model
        );
    }

    /**
     * 从用户输入中提取保单查询参数
     *
     * @param userMessage 用户输入的消息
     * @return 提取的保单查询参数
     */
    public PolicyQueryParamsDTO extractPolicyParams(String userMessage) {
        log.info("开始从用户输入中提取保单查询参数: {}", userMessage);
        
        try {
            String functionCallResult = chatClient.chatWithFunctionCall(
                    buildExtractPrompt(userMessage),
                    getExtractPolicyParamsFunction(),
                    null
            );

            if (functionCallResult == null || functionCallResult.isEmpty()) {
                log.warn("function calling返回空结果");
                return new PolicyQueryParamsDTO();
            }

            PolicyQueryParamsDTO params = JSON.parseObject(functionCallResult, PolicyQueryParamsDTO.class);
            log.info("提取到的保单查询参数: {}", params);
            return params;
        } catch (Exception e) {
            log.error("提取保单查询参数失败", e);
            return new PolicyQueryParamsDTO();
        }
    }

    /**
     * 构建提取参数的提示词
     */
    private String buildExtractPrompt(String userMessage) {
        return """
            请分析以下用户消息，提取其中与查询保单相关的参数。
            
            用户消息: %s
            
            需要提取的参数：
            - userId: 用户ID（如果有明确提到）
            - idCardNo: 身份证号（18位数字，最后一位可能是X）
            - polNo: 保单号（通常包含字母和数字的组合）
            - policyHolderName: 投保人姓名（中文姓名）
            
            注意：
            1. 只提取消息中明确提到的信息
            2. 如果某个参数没有提到，请返回null，不要猜测
            3. 身份证号需要验证格式是否正确
            4. 请只返回JSON格式的结果，不要其他解释
            """.formatted(userMessage);
    }

    /**
     * 获取提取保单参数的function定义
     */
    private String getExtractPolicyParamsFunction() {
        return """
            {
                "name": "extractPolicyQueryParams",
                "description": "从用户输入中提取保单查询相关参数",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "userId": {
                            "type": "string",
                            "description": "用户ID"
                        },
                        "idCardNo": {
                            "type": "string",
                            "description": "身份证号，18位数字，最后一位可能是X"
                        },
                        "polNo": {
                            "type": "string",
                            "description": "保单号"
                        },
                        "policyHolderName": {
                            "type": "string",
                            "description": "投保人姓名"
                        }
                    },
                    "required": []
                }
            }
            """;
    }
}