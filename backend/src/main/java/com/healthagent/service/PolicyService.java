package com.healthagent.service;

import com.healthagent.dto.PolicyInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class PolicyService {

    private static final Map<String, List<PolicyInfo>> MOCK_POLICIES = new HashMap<>();

    static {
        List<PolicyInfo> user1Policies = new ArrayList<>();
        user1Policies.add(new PolicyInfo(
            "POL20240001",
            "health",
            "健康医疗保险",
            "active",
            new BigDecimal("3650.00"),
            new BigDecimal("500000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 180),
            new Date(System.currentTimeMillis() + 86400000L * 185),
            "平安保险"
        ));
        user1Policies.add(new PolicyInfo(
            "POL20240002",
            "life",
            "终身寿险",
            "active",
            new BigDecimal("12000.00"),
            new BigDecimal("1000000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 365),
            null,
            "中国人寿"
        ));
        user1Policies.add(new PolicyInfo(
            "POL20230015",
            "accident",
            "意外伤害保险",
            "expired",
            new BigDecimal("280.00"),
            new BigDecimal("100000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 400),
            new Date(System.currentTimeMillis() - 86400000L * 35),
            "太平洋保险"
        ));

        List<PolicyInfo> user2Policies = new ArrayList<>();
        user2Policies.add(new PolicyInfo(
            "POL20240015",
            "health",
            "重大疾病保险",
            "active",
            new BigDecimal("5800.00"),
            new BigDecimal("800000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 90),
            new Date(System.currentTimeMillis() + 86400000L * 275),
            "友邦保险"
        ));

        List<PolicyInfo> user3Policies = new ArrayList<>();
        user3Policies.add(new PolicyInfo(
            "POL20240020",
            "property",
            "家庭财产保险",
            "active",
            new BigDecimal("520.00"),
            new BigDecimal("500000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 60),
            new Date(System.currentTimeMillis() + 86400000L * 305),
            "中华保险"
        ));

        MOCK_POLICIES.put("user001", user1Policies);
        MOCK_POLICIES.put("user002", user2Policies);
        MOCK_POLICIES.put("user003", user3Policies);
    }

    public List<PolicyInfo> getUserPolicies(String userId) {
        log.info("查询用户 {} 的保单信息", userId);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        List<PolicyInfo> policies = MOCK_POLICIES.get(userId);
        if (policies == null) {
            return new ArrayList<>();
        }
        
        return policies.stream()
            .filter(policy -> "active".equals(policy.getStatus()))
            .sorted(Comparator.comparing(PolicyInfo::getStartDate).reversed())
            .toList();
    }

    public List<PolicyInfo> getAllUserPolicies(String userId) {
        log.info("查询用户 {} 的所有保单信息（包括已过期）", userId);
        
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        List<PolicyInfo> policies = MOCK_POLICIES.get(userId);
        if (policies == null) {
            return new ArrayList<>();
        }
        
        return policies.stream()
            .sorted(Comparator.comparing(PolicyInfo::getStartDate).reversed())
            .toList();
    }

    public Optional<PolicyInfo> getPolicyById(String userId, String policyId) {
        List<PolicyInfo> policies = getUserPolicies(userId);
        return policies.stream()
            .filter(p -> p.getPolicyId().equals(policyId))
            .findFirst();
    }

    public String formatPoliciesAsText(List<PolicyInfo> policies) {
        if (policies == null || policies.isEmpty()) {
            return "未查询到有效保单";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您共有 ").append(policies.size()).append(" 份有效保单：\n\n");

        for (int i = 0; i < policies.size(); i++) {
            PolicyInfo policy = policies.get(i);
            sb.append("【保单").append(i + 1).append("】\n");
            sb.append("• 保单号：").append(policy.getPolicyId()).append("\n");
            sb.append("• 产品名称：").append(policy.getPolicyName()).append("\n");
            sb.append("• 保险公司：").append(policy.getInsuranceCompany()).append("\n");
            sb.append("• 保障额度：¥").append(policy.getCoverage()).append("\n");
            sb.append("• 年缴保费：¥").append(policy.getPremium()).append("\n");
            sb.append("• 生效日期：").append(formatDate(policy.getStartDate())).append("\n");
            if (policy.getEndDate() != null) {
                sb.append("• 到期日期：").append(formatDate(policy.getEndDate())).append("\n");
            } else {
                sb.append("• 保障期限：终身\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "未知";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
}
