package com.healthagent.controller;

import com.healthagent.common.Result;
import com.healthagent.dto.ChatRequest;
import com.healthagent.dto.ChatResponse;
import com.healthagent.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "对话接口")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Operation(summary = "发送对话消息")
    @PostMapping("/send")
    public Result<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.chat(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("对话请求失败", e);
            return Result.error("对话失败: " + e.getMessage());
        }
    }

    @Operation(summary = "发送对话消息(流式)")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String sendMessageStream(@Valid @RequestBody ChatRequest request) {
        try {
            return chatService.chatStream(request);
        } catch (Exception e) {
            log.error("流式对话请求失败", e);
            return "data: {\"error\": \"" + e.getMessage() + "\"}\n\n";
        }
    }
}
