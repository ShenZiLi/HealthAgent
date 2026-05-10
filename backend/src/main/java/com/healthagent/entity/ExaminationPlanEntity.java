package com.healthagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("examination_plan")
public class ExaminationPlanEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("hospital_id")
    private Long hospitalId;

    @TableField("package_id")
    private Long packageId;

    @TableField("plan_name")
    private String planName;

    @TableField("schedule_date")
    private LocalDate scheduleDate;

    @TableField("schedule_time")
    private String scheduleTime;

    @TableField("total_slots")
    private Integer totalSlots;

    @TableField("available_slots")
    private Integer availableSlots;

    @TableField("price")
    private BigDecimal price;

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
