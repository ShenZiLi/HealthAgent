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

    public String executePolicyQuery(String userId, String policyNo, String status) {
        SkillConfigLoader.SkillConfig skillConfig = skillConfigLoader.getSkill("policy_query");

        if (userId == null || userId.trim().isEmpty()) {
            return formatAsMarkdown(skillConfig, buildErrorResult("USER_NOT_FOUND", "用户ID不能为空"));
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
                return formatAsMarkdown(skillConfig, buildErrorResult("NO_ACTIVE_POLICY", "未找到保单信息"));
            }

            return formatPoliciesAsMarkdown(skillConfig, policies);

        } catch (Exception e) {
            log.error("查询保单失败: {}", e.getMessage(), e);
            return formatAsMarkdown(skillConfig, buildErrorResult("SYSTEM_ERROR", "系统繁忙，请稍后再试"));
        }
    }

    private Map<String, Object> buildErrorResult(String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    private String formatAsMarkdown(SkillConfigLoader.SkillConfig skillConfig, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        Boolean success = (Boolean) data.get("message");
        if (success != null && !success) {
            sb.append("❌ 查询失败\n\n");
        }
        sb.append("**提示信息**: ").append(data.get("message")).append("\n");
        return sb.toString();
    }

    private String formatPoliciesAsMarkdown(SkillConfigLoader.SkillConfig skillConfig, List<PolicyInfo> policies) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ 查询成功\n\n");
        sb.append("您共有 ").append(policies.size()).append(" 份有效保单：\n\n");

        for (int i = 0; i < policies.size(); i++) {
            PolicyInfo policy = policies.get(i);
            sb.append("**【保单").append(i + 1).append("】**\n");

            sb.append("- **保单号**: ").append(dataMaskingService.maskPolicyId(policy.getPolicyId())).append("\n");
            sb.append("- **产品名称**: ").append(policy.getPolicyName()).append("\n");
            sb.append("- **保险公司**: ").append(policy.getInsuranceCompany()).append("\n");
            sb.append("- **保障额度**: ").append(formatAmount(policy.getCoverage())).append("元\n");
            sb.append("- **年缴保费**: ").append(formatAmount(policy.getPremium())).append("元\n");
            sb.append("- **生效日期**: ").append(formatDate(policy.getStartDate())).append("\n");

            if (policy.getEndDate() != null) {
                sb.append("- **到期日期**: ").append(formatDate(policy.getEndDate())).append("\n");
            } else {
                sb.append("- **保障期限**: 终身\n");
            }

            sb.append("- **状态**: ").append(formatStatus(policy.getStatus())).append("\n");
            sb.append("\n");
        }

        return sb.toString();
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

    private String formatStatus(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status.toLowerCase()) {
            case "active" -> "生效中";
            case "expired" -> "已过期";
            case "pending" -> "待生效";
            case "cancelled" -> "已取消";
            default -> status;
        };
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

    public SkillConfigLoader.SkillConfig matchSkillByTrigger(String message) {
        return skillConfigLoader.getSkillByTrigger(message);
    }
}
