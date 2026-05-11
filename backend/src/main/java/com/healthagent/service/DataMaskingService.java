package com.healthagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DataMaskingService {

    private static final Pattern POLICY_ID_PATTERN = Pattern.compile("(.{2}).*(.{2})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(.{6}).*(.{4})");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(.{3}).*(.{4})");
    private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile("(.{4}).*(.{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(.{1}).*(@.{1,})$");

    public String maskPolicyId(String policyId) {
        if (policyId == null || policyId.length() < 4) {
            return "****";
        }
        Matcher matcher = POLICY_ID_PATTERN.matcher(policyId);
        if (matcher.matches()) {
            return matcher.replaceFirst("$1****$2");
        }
        return policyId.length() > 4 ? policyId.substring(0, 2) + "****" + policyId.substring(policyId.length() - 2) : "****";
    }

    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return "******************";
        }
        Matcher matcher = ID_CARD_PATTERN.matcher(idCard);
        if (matcher.matches()) {
            return matcher.replaceFirst("$1********$2");
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        Matcher matcher = PHONE_PATTERN.matcher(phone);
        if (matcher.matches()) {
            return matcher.replaceFirst("$1****$2");
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public String maskBankAccount(String bankAccount) {
        if (bankAccount == null || bankAccount.length() < 8) {
            return "****";
        }
        Matcher matcher = BANK_ACCOUNT_PATTERN.matcher(bankAccount);
        if (matcher.matches()) {
            return matcher.replaceFirst("$1****$2");
        }
        return bankAccount.substring(0, 4) + "****" + bankAccount.substring(bankAccount.length() - 4);
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.find()) {
            return matcher.replaceFirst("$1****$2");
        }
        int atIndex = email.indexOf('@');
        if (atIndex > 1) {
            return email.substring(0, 1) + "****" + email.substring(atIndex);
        }
        return "****" + email.substring(atIndex);
    }

    public String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "**";
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    public <T> T maskSensitiveFields(T data, Class<T> clazz) {
        if (data == null) {
            return null;
        }
        try {
            com.alibaba.fastjson2.JSONObject jsonObj = (com.alibaba.fastjson2.JSONObject) data;
            jsonObj.put("policyId", maskPolicyId(jsonObj.getString("policyId")));
            if (jsonObj.containsKey("idCardNo")) {
                jsonObj.put("idCardNo", maskIdCard(jsonObj.getString("idCardNo")));
            }
            if (jsonObj.containsKey("phone")) {
                jsonObj.put("phone", maskPhone(jsonObj.getString("phone")));
            }
            if (jsonObj.containsKey("bankAccount")) {
                jsonObj.put("bankAccount", maskBankAccount(jsonObj.getString("bankAccount")));
            }
            if (jsonObj.containsKey("email")) {
                jsonObj.put("email", maskEmail(jsonObj.getString("email")));
            }
            if (jsonObj.containsKey("policyHolderName")) {
                jsonObj.put("policyHolderName", maskName(jsonObj.getString("policyHolderName")));
            }
            return (T) jsonObj;
        } catch (Exception e) {
            log.warn("脱敏处理失败: {}", e.getMessage());
            return data;
        }
    }
}
