package com.healthagent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExaminationPlanDTO {
    private Long id;
    private Long hospitalId;
    private Long packageId;
    private String planName;
    private String hospitalName;
    private String packageName;
    private LocalDate scheduleDate;
    private String scheduleTime;
    private Integer totalSlots;
    private Integer availableSlots;
    private BigDecimal price;
    private Integer status;
}
