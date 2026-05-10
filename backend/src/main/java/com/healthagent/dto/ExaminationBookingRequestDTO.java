package com.healthagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExaminationBookingRequestDTO {

    @NotNull(message = "体检计划ID不能为空")
    private Long planId;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "登记人姓名不能为空")
    private String bookerName;

    @NotBlank(message = "登记人电话不能为空")
    private String bookerPhone;

    private String idCardNo;

    private String notes;
}
