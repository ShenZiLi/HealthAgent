package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.dto.ExaminationBookingDTO;
import com.healthagent.dto.ExaminationBookingRequestDTO;
import com.healthagent.dto.ExaminationHospitalDTO;
import com.healthagent.dto.ExaminationPackageDTO;
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
        return "创建体检预约。可以使用 hospitalName（医院名称）和 packageName（套餐名称）来预约，系统会自动匹配对应的ID。";
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
        hospitalIdProp.setDescription("医院ID（可选，如果提供了 hospitalName 则不需要）");
        properties.put("hospitalId", hospitalIdProp);

        ToolSchema.Property hospitalNameProp = new ToolSchema.Property();
        hospitalNameProp.setType("string");
        hospitalNameProp.setDescription("医院名称（如：上海瑞金医院体检中心）");
        properties.put("hospitalName", hospitalNameProp);

        ToolSchema.Property packageIdProp = new ToolSchema.Property();
        packageIdProp.setType("integer");
        packageIdProp.setDescription("套餐ID（可选，如果提供了 packageName 则不需要）");
        properties.put("packageId", packageIdProp);

        ToolSchema.Property packageNameProp = new ToolSchema.Property();
        packageNameProp.setType("string");
        packageNameProp.setDescription("套餐名称（如：基础体检套餐）");
        properties.put("packageName", packageNameProp);

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
        schema.setRequired(List.of("userId", "date", "name", "phone"));

        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        return true;
    }

    @Override
    public ToolResult execute(ToolInput input) {
        try {
            String userId = (String) input.getParameters().get("userId");
            String dateStr = (String) input.getParameters().get("date");
            String bookerName = (String) input.getParameters().get("name");
            String bookerPhone = (String) input.getParameters().get("phone");

            if (userId == null || userId.isBlank()) {
                return ToolResult.error("预约失败：缺少用户ID参数");
            }
            if (dateStr == null || dateStr.isBlank()) {
                return ToolResult.error("预约失败：缺少预约日期参数");
            }
            if (bookerName == null || bookerName.isBlank()) {
                return ToolResult.error("预约失败：缺少预约人姓名参数");
            }
            if (bookerPhone == null || bookerPhone.isBlank()) {
                return ToolResult.error("预约失败：缺少联系电话参数");
            }

            Long hospitalId = resolveHospitalId(input.getParameters());
            if (hospitalId == null) {
                return ToolResult.error("预约失败：未找到匹配的医院，请提供正确的医院名称或医院ID。可使用 list_hospitals 工具查询可用医院。");
            }

            Long packageId = resolvePackageId(input.getParameters());
            if (packageId == null) {
                return ToolResult.error("预约失败：未找到匹配的套餐，请提供正确的套餐名称或套餐ID。可使用 list_packages 工具查询可用套餐。");
            }

            ExaminationBookingRequestDTO request = new ExaminationBookingRequestDTO();
            request.setUserId(userId);
            request.setHospitalId(hospitalId);
            request.setPackageId(packageId);
            request.setScheduleDate(LocalDate.parse(dateStr));
            request.setBookerName(bookerName);
            request.setBookerPhone(bookerPhone);

            ExaminationBookingDTO booking = examinationService.bookExamination(request);

            return ToolResult.success(String.format("""
                预约成功！
                
                预约号：%s
                医院ID：%d
                套餐ID：%d
                日期：%s
                """,
                    booking.getBookingNo(),
                    hospitalId,
                    packageId,
                    booking.getScheduleDate()
            ));
        } catch (Exception e) {
            log.error("BookExaminationTool execution failed", e);
            return ToolResult.error("预约失败：" + e.getMessage());
        }
    }

    private Long resolveHospitalId(Map<String, Object> params) {
        Object hospitalIdObj = params.get("hospitalId");
        if (hospitalIdObj != null) {
            return ((Number) hospitalIdObj).longValue();
        }

        String hospitalName = (String) params.get("hospitalName");
        if (hospitalName == null || hospitalName.isBlank()) {
            hospitalName = (String) params.get("hospital");
        }
        if (hospitalName != null && !hospitalName.isBlank()) {
            List<ExaminationHospitalDTO> hospitals = examinationService.searchHospitals(hospitalName);
            if (!hospitals.isEmpty()) {
                return hospitals.get(0).getId();
            }
        }

        return null;
    }

    private Long resolvePackageId(Map<String, Object> params) {
        Object packageIdObj = params.get("packageId");
        if (packageIdObj != null) {
            return ((Number) packageIdObj).longValue();
        }

        String packageName = (String) params.get("packageName");
        if (packageName == null || packageName.isBlank()) {
            packageName = (String) params.get("package");
        }
        if (packageName != null && !packageName.isBlank()) {
            List<ExaminationPackageDTO> packages = examinationService.getAvailablePackages();
            for (ExaminationPackageDTO pkg : packages) {
                if (pkg.getPackageName().contains(packageName)) {
                    return pkg.getId();
                }
            }
        }

        return null;
    }
}
