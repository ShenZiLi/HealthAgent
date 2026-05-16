package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.dto.ExaminationBookingDTO;
import com.healthagent.dto.ExaminationBookingRequestDTO;
import com.healthagent.dto.ExaminationHospitalDTO;
import com.healthagent.dto.ExaminationPackageDTO;
import com.healthagent.service.ExaminationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
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
        return "创建体检预约。必填参数：userId、date(YYYY-MM-DD)、name(预约人姓名)、phone(联系电话)。" +
               "使用 hospitalName（如'北京协和医院'）或 hospitalId 指定医院；" +
               "使用 packageName（如'入职体检套餐'）或 packageId 指定套餐。" +
               "可用套餐：入职体检套餐、基础体检套餐、全身体检套餐、老年体检套餐、女性专项体检套餐。" +
               "如果用户未提供姓名或电话，请向用户追问，不要使用默认值。";
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
            //if (bookerName == null || bookerName.isBlank()) {
            //    return ToolResult.error("预约失败：缺少预约人姓名参数");
            //}
            //if (bookerPhone == null || bookerPhone.isBlank()) {
            //    return ToolResult.error("预约失败：缺少联系电话参数");
            //}

            Long hospitalId = resolveHospitalId(input.getParameters());
            if (hospitalId == null) {
                List<ExaminationHospitalDTO> hospitals = examinationService.getAvailableHospitals();
                StringBuilder sb = new StringBuilder("预约失败：未找到匹配的医院。");
                sb.append("\n请向用户询问选择以下可用医院之一：");
                for (ExaminationHospitalDTO h : hospitals) {
                    sb.append("\n- ").append(h.getHospitalName()).append(" (地址：").append(h.getAddress()).append(")");
                }
                sb.append("\n\n请使用 hospitalName 参数指定医院名称，或告知用户从中选择一家医院。");
                return ToolResult.error(sb.toString());
            }

            Long packageId = resolvePackageId(input.getParameters());
            if (packageId == null) {
                List<ExaminationPackageDTO> packages = examinationService.getAvailablePackages();
                StringBuilder sb = new StringBuilder("预约失败：未找到匹配的套餐。");
                sb.append("\n请向用户询问选择以下可用套餐之一：");
                for (ExaminationPackageDTO pkg : packages) {
                    sb.append("\n- ").append(pkg.getPackageName()).append(" ¥").append(pkg.getPrice());
                    if (pkg.getPackageDesc() != null) {
                        sb.append(" - ").append(pkg.getPackageDesc());
                    }
                }
                sb.append("\n\n请使用 packageName 参数指定套餐名称（必须使用上方列表中的准确名称），或告知用户从中选择一个套餐。");
                return ToolResult.error(sb.toString());
            }

            ExaminationBookingRequestDTO request = new ExaminationBookingRequestDTO();
            request.setUserId(userId);
            request.setHospitalId(hospitalId);
            request.setPackageId(packageId);
            request.setScheduleDate(resolveDate(dateStr));
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
                String searchName = hospitalName.toLowerCase().trim();
                for (ExaminationHospitalDTO hospital : hospitals) {
                    String dbName = hospital.getHospitalName().toLowerCase();
                    if (dbName.equals(searchName) || dbName.contains(searchName)) {
                        return hospital.getId();
                    }
                }
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
        if (packageName == null || packageName.isBlank()) {
            return null;
        }

        List<ExaminationPackageDTO> packages = examinationService.getAvailablePackages();

        for (ExaminationPackageDTO pkg : packages) {
            if (pkg.getPackageName().contains(packageName)) {
                return pkg.getId();
            }
        }

        for (ExaminationPackageDTO pkg : packages) {
            String pkgName = pkg.getPackageName().toLowerCase();
            String searchName = packageName.toLowerCase();
            if (pkgName.contains("入职") && searchName.contains("入职")) return pkg.getId();
            if (pkgName.contains("基础") && searchName.contains("基础")) return pkg.getId();
            if (pkgName.contains("全身") && searchName.contains("全身")) return pkg.getId();
            if (pkgName.contains("老年") && searchName.contains("老年")) return pkg.getId();
            if (pkgName.contains("女性") && searchName.contains("女性")) return pkg.getId();
            if (pkgName.contains("标准") && searchName.contains("标准")) return pkg.getId();
            if (pkgName.contains("全面") && searchName.contains("全面")) return pkg.getId();
            if (pkgName.contains("全身体检") && (searchName.contains("全身") || searchName.contains("全面") || searchName.contains("全项"))) return pkg.getId();
            if (pkgName.contains("基础体检") && (searchName.contains("基础") || searchName.contains("普通") || searchName.contains("标准"))) return pkg.getId();
        }

        if (!packages.isEmpty()) {
            return packages.get(0).getId();
        }

        return null;
    }

    private LocalDate resolveDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("预约日期不能为空");
        }

        dateStr = dateStr.trim();

        if (dateStr.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(dateStr);
            if (m.find()) {
                return LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        }

        LocalDate today = LocalDate.now();
        String lower = dateStr.toLowerCase();

        switch (lower) {
            case "今天":
                return today;
            case "明天":
                return today.plusDays(1);
            case "后天":
                return today.plusDays(2);
            case "大后天":
                return today.plusDays(3);
            case "昨天":
                return today.minusDays(1);
            case "前天":
                return today.minusDays(2);
        }

        if (lower.contains("下周末")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).plusWeeks(1);
        }
        if (lower.contains("上周末")) {
            return today.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        }
        if (lower.contains("周末") || lower.contains("这周末") || lower.contains("本周末")) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        }

        if (lower.contains("下周一")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        if (lower.contains("下周二")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        }
        if (lower.contains("下周三")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        }
        if (lower.contains("下周四")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        }
        if (lower.contains("下周五")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        }
        if (lower.contains("下周六")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        }
        if (lower.contains("下周日")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        }
        if (lower.contains("下周")) {
            return today.plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        }

        if (lower.contains("本周一") || lower.contains("这周一")) {
            return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if (lower.contains("本周二") || lower.contains("这周二")) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        }
        if (lower.contains("本周三") || lower.contains("这周三")) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        }
        if (lower.contains("本周四") || lower.contains("这周四")) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
        }
        if (lower.contains("本周五") || lower.contains("这周五")) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        }
        if (lower.contains("本周六") || lower.contains("这周六")) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        }
        if (lower.contains("本周日") || lower.contains("这周日")) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }
        if (lower.contains("这周") || lower.contains("本周")) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        }

        if (lower.contains("下个月底") || lower.contains("下月末")) {
            return today.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        }
        if (lower.contains("月底") || lower.contains("月末")) {
            return today.with(TemporalAdjusters.lastDayOfMonth());
        }

        if (lower.contains("下月初") || lower.contains("下月头")) {
            return today.plusMonths(1).withDayOfMonth(1);
        }
        if (lower.contains("月初") || lower.contains("月头")) {
            return today.withDayOfMonth(1);
        }

        if (lower.contains("下个月") || lower.contains("次月")) {
            return today.plusMonths(1);
        }
        if (lower.contains("这月") || lower.contains("本月")) {
            return today;
        }

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            log.warn("无法解析日期字符串: {}, 默认为明天", dateStr);
            return today.plusDays(1);
        }
    }
}
