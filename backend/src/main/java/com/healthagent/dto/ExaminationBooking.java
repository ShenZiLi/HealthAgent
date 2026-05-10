package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationBooking {
    
    private String bookingId;
    
    private String userId;
    
    private String hospitalName;
    
    private String hospitalCode;
    
    private String examinationDate;
    
    private String examinationTime;
    
    private String status;
    
    private String packageName;
    
    private String contactPhone;
    
    private String notes;
}
