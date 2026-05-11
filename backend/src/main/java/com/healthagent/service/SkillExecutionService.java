package com.healthagent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.healthagent.config.SkillConfigLoader;
import com.healthagent.dto.PolicyInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class SkillExecutionService {

    @Autowired
    private PolicyService policyService;

    @Autowired
    private DataMaskingService dataMaskingService;

    @Autowired
    private SkillConfigLoader skillConfigLoader;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public JSONObject executePolicyQuery(String userId, String policyNo, String status) {
        SkillConfigLoader.SkillConfig skillConfig = skillConfigLoader.getSkill("policy_query");
        JSONObject errorConfig = skillConfig != null ?
            JSON.parseObject(skillConfig.getErrorHandling()) : null;

        if (userId == null || userId.trim().isEmpty()) {
            return buildErrorResponse("USER_NOT_FOUND",
                errorConfig != null ? errorConfig.getString("userNotFound") : "用户ID不能为空");
        }

        try {
            List<PolicyInfo> policies;

            if (policyNo != null && !policyNo.trim().isEmpty()) {
                Optional<PolicyInfo> policy = policyService.getPolicyById(userId, policyNo);
                policies = policy.map(Collections::singletonList).orElse(Collections.emptyList());
            } else {
                if ("all".equalsIgnoreCase(status)) {
                    policies = policyService.getAllUserPolicies(userId);
                } else {
                    policies = policyService.getUserPolicies(userId);
                }
            }

            if (policies.isEmpty()) {
                return buildErrorResponse("NO_ACTIVE_POLICY",
                    errorConfig != null ? errorConfig.getString("noActivePolicy") : "未找到保单信息");
            }

            return buildSuccessResponse(policies);

        } catch (Exception e) {
            log.error("查询保单失败: {}", e.getMessage(), e);
            return buildErrorResponse("SYSTEM_ERROR",
                errorConfig != null ? errorConfig.getString("systemError") : "系统繁忙，请稍后再试");
        }
    }

    private JSONObject buildSuccessResponse(List<PolicyInfo> policies) {
        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("totalCount", policies.size());

        JSONArray policyList = new JSONArray();
        for (PolicyInfo policy : policies) {
            JSONObject policyJson = maskAndFormatPolicy(policy);
            policyList.add(policyJson);
        }
        response.put("policyList", policyList);
        response.put("message", "查询成功");

        return response;
    }

    private JSONObject buildErrorResponse(String code, String message) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("code", code);
        response.put("message", message);
        response.put("policyList", new JSONArray());
        response.put("totalCount", 0);
        return response;
    }

    private JSONObject maskAndFormatPolicy(PolicyInfo policy) {
        JSONObject policyJson = new JSONObject();

        policyJson.put("policyId", dataMaskingService.maskPolicyId(policy.getPolicyId()));

        policyJson.put("policyName", policy.getPolicyName());

        String statusText = "unknown".equals(policy.getStatus()) ? "未知" :
                           "active".equals(policy.getStatus()) ? "生效中" :
                           "expired".equals(policy.getStatus()) ? "已过期" :
                           "pending".equals(policy.getStatus()) ? "待生效" : policy.getStatus();
        policyJson.put("status", statusText);

        policyJson.put("coverage", formatAmount(policy.getCoverage()));
        policyJson.put("premium", formatAmount(policy.getPremium()));

        policyJson.put("effectiveDate", formatDate(policy.getStartDate()));
        policyJson.put("expiryDate", formatDate(policy.getEndDate()));

        policyJson.put("insuranceCompany", policy.getInsuranceCompany());

        return policyJson;
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("%,.2f", amount);
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "终身";
        }
        return DATE_FORMAT.format(date);
    }

    public boolean isPolicyQuerySkill(String message) {
        SkillConfigLoader.SkillConfig skillConfig = skillConfigLoader.getSkill("policy_query");
        if (skillConfig == null) {
            return false;
        }

        List<String> triggers = skillConfig.getTriggers();
        if (triggers == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        for (String trigger : triggers) {
            if (lowerMessage.contains(trigger.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
