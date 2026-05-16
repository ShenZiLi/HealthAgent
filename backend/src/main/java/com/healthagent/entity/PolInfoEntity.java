package com.healthagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保单信息实体
 */
@Data
@TableName("pol_info")
public class PolInfoEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("pol_no")
    private String polNo;

    @TableField("policy_holder_name")
    private String policyHolderName;

    @TableField("insured_name")
    private String insuredName;

    @TableField("id_card_no")
    private String idCardNo;

    @TableField("insurance_company")
    private String insuranceCompany;

    @TableField("product_name")
    private String productName;

    @TableField("insurance_type")
    private String insuranceType;

    @TableField("premium_amount")
    private BigDecimal premiumAmount;

    @TableField("insured_amount")
    private BigDecimal insuredAmount;

    @TableField("status")
    private String status;

    @TableField("effective_date")
    private LocalDateTime effectiveDate;

    @TableField("expiry_date")
    private LocalDateTime expiryDate;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
