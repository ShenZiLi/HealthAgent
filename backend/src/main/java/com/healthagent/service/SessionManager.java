package com.healthagent.service;

import ai.z.openapi.service.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SessionManager {

    private final Map<String, List<ChatMessage>> sessionHistory = new ConcurrentHashMap<>();

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        sessionHistory.put(sessionId, new ArrayList<>());
        log.info("创建新会话: {}", sessionId);
        return sessionId;
    }

    public List<ChatMessage> getSessionHistory(String sessionId) {
        return sessionHistory.getOrDefault(sessionId, new ArrayList<>());
    }

    public void addMessage(String sessionId, ChatMessage message) {
        sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);
    }

    public void clearSession(String sessionId) {
        sessionHistory.remove(sessionId);
        log.info("清除会话: {}", sessionId);
    }

    public int getMessageCount(String sessionId) {
        return sessionHistory.getOrDefault(sessionId, new ArrayList<>()).size();
    }
}
