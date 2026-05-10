package com.healthagent.controller;

import com.healthagent.common.Result;
import com.healthagent.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI聊天控制器
 */
@Tag(name = "AI聊天接口")
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private AiService aiService;
    
    /**
     * 提示词：N
     * 上下文记忆：N
     *
     * @param message
     * @return
     */
    @Operation(summary = "通用聊天")
    @PostMapping
    public Result<String> chat(@RequestBody String message) {
        String response = aiService.chat(message);
        return Result.success(response);
    }
    
    /**
     * 提示词：自定义
     * 上下文记忆：N
     *
     * @param message
     * @return
     */
    @Operation(summary = "带系统提示词的聊天")
    @PostMapping("/custom")
    public Result<String> customChat(@RequestParam String systemPrompt, @RequestBody String message) {
        String response = aiService.chatWithSystemPrompt(systemPrompt, message);
        return Result.success(response);
    }
    
    /**
     * 提示词：健康助手人设
     * 上下文记忆：N
     *
     * @param message
     * @return
     */
    @Operation(summary = "健康助手聊天")
    @PostMapping("/health")
    public Result<String> healthChat(@RequestBody String message) {
        String response = aiService.healthChat(message);
        return Result.success(response);
    }
    
    
    @Operation(summary = "创建新会话")
    @PostMapping("/session")
    public Result<String> createSession() {
        String sessionId = aiService.createSession();
        return Result.success(sessionId);
    }
    
    @Operation(summary = "清除会话")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        aiService.clearSession(sessionId);
        return Result.success();
    }
    
    @Operation(summary = "获取会话消息数")
    @GetMapping("/session/{sessionId}")
    public Result<Integer> getSessionMessageCount(@PathVariable String sessionId) {
        int count = aiService.getSessionMessageCount(sessionId);
        return Result.success(count);
    }
    
    @Operation(summary = "会话聊天 - 支持持续对话带上下文")
    @PostMapping("/session/{sessionId}")
    public Result<Map<String, Object>> sessionChat(@PathVariable String sessionId, @RequestBody String message) {
        String response = aiService.sessionChat(sessionId, message);
        int messageCount = aiService.getSessionMessageCount(sessionId);
        return Result.success(Map.of(
                "sessionId", sessionId,
                "message", response,
                "messageCount", messageCount
        ));
    }
}
