package com.healthagent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExaminationBookingDTO {
    private Long id;
    private String bookingNo;
    private Long hospitalId;
    private Long packageId;
    private String hospitalName;
    private String hospitalLevel;
    private String hospitalAddress;
    private String hospitalPhone;
    private String packageName;
    private String packageDesc;
    private LocalDate scheduleDate;
    private BigDecimal price;
    private String userId;
    private String bookerName;
    private String bookerPhone;
    private String idCardNo;
    private String notes;
    private String status;
    private LocalDateTime createTime;
}
