package com.healthagent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.healthagent.dto.ExaminationIntentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExaminationIntentService {

    @Value("${spring.ai.openai.base-url:https://open.bigmodel.cn}")
    private String baseUrl;

    @Value("${healthagent.glm.api-key:}")
    private String apiKey;

    @Value("${healthagent.chat.model:glm-4}")
    private String defaultModel;

    private static final List<String> COMMON_HOSPITALS = List.of(
        "北京协和医院",
        "301医院",
        "北京大学第一医院",
        "复旦大学附属中山医院",
        "上海交通大学医学院附属瑞金医院",
        "广东省人民医院",
        "四川大学华西医院",
        "武汉同济医院"
    );

    public ExaminationIntentData recognizeExaminationIntent(String userMessage) {
        ExaminationIntentData result = new ExaminationIntentData();
        result.setIntent("book_examination");
        result.setNeedsMoreInfo(false);
        List<String> missingFields = new ArrayList<>();

        try {
            String prompt = buildExaminationIntentPrompt(userMessage);
            String jsonResponse = callGLMForIntent(prompt);
            
            // 清理AI返回的内容，移除markdown代码块标记
            String cleanedJson = cleanJsonContent(jsonResponse);
            
            JSONObject intentData = JSON.parseObject(cleanedJson);
            
            String hospitalName = intentData.getString("hospitalName");
            String hospitalCode = intentData.getString("hospitalCode");
            String examinationDate = intentData.getString("examinationDate");
            String examinationTime = intentData.getString("examinationTime");
            String packageType = intentData.getString("packageType");
            String notes = intentData.getString("notes");
            Double confidence = intentData.getDouble("confidence");

            result.setHospitalName(hospitalName);
            result.setHospitalCode(hospitalCode);
            result.setExaminationDate(examinationDate);
            result.setExaminationTime(examinationTime);
            result.setPackageType(packageType);
            result.setNotes(notes);
            result.setConfidence(confidence != null ? confidence : 0.8);

            if (hospitalName == null || hospitalName.trim().isEmpty()) {
                missingFields.add("hospitalName");
            }
            
            if (examinationDate == null || examinationDate.trim().isEmpty()) {
                missingFields.add("examinationDate");
            }

            if (!missingFields.isEmpty()) {
                result.setNeedsMoreInfo(true);
                result.setMissingFields(String.join(",", missingFields));
            }

            return result;

        } catch (Exception e) {
            log.error("体检意图识别失败，尝试规则匹配", e);
            return fallbackRuleBasedRecognition(userMessage);
        }
    }

    private String cleanJsonContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // 移除markdown代码块标记
        String cleaned = content.trim();
        
        // 处理 ```json ... ``` 格式
        if (cleaned.startsWith("```")) {
            int firstNewLine = cleaned.indexOf('\n');
            if (firstNewLine != -1) {
                cleaned = cleaned.substring(firstNewLine + 1);
            } else {
                cleaned = cleaned.substring(3);
            }
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }

    private String buildExaminationIntentPrompt(String userMessage) {
        return String.format("""
            请分析用户消息，提取体检预约相关信息。
            
            用户消息: %s
            
            请以JSON格式返回以下信息：
            {
                "hospitalName": "医院名称（如果没有明确提到，请填null）",
                "hospitalCode": "医院代码（如果没有明确提到，请填null）",
                "examinationDate": "体检日期，格式YYYY-MM-DD（如果没有明确提到，请填null）",
                "examinationTime": "体检时间，如'上午9点'（如果没有明确提到，请填null）",
                "packageType": "套餐类型，如'全身体检'、'入职体检'等（如果没有明确提到，请填null）",
                "notes": "其他备注信息（如果没有，请填null）",
                "confidence": 0.0到1.0之间的置信度
            }
            
            只返回JSON，不要有其他内容。
            """, userMessage);
    }

    private String callGLMForIntent(String prompt) throws Exception {
        String model = defaultModel;
        String apiUrl = baseUrl + "/api/paas/v4/chat/completions";

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个体检预约助手，负责提取用户想要预约体检的信息。严格按照JSON格式返回。");
        
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
            return message.getString("content").trim();
        }
        
        throw new RuntimeException("GLM API响应格式错误");
    }

    private ExaminationIntentData fallbackRuleBasedRecognition(String userMessage) {
        ExaminationIntentData result = new ExaminationIntentData();
        result.setIntent("book_examination");
        result.setConfidence(0.6);
        result.setNeedsMoreInfo(true);
        List<String> missingFields = new ArrayList<>();

        String hospitalName = extractHospitalName(userMessage);
        result.setHospitalName(hospitalName);
        if (hospitalName == null) {
            missingFields.add("hospitalName");
        }

        String examinationDate = extractDate(userMessage);
        result.setExaminationDate(examinationDate);
        if (examinationDate == null) {
            missingFields.add("examinationDate");
        }

        String examinationTime = extractTime(userMessage);
        result.setExaminationTime(examinationTime);

        if (!missingFields.isEmpty()) {
            result.setMissingFields(String.join(",", missingFields));
        }

        return result;
    }

    private String extractHospitalName(String message) {
        for (String hospital : COMMON_HOSPITALS) {
            if (message.contains(hospital)) {
                return hospital;
            }
        }
        
        Pattern pattern = Pattern.compile("(.*?医院)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    private String extractDate(String message) {
        Pattern pattern = Pattern.compile("(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            dateStr = dateStr.replace("年", "-").replace("月", "-").replace("日", "").replace("/", "-");
            try {
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                log.warn("日期解析失败: {}", dateStr);
            }
        }
        
        if (message.contains("明天")) {
            return LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (message.contains("后天")) {
            return LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (message.contains("下周")) {
            return LocalDate.now().plusWeeks(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        return null;
    }

    private String extractTime(String message) {
        Pattern pattern = Pattern.compile("(\\d{1,2}[点时](?:\\d{1,2}分)?)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        if (message.contains("上午") || message.contains("早上")) {
            return "上午";
        } else if (message.contains("下午")) {
            return "下午";
        }
        
        return null;
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
                throw new RuntimeException("GLM API返回错误码: " + responseCode);
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
