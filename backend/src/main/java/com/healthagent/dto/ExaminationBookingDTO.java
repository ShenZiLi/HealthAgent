package com.healthagent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExaminationBookingDTO {
    private Long id;
    private String bookingNo;
    private Long planId;
    private String planName;
    private String hospitalName;
    private String packageName;
    private LocalDate scheduleDate;
    private String scheduleTime;
    private BigDecimal price;
    private String userId;
    private String bookerName;
    private String bookerPhone;
    private String idCardNo;
    private String notes;
    private String status;
    private LocalDateTime createTime;
}
