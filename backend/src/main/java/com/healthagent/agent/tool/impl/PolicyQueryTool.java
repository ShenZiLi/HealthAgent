package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.entity.PolInfoEntity;
import com.healthagent.service.PolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
public class PolicyQueryTool implements Tool {
    private final PolicyService policyService;

    public PolicyQueryTool(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public String getName() {
        return "query_policy";
    }

    @Override
    public String getDescription() {
        return "查询用户的保单信息，可以按保单号、状态筛选";
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setType("object");

        Map<String, ToolSchema.Property> properties = new HashMap<>();

        ToolSchema.Property userIdProp = new ToolSchema.Property();
        userIdProp.setType("string");
        userIdProp.setDescription("用户ID");
        properties.put("userId", userIdProp);

        ToolSchema.Property policyNoProp = new ToolSchema.Property();
        policyNoProp.setType("string");
        policyNoProp.setDescription("保单号（可选）");
        properties.put("policyNo", policyNoProp);

        ToolSchema.Property statusProp = new ToolSchema.Property();
        statusProp.setType("string");
        statusProp.setEnumValues(List.of("active", "expired", "all"));
        statusProp.setDescription("保单状态（可选）");
        properties.put("status", statusProp);

        schema.setProperties(properties);
        schema.setRequired(List.of("userId"));

        return schema;
    }

    @Override
    public ToolResult execute(ToolInput input) {
        try {
            String userId = (String) input.getParameters().get("userId");
            String policyNo = (String) input.getParameters().get("policyNo");
            String status = (String) input.getParameters().getOrDefault("status", "active");

            log.info("Executing PolicyQueryTool: userId={}, policyNo={}, status={}",
                    userId, policyNo, status);

            List<PolInfoEntity> policies;

            if (policyNo != null && !policyNo.isBlank()) {
                Optional<PolInfoEntity> policy = policyService.getByPolNo(policyNo);
                policies = policy.map(Collections::singletonList).orElse(Collections.emptyList());
            } else {
                policies = policyService.getByUserId(userId);
                if (!"all".equalsIgnoreCase(status)) {
                    String finalStatus = status;
                    policies = policies.stream()
                            .filter(p -> finalStatus.equalsIgnoreCase(p.getStatus()))
                            .toList();
                }
            }

            if (policies.isEmpty()) {
                return ToolResult.success("未找到相关保单信息");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(policies.size()).append(" 份保单：\n\n");
            for (int i = 0; i < policies.size(); i++) {
                PolInfoEntity p = policies.get(i);
                sb.append(i + 1).append(". ").append(p.getProductName()).append("\n");
                sb.append("   保单号: ").append(p.getPolNo()).append("\n");
                sb.append("   保险公司: ").append(p.getInsuranceCompany()).append("\n");
                sb.append("   保额: ").append(formatAmount(p.getInsuredAmount())).append("\n");
                sb.append("   状态: ").append(p.getStatus()).append("\n\n");
            }

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("PolicyQueryTool execution failed", e);
            return ToolResult.error("查询保单失败：" + e.getMessage());
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "未知";
        }
        return "¥" + amount.toPlainString();
    }
}
