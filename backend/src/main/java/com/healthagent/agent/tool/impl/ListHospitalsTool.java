package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.dto.ExaminationHospitalDTO;
import com.healthagent.service.ExaminationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ListHospitalsTool implements Tool {
    private final ExaminationService examinationService;

    public ListHospitalsTool(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @Override
    public String getName() {
        return "list_hospitals";
    }

    @Override
    public String getDescription() {
        return "查询可用的体检医院列表";
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setType("object");
        schema.setProperties(new HashMap<>());
        schema.setRequired(Collections.emptyList());
        return schema;
    }

    @Override
    public ToolResult execute(ToolInput input) {
        try {
            List<ExaminationHospitalDTO> hospitals = examinationService.getAvailableHospitals();

            StringBuilder sb = new StringBuilder();
            sb.append("可用医院列表：\n\n");
            for (ExaminationHospitalDTO hospital : hospitals) {
                sb.append("- ").append(hospital.getHospitalName());
                if (hospital.getHospitalLevel() != null) {
                    sb.append(" (").append(hospital.getHospitalLevel()).append(")");
                }
                sb.append(" [ID: ").append(hospital.getId()).append("]");
                sb.append("\n");
            }

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("ListHospitalsTool execution failed", e);
            return ToolResult.error("查询医院列表失败：" + e.getMessage());
        }
    }
}
