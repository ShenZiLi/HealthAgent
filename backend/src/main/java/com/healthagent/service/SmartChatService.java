package com.healthagent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.healthagent.dto.SmartChatRequest;
import com.healthagent.dto.SmartChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmartChatService {

    @Autowired
    private IntentRecognitionService intentRecognitionService;

    @Autowired
    private PolicyService policyService;

    @Value("${spring.ai.openai.base-url:https://open.bigmodel.cn}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${healthagent.chat.model:glm-4}")
    private String defaultModel;

    public SmartChatResponse chat(SmartChatRequest request) {
        String userMessage = request.getMessage();
        String userId = request.getUserId();

        log.info("接收到用户消息: {}, userId: {}", userMessage, userId);

        String intent = intentRecognitionService.recognizeIntent(userMessage);
        log.info("识别到的意图: {}", intent);

        SmartChatResponse response = new SmartChatResponse();
        response.setIntent(intent);

        if (intentRecognitionService.isInsuranceQuery(intent)) {
            return handleInsuranceQuery(userMessage, userId, response);
        }

        return handleGeneralConversation(userMessage, userId, response);
    }

    private SmartChatResponse handleInsuranceQuery(String userMessage, String userId, SmartChatResponse response) {
        if (userId == null || userId.trim().isEmpty()) {
            response.setMessage("我需要您的用户ID才能查询您的保单信息，请问您的用户ID是多少？");
            response.setNeedsMoreInfo(true);
            response.setAction("require_user_id");
            response.setMessageType("info_request");
            return response;
        }

        try {
            log.info("查询用户 {} 的保单信息", userId);
            var policies = policyService.getUserPolicies(userId);
            String policyInfo = policyService.formatPoliciesAsText(policies);

            response.setData(policies);
            response.setAction("query_policy_success");
            response.setMessageType("policy_info");

            String aiResponse = generatePolicyResponse(userMessage, policyInfo);
            response.setMessage(aiResponse);

            return response;
        } catch (Exception e) {
            log.error("查询保单失败", e);
            response.setMessage("抱歉，查询保单时出现了问题，请稍后再试。");
            response.setAction("query_policy_failed");
            response.setMessageType("error");
            return response;
        }
    }

    private SmartChatResponse handleGeneralConversation(String userMessage, String userId, SmartChatResponse response) {
        try {
            String aiResponse = callGLM(userMessage, userId);
            response.setMessage(aiResponse);
            response.setAction("general_response");
            response.setMessageType("conversation");
            return response;
        } catch (Exception e) {
            log.error("调用GLM失败", e);
            response.setMessage("抱歉，我现在无法回答您的问题，请稍后再试。");
            response.setAction("glm_call_failed");
            response.setMessageType("error");
            return response;
        }
    }

    private String generatePolicyResponse(String userMessage, String policyInfo) {
        try {
            String prompt = String.format("""
                用户询问保单相关问题，以下是查询到的保单信息：
                
                %s
                
                请根据以上信息，用友好的方式回复用户，可以：
                1. 总结保单的主要特点
                2. 提醒用户关注的事项
                3. 询问是否需要了解更多信息
                
                用户原问题：%s
                
                回复要简洁、自然，像一个专业的保险顾问。
                """, policyInfo, userMessage);

            return callGLM(prompt, null);
        } catch (Exception e) {
            log.error("生成保单回复失败", e);
            return policyInfo + "\n请问还有什么需要了解的吗？";
        }
    }

    private String callGLM(String userMessage, String userId) throws Exception {
        String model = defaultModel;
        String apiUrl = baseUrl + "/api/paas/v4/chat/completions";

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);

        java.util.List<JSONObject> messages = new java.util.ArrayList<>();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是健康助手AI客服，专注于为用户提供健康保险相关的咨询和服务。回答要专业、友好、简洁。");
        messages.add(systemMessage);

        if (userId != null) {
            JSONObject userInfoMessage = new JSONObject();
            userInfoMessage.put("role", "system");
            userInfoMessage.put("content", "当前用户ID: " + userId);
            messages.add(userInfoMessage);
        }

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        String response = sendPostRequest(apiUrl, requestBody.toJSONString());
        JSONObject responseJson = JSON.parseObject(response);

        JSONArray choices = responseJson.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            return message.getString("content");
        }

        return "抱歉，我现在无法回答您的问题。";
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
