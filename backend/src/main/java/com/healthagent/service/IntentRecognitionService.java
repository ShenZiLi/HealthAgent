package com.healthagent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class IntentRecognitionService {

    @Value("${spring.ai.openai.base-url:https://open.bigmodel.cn}")
    private String baseUrl;

    @Value("${healthagent.glm.api-key:}")
    private String apiKey;

    @Value("${healthagent.chat.model:glm-4}")
    private String defaultModel;

    public String recognizeIntent(String userMessage) {
        try {
            String prompt = buildIntentRecognitionPrompt(userMessage);
            return callGLMForIntent(prompt);
        } catch (Exception e) {
            log.error("意图识别失败", e);
            return "general_conversation";
        }
    }

    private String buildIntentRecognitionPrompt(String userMessage) {
        return String.format("""
            请分析用户消息，判断用户的意图。
            
            用户消息: %s
            
            可能的意图类型:
            1. query_policy - 用户想查询保单信息
            2. general_conversation - 一般对话，闲聊
            3. health_consultation - 健康咨询
            4. other - 其他
            
            请只返回一个意图类型，不需要其他解释。
            """, userMessage);
    }

    private String callGLMForIntent(String prompt) throws Exception {
        String model = defaultModel;
        String apiUrl = baseUrl + "/api/paas/v4/chat/completions";

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个意图识别助手，只返回简洁的意图类型。");
        
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        List<JSONObject> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        String response = sendPostRequest(apiUrl, requestBody.toJSONString());
        JSONObject responseJson = JSON.parseObject(response);
        
        JSONArray choices = responseJson.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            return message.getString("content").trim().toLowerCase();
        }
        
        return "general_conversation";
    }

    public boolean isInsuranceQuery(String intent) {
        return "query_policy".equals(intent) || 
               intent.contains("policy") || 
               intent.contains("保单") ||
               intent.contains("保险");
    }

    private String sendPostRequest(String urlStr, String jsonBody) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                throw new RuntimeException("GLM API返回错误码: " + responseCode + ", 响应: " + response);
            }
        }

        StringBuilder response = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
}
