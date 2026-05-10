package com.healthagent.dto;

import lombok.Data;

/**
 * 从用户输入中提取的保单查询参数
 */
@Data
public class PolicyQueryParamsDTO {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 身份证号
     */
    private String idCardNo;
    
    /**
     * 保单号
     */
    private String polNo;
    
    /**
     * 投保人姓名
     */
    private String policyHolderName;
    
    /**
     * 是否提取到有效参数
     */
    public boolean hasValidParams() {
        return (userId != null && !userId.isEmpty()) ||
               (idCardNo != null && !idCardNo.isEmpty()) ||
               (polNo != null && !polNo.isEmpty());
    }
}