package com.healthagent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExaminationHospitalDTO {
    private Long id;
    private String hospitalCode;
    private String hospitalName;
    private String hospitalLevel;
    private String address;
    private String phone;
    private String department;
    private Integer availableSlots;
}
