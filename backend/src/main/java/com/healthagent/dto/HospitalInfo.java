package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HospitalInfo {
    
    private String hospitalCode;
    
    private String hospitalName;
    
    private String hospitalLevel;
    
    private String address;
    
    private String phone;
    
    private String department;
    
    private Integer availableSlots;
}
