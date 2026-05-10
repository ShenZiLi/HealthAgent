package com.healthagent.controller;

import com.healthagent.common.Result;
import com.healthagent.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI聊天控制器
 */
@Tag(name = "AI聊天接口")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private AiService aiService;

    @Operation(summary = "健康助手聊天")
    @PostMapping("/health")
    public Result<String> healthChat(@RequestBody String message) {
        String response = aiService.healthChat(message);
        return Result.success(response);
    }

    @Operation(summary = "通用聊天")
    @PostMapping
    public Result<String> chat(@RequestBody String message) {
        String response = aiService.chat(message);
        return Result.success(response);
    }

    @Operation(summary = "带系统提示词的聊天")
    @PostMapping("/custom")
    public Result<String> customChat(@RequestParam String systemPrompt, @RequestBody String message) {
        String response = aiService.chatWithSystemPrompt(systemPrompt, message);
        return Result.success(response);
    }
}
