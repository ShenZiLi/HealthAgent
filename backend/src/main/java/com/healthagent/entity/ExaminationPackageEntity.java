package com.healthagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("examination_package")
public class ExaminationPackageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("package_code")
    private String packageCode;

    @TableField("package_name")
    private String packageName;

    @TableField("package_desc")
    private String packageDesc;

    @TableField("price")
    private BigDecimal price;

    @TableField("duration")
    private String duration;

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
