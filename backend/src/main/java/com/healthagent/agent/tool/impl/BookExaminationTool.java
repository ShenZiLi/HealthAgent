package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.dto.ExaminationBookingDTO;
import com.healthagent.dto.ExaminationBookingRequestDTO;
import com.healthagent.service.ExaminationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class BookExaminationTool implements Tool {
    private final ExaminationService examinationService;

    public BookExaminationTool(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @Override
    public String getName() {
        return "book_examination";
    }

    @Override
    public String getDescription() {
        return "创建体检预约";
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setType("object");

        Map<String, ToolSchema.Property> properties = new HashMap<>();

        ToolSchema.Property userIdProp = new ToolSchema.Property();
        userIdProp.setType("string");
        userIdProp.setDescription("用户ID");
        properties.put("userId", userIdProp);

        ToolSchema.Property hospitalIdProp = new ToolSchema.Property();
        hospitalIdProp.setType("integer");
        hospitalIdProp.setDescription("医院ID");
        properties.put("hospitalId", hospitalIdProp);

        ToolSchema.Property packageIdProp = new ToolSchema.Property();
        packageIdProp.setType("integer");
        packageIdProp.setDescription("套餐ID");
        properties.put("packageId", packageIdProp);

        ToolSchema.Property dateProp = new ToolSchema.Property();
        dateProp.setType("string");
        dateProp.setDescription("预约日期 (YYYY-MM-DD)");
        properties.put("date", dateProp);

        ToolSchema.Property nameProp = new ToolSchema.Property();
        nameProp.setType("string");
        nameProp.setDescription("预约人姓名");
        properties.put("name", nameProp);

        ToolSchema.Property phoneProp = new ToolSchema.Property();
        phoneProp.setType("string");
        phoneProp.setDescription("联系电话");
        properties.put("phone", phoneProp);

        schema.setProperties(properties);
        schema.setRequired(List.of("userId", "hospitalId", "packageId", "date", "name", "phone"));

        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        return true;
    }

    @Override
    public ToolResult execute(ToolInput input) {
        try {
            ExaminationBookingRequestDTO request = new ExaminationBookingRequestDTO();
            request.setUserId((String) input.getParameters().get("userId"));
            request.setHospitalId(((Number) input.getParameters().get("hospitalId")).longValue());
            request.setPackageId(((Number) input.getParameters().get("packageId")).longValue());
            request.setScheduleDate(LocalDate.parse((String) input.getParameters().get("date")));
            request.setBookerName((String) input.getParameters().get("name"));
            request.setBookerPhone((String) input.getParameters().get("phone"));

            ExaminationBookingDTO booking = examinationService.bookExamination(request);

            return ToolResult.success(String.format("""
                预约成功！
                
                预约号：%s
                医院：%s
                套餐：%s
                日期：%s
                """,
                    booking.getBookingNo(),
                    booking.getHospitalName(),
                    booking.getPackageName(),
                    booking.getScheduleDate()
            ));
        } catch (Exception e) {
            log.error("BookExaminationTool execution failed", e);
            return ToolResult.error("预约失败：" + e.getMessage());
        }
    }
}
