package com.healthagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("examination_hospital")
public class ExaminationHospitalEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("hospital_code")
    private String hospitalCode;

    @TableField("hospital_name")
    private String hospitalName;

    @TableField("hospital_level")
    private String hospitalLevel;

    @TableField("address")
    private String address;

    @TableField("phone")
    private String phone;

    @TableField("department")
    private String department;

    @TableField("available_slots")
    private Integer availableSlots;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
