package com.healthagent.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExaminationPackageDTO {
    private Long id;
    private String packageCode;
    private String packageName;
    private String packageDesc;
    private BigDecimal price;
    private String duration;
}
