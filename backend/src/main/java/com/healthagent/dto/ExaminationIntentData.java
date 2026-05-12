package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationIntentData {
    
    private String intent;
    
    private String hospitalName;
    
    private String hospitalCode;
    
    private String examinationDate;
    
    private String examinationTime;
    
    private String packageType;
    
    private String notes;
    
    private Double confidence;
    
    private Boolean needsMoreInfo;
    
    private String missingFields;
    
    private boolean isBookingReady;
}
