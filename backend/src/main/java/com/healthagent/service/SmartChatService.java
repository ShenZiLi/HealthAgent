package com.healthagent.service;

import com.healthagent.common.IntentType;
import com.healthagent.dto.*;
import com.healthagent.service.chat.AbstractChatClient;
import com.healthagent.service.chat.ChatClientFactory;
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

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private ExaminationIntentService examinationIntentService;

    @Autowired
    private ExaminationService examinationService;

    private AbstractChatClient chatClient;

    @Value("${spring.ai.openai.base-url:https://open.bigmodel.cn}")
    private String baseUrl;

    @Value("${healthagent.glm.api-key:}")
    private String apiKey;

    @Value("${healthagent.chat.model:glm-4}")
    private String defaultModel;

    @Value("${healthagent.chat.provider:glm}")
    private String provider;

    private AbstractChatClient getChatClient() {
        if (chatClient == null) {
            chatClient = ChatClientFactory.createClient(provider, apiKey, baseUrl, defaultModel);
        }
        return chatClient;
    }

    public SmartChatResponse chat(SmartChatRequest request) {
        String userMessage = request.getMessage();
        String userId = request.getUserId();

        log.info("接收到用户消息: {}, userId: {}", userMessage, userId);

        IntentType intent = sessionManager.getCachedIntent(userId);
        if (intent == null) {
            intent = intentRecognitionService.recognizeIntent(userMessage);
            log.info("首次识别意图: {}", intent.getDesc());
            sessionManager.cacheIntent(userId, intent);
        } else {
            log.info("复用缓存意图: {}", intent.getDesc());
        }

        SmartChatResponse response = new SmartChatResponse();
        response.setIntent(intent.getCode());

        switch (intent) {
            case QUERY_POLICY:
                return handleInsuranceQuery(userMessage, userId, response);
            case BOOK_EXAMINATION:
                return handleExaminationBooking(userMessage, userId, response);
            case HEALTH_CONSULTATION:
            case GENERAL_CONVERSATION:
            default:
                return handleGeneralConversation(userMessage, userId, response);
        }
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
            // MOCK 保单查询数据
            var policies = policyService.getUserPolicies(userId);
            // 格式化结构
            String policyInfo = policyService.formatPoliciesAsText(policies);

            response.setData(policies);
            response.setAction("query_policy_success");
            response.setMessageType("policy_info");
            // AI友好化处理
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

    private SmartChatResponse handleExaminationBooking(String userMessage, String userId, SmartChatResponse response) {
        if (userId == null || userId.trim().isEmpty()) {
            response.setMessage("我需要您的用户ID才能为您预约体检，请问您的用户ID是多少？");
            response.setNeedsMoreInfo(true);
            response.setAction("require_user_id");
            response.setMessageType("info_request");
            return response;
        }

        try {
            log.info("识别体检预约意图");
            ExaminationIntentData intentData = examinationIntentService.recognizeExaminationIntent(userMessage);

            if (intentData.getNeedsMoreInfo() != null && intentData.getNeedsMoreInfo()) {
                String missingInfo = buildMissingInfoMessage(intentData);
                response.setMessage(missingInfo);
                response.setNeedsMoreInfo(true);
                response.setAction("require_examination_info");
                response.setMessageType("info_request");
                return response;
            }

            log.info("创建体检预约: hospital={}, date={}", intentData.getHospitalName(), intentData.getExaminationDate());

            String mockBookingResult = buildMockBookingResult(intentData);
            response.setMessage(mockBookingResult);
            response.setAction("examination_booking_success");
            response.setMessageType("booking_confirm");
            return response;

        } catch (Exception e) {
            log.error("体检预约失败", e);
            response.setMessage("抱歉，预约体检时出现了问题，请稍后再试。错误信息：" + e.getMessage());
            response.setAction("examination_booking_failed");
            response.setMessageType("error");
            return response;
        }
    }

    private String buildMockBookingResult(ExaminationIntentData intentData) {
        String bookingNo = "EXM"
                + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int) (Math.random() * 10000));
        return """
                ✅ 体检预约成功！

                📋 预约信息：
                • 预约号：%s
                • 医院：%s
                • 套餐：%s
                • 预约日期：%s
                • 预约时间：%s

                📌 注意事项：
                • 体检前一天清淡饮食
                • 体检当天需空腹
                • 请携带身份证和预约凭证

                如需变更或取消，请提前联系我们。
                """.formatted(
                bookingNo,
                intentData.getHospitalName() != null ? intentData.getHospitalName() : "北京协和医院",
                intentData.getPackageType() != null ? intentData.getPackageType() : "全身体检套餐",
                intentData.getExaminationDate() != null ? intentData.getExaminationDate() : "待确认",
                intentData.getExaminationTime() != null ? intentData.getExaminationTime() : "上午 9:00");
    }

    private String buildMissingInfoMessage(ExaminationIntentData intentData) {
        StringBuilder message = new StringBuilder("为了帮您预约体检，请提供以下信息：\n\n");

        if (intentData.getMissingFields() != null && intentData.getMissingFields().contains("hospitalName")) {
            message.append("🏥 医院名称：您想去哪家医院体检？\n");
            message.append("   可选医院：北京协和医院、301医院、北大医院、中山医院等\n\n");
        }

        if (intentData.getMissingFields() != null && intentData.getMissingFields().contains("examinationDate")) {
            message.append("📅 体检日期：您想哪天去体检？\n");
            message.append("   例如：明天、后天、2024-02-15\n\n");
        }

        message.append("请告诉我这些信息，我来帮您预约！");

        return message.toString();
    }

    private SmartChatResponse handleGeneralConversation(String userMessage, String userId, SmartChatResponse response) {
        try {
            String aiResponse = getChatClient().chat(
                    userMessage,
                    "你是健康助手AI客服，专注于为用户提供健康保险和体检预约相关的咨询和服务。回答要专业、友好、简洁。",
                    userId);
            response.setMessage(aiResponse);
            response.setAction("general_response");
            response.setMessageType("conversation");
            return response;
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            response.setMessage("抱歉，我现在无法回答您的问题，请稍后再试。");
            response.setAction("chat_call_failed");
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

                    回复要简洁，自然，像一个专业的保险顾问。
                    """, policyInfo, userMessage);

            return getChatClient().chat(prompt, "你是一个专业的保险顾问助手。", null);
        } catch (Exception e) {
            log.error("生成保单回复失败", e);
            return policyInfo + "\n请问还有什么需要了解的吗？";
        }
    }
}
