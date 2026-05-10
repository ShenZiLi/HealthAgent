package com.healthagent.dto;

import lombok.Data;

/**
 * 保单查询请求 DTO
 */
@Data
public class PolicyQueryRequest {
    
    /**
     * 保单号
     */
    private String polNo;
    
    /**
     * 投保人姓名
     */
    private String policyHolderName;
    
    /**
     * 身份证号
     */
    private String idCardNo;
    
    /**
     * 保单状态
     */
    private String status;
}
