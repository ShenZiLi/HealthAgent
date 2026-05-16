package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.dto.ExaminationBookingDTO;
import com.healthagent.service.ExaminationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class QueryBookingTool implements Tool {
    private final ExaminationService examinationService;

    public QueryBookingTool(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @Override
    public String getName() {
        return "query_booking";
    }

    @Override
    public String getDescription() {
        return "查询用户的体检预约记录。只需要 userId 参数，系统会自动查询该用户的所有预约记录。";
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

        ToolSchema.Property statusProp = new ToolSchema.Property();
        statusProp.setType("string");
        statusProp.setEnumValues(List.of("all", "pending", "confirmed", "completed", "cancelled"));
        statusProp.setDescription("预约状态过滤（可选，默认为all）");
        properties.put("status", statusProp);

        schema.setProperties(properties);
        schema.setRequired(List.of("userId"));

        return schema;
    }

    @Override
    public ToolResult execute(ToolInput input) {
        try {
            String userId = (String) input.getParameters().get("userId");
            String statusFilter = (String) input.getParameters().getOrDefault("status", "all");

            log.info("Executing QueryBookingTool: userId={}, status={}", userId, statusFilter);

            if (userId == null || userId.isBlank()) {
                return ToolResult.error("查询失败：缺少用户ID参数");
            }

            List<ExaminationBookingDTO> bookings = examinationService.getUserBookings(userId);

            if (statusFilter != null && !"all".equalsIgnoreCase(statusFilter)) {
                String finalStatus = statusFilter;
                bookings = bookings.stream()
                        .filter(b -> finalStatus.equalsIgnoreCase(b.getStatus()))
                        .toList();
            }

            if (bookings.isEmpty()) {
                return ToolResult.success("您暂无体检预约记录。如需预约体检，请告诉我您希望预约的医院和时间。");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("您共有 ").append(bookings.size()).append(" 条体检预约记录：\n\n");
            for (int i = 0; i < bookings.size(); i++) {
                ExaminationBookingDTO b = bookings.get(i);
                String statusLabel = switch (b.getStatus()) {
                    case "pending" -> "待确认";
                    case "confirmed" -> "已确认";
                    case "completed" -> "已完成";
                    case "cancelled" -> "已取消";
                    default -> b.getStatus();
                };

                sb.append(i + 1).append(". ").append(b.getBookingNo()).append("\n");
                sb.append("   医院: ").append(b.getHospitalName() != null ? b.getHospitalName() : "未知").append("\n");
                if (b.getHospitalLevel() != null) {
                    sb.append("   医院等级: ").append(b.getHospitalLevel()).append("\n");
                }
                if (b.getHospitalAddress() != null) {
                    sb.append("   医院地址: ").append(b.getHospitalAddress()).append("\n");
                }
                if (b.getHospitalPhone() != null) {
                    sb.append("   联系电话: ").append(b.getHospitalPhone()).append("\n");
                }
                sb.append("   套餐: ").append(b.getPackageName() != null ? b.getPackageName() : "未知").append("\n");
                if (b.getPackageDesc() != null) {
                    sb.append("   套餐说明: ").append(b.getPackageDesc()).append("\n");
                }
                sb.append("   日期: ").append(b.getScheduleDate() != null ? b.getScheduleDate() : "未设定").append("\n");
                sb.append("   状态: ").append(statusLabel).append("\n");
                if (b.getPrice() != null) {
                    sb.append("   价格: ¥").append(b.getPrice()).append("\n");
                }
                sb.append("\n");
            }

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("QueryBookingTool execution failed", e);
            return ToolResult.error("查询预约记录失败：" + e.getMessage());
        }
    }
}
