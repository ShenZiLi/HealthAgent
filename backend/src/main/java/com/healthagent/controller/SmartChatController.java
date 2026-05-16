package com.healthagent.controller;

import com.healthagent.agent.ReActAgentOrchestrator;
import com.healthagent.common.Result;
import com.healthagent.dto.SmartChatRequest;
import com.healthagent.dto.SmartChatResponse;
import com.healthagent.service.SmartChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "智能对话接口")
@RestController
@RequestMapping("/api/smart-chat")
public class SmartChatController {

    @Autowired
    private SmartChatService smartChatService;

    @Autowired
    private ReActAgentOrchestrator reActAgentOrchestrator;

    @Operation(summary = "发送智能对话消息")
    @PostMapping("/send")
    public Result<SmartChatResponse> sendSmartMessage(@Valid @RequestBody SmartChatRequest request) {
        try {
            log.info("收到智能对话请求: message={}, userId={}", request.getMessage(), request.getUserId());
            
            SmartChatResponse response = reActAgentOrchestrator.execute(request);
            
            log.info("智能对话响应: intent={}, action={}", response.getIntent(), response.getAction());
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("智能对话处理失败", e);
            return Result.error("对话处理失败: " + e.getMessage());
        }
    }
}
