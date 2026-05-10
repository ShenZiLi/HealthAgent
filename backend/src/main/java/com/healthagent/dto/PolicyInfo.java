package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyInfo {
    
    private String policyId;
    
    private String policyType;
    
    private String policyName;
    
    private String status;
    
    private BigDecimal premium;
    
    private BigDecimal coverage;
    
    private Date startDate;
    
    private Date endDate;
    
    private String insuranceCompany;
}
