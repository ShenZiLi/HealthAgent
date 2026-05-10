package com.healthagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExaminationBookingRequestDTO {

    private Long planId;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "登记人姓名不能为空")
    private String bookerName;

    @NotBlank(message = "登记人电话不能为空")
    private String bookerPhone;

    private String idCardNo;

    private String notes;

    private LocalDate scheduleDate;

    private Long hospitalId;

    private Long packageId;
}
