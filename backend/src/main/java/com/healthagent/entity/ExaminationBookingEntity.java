package com.healthagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("examination_booking")
public class ExaminationBookingEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("booking_no")
    private String bookingNo;

    @TableField("plan_id")
    private Long planId;

    @TableField("user_id")
    private String userId;

    @TableField("booker_name")
    private String bookerName;

    @TableField("booker_phone")
    private String bookerPhone;

    @TableField("id_card_no")
    private String idCardNo;

    @TableField("notes")
    private String notes;

    @TableField("status")
    private String status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
