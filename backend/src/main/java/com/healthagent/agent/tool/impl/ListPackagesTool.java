package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.dto.ExaminationPackageDTO;
import com.healthagent.service.ExaminationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ListPackagesTool implements Tool {
    private final ExaminationService examinationService;

    public ListPackagesTool(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @Override
    public String getName() {
        return "list_packages";
    }

    @Override
    public String getDescription() {
        return "查询体检套餐列表";
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
            List<ExaminationPackageDTO> packages = examinationService.getAvailablePackages();

            StringBuilder sb = new StringBuilder();
            sb.append("可用套餐列表：\n\n");
            for (ExaminationPackageDTO pkg : packages) {
                sb.append("- ").append(pkg.getName());
                sb.append(" ¥").append(pkg.getPrice());
                if (pkg.getDescription() != null) {
                    sb.append(" - ").append(pkg.getDescription());
                }
                sb.append(" [ID: ").append(pkg.getId()).append("]");
                sb.append("\n");
            }

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("ListPackagesTool execution failed", e);
            return ToolResult.error("查询套餐列表失败：" + e.getMessage());
        }
    }
}
