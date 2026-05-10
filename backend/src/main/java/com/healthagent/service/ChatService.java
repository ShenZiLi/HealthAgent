package com.healthagent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.healthagent.dto.ChatRequest;
import com.healthagent.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class ChatService {

    @Value("${spring.ai.openai.base-url:https://open.bigmodel.cn}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${healthagent.chat.model:glm-4}")
    private String defaultModel;

    public ChatResponse chat(ChatRequest request) {
        try {
            String model = request.getModel() != null ? request.getModel() : defaultModel;
            String apiUrl = baseUrl + "/api/paas/v4/chat/completions";

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            List<JSONObject> messages = new ArrayList<>();
            
            if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                for (ChatRequest.ChatMessage chatMessage : request.getHistory()) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", chatMessage.getRole());
                    msg.put("content", chatMessage.getContent());
                    messages.add(msg);
                }
            }
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("stream", false);

            String response = sendPostRequest(apiUrl, requestBody.toJSONString());
            
            JSONObject responseJson = JSON.parseObject(response);
            
            JSONArray choices = responseJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                String content = message.getString("content");
                
                ChatResponse chatResponse = new ChatResponse();
                chatResponse.setMessage(content);
                chatResponse.setModel(model);
                chatResponse.setConversationId(responseJson.getString("id"));
                
                JSONObject usage = responseJson.getJSONObject("usage");
                if (usage != null) {
                    chatResponse.setTokens(usage.getLong("total_tokens"));
                }
                
                return chatResponse;
            }
            
            throw new RuntimeException("GLM API响应格式错误");
            
        } catch (Exception e) {
            log.error("调用GLM API失败", e);
            throw new RuntimeException("调用GLM API失败: " + e.getMessage(), e);
        }
    }

    public String chatStream(ChatRequest request) {
        try {
            String model = request.getModel() != null ? request.getModel() : defaultModel;
            String apiUrl = baseUrl + "/api/paas/v4/chat/completions";

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            List<JSONObject> messages = new ArrayList<>();
            
            if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                for (ChatRequest.ChatMessage chatMessage : request.getHistory()) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", chatMessage.getRole());
                    msg.put("content", chatMessage.getContent());
                    messages.add(msg);
                }
            }
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("stream", true);

            return sendStreamingRequest(apiUrl, requestBody.toJSONString());
            
        } catch (Exception e) {
            log.error("调用GLM流式API失败", e);
            throw new RuntimeException("调用GLM流式API失败: " + e.getMessage(), e);
        }
    }

    private String sendPostRequest(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                throw new RuntimeException("GLM API返回错误码: " + responseCode + ", 响应: " + response);
            }
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }

    private String sendStreamingRequest(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                throw new RuntimeException("GLM API返回错误码: " + responseCode + ", 响应: " + response);
            }
        }

        StringBuilder fullResponse = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    fullResponse.append(data).append("\n");
                }
            }
        }
        
        return fullResponse.toString();
    }
}
