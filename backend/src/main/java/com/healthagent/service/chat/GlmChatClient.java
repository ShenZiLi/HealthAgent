package com.healthagent.service.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GLM大模型聊天客户端实现
 */
public class GlmChatClient extends AbstractChatClient {

    public GlmChatClient(String apiKey, String baseUrl, String model) {
        super(apiKey, baseUrl, model);
    }

    @Override
    protected String buildRequestBody(List<ChatMessage> messages) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);

        JSONArray messageArray = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getRole());
            msgObj.put("content", msg.getContent());
            messageArray.add(msgObj);
        }
        requestBody.put("messages", messageArray);
        requestBody.put("stream", false);

        return requestBody.toJSONString();
    }

    @Override
    protected String callApi(String requestBody) throws Exception {
        String apiUrl = baseUrl + "/api/paas/v4/chat/completions";
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                throw new RuntimeException("GLM API返回错误码: " + responseCode + ", 响应: " + response);
            }
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    @Override
    protected String parseResponse(String rawResponse) throws Exception {
        JSONObject responseJson = JSON.parseObject(rawResponse);
        JSONArray choices = responseJson.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getString("content");
            return content != null ? content : "抱歉，我无法回答该问题。";
        }
        return "抱歉，我现在无法回答您的问题。";
    }

    @Override
    protected String buildErrorMessage(Exception e) {
        return "抱歉，我现在无法回答您的问题，请稍后再试。";
    }
}
