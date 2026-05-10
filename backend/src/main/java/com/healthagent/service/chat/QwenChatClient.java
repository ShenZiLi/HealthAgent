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
 * 通义千问大模型聊天客户端实现
 */
public class QwenChatClient extends AbstractChatClient {

    public QwenChatClient(String apiKey, String baseUrl, String model) {
        super(apiKey, baseUrl, model);
    }

    @Override
    protected String getDefaultSystemPrompt() {
        return "你是健康助手AI客服，专注于为用户提供健康保险和体检预约相关的咨询和服务。回答要专业、友好、简洁。";
    }

    @Override
    protected String buildRequestBody(List<ChatMessage> messages) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);

        // 千问 API 使用 "input" 嵌套结构
        JSONObject input = new JSONObject();
        JSONArray messageArray = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getRole());
            msgObj.put("content", msg.getContent());
            messageArray.add(msgObj);
        }
        input.put("messages", messageArray);
        requestBody.put("input", input);

        return requestBody.toJSONString();
    }

    @Override
    protected String buildRequestBodyWithFunction(List<ChatMessage> messages, String functionDefinition) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);

        // 千问 API 使用 "input" 嵌套结构
        JSONObject input = new JSONObject();
        JSONArray messageArray = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getRole());
            msgObj.put("content", msg.getContent());
            messageArray.add(msgObj);
        }
        input.put("messages", messageArray);
        requestBody.put("input", input);

        // 千问使用 tools 字段
        JSONObject functionDef = JSON.parseObject(functionDefinition);
        JSONArray tools = new JSONArray();
        JSONObject tool = new JSONObject();
        tool.put("type", "function");
        tool.put("function", functionDef);
        tools.add(tool);
        requestBody.put("tools", tools);

        return requestBody.toJSONString();
    }

    @Override
    protected String callApi(String requestBody) throws Exception {
        String apiUrl = baseUrl + "/api/v1/services/aigc/text-generation/generation";
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        // 千问要求 X-DashScope-SSE 头部
        conn.setRequestProperty("X-DashScope-SSE", "disable");
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
                throw new RuntimeException("Qwen API返回错误码: " + responseCode + ", 响应: " + response);
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
        // 千问响应格式: output.choices[0].message.content
        JSONObject output = responseJson.getJSONObject("output");
        if (output != null) {
            JSONArray choices = output.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    String content = message.getString("content");
                    return content != null ? content : "抱歉，我无法回答该问题。";
                }
            }
        }
        return "抱歉，我现在无法回答您的问题。";
    }

    @Override
    protected String parseFunctionCallResponse(String rawResponse) throws Exception {
        JSONObject responseJson = JSON.parseObject(rawResponse);
        // 千问响应格式: output.choices[0].message.tool_calls
        JSONObject output = responseJson.getJSONObject("output");
        if (output != null) {
            JSONArray choices = output.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                JSONArray toolCalls = message.getJSONArray("tool_calls");
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    JSONObject toolCall = toolCalls.getJSONObject(0);
                    JSONObject function = toolCall.getJSONObject("function");
                    String arguments = function.getString("arguments");
                    return arguments != null ? arguments : "";
                }
                String content = message.getString("content");
                return content != null ? content : "";
            }
        }
        return "";
    }

    @Override
    protected String buildErrorMessage(Exception e) {
        return "抱歉，我现在无法回答您的问题，请稍后再试。";
    }
}
