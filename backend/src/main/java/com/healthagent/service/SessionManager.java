package com.healthagent.service;

import ai.z.openapi.service.model.ChatMessage;
import com.healthagent.common.IntentType;
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
    private final Map<String, IntentType> userIntentCache = new ConcurrentHashMap<>();

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

    /**
     * 获取用户的意图（如果已识别过则返回缓存的意图）
     *
     * @param userId 用户ID
     * @return 意图类型，如果未识别过则返回 null
     */
    public IntentType getCachedIntent(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        return userIntentCache.get(userId);
    }

    /**
     * 缓存用户的意图
     *
     * @param userId 用户ID
     * @param intent 意图类型
     */
    public void cacheIntent(String userId, IntentType intent) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        userIntentCache.put(userId, intent);
        log.info("缓存用户 {} 的意图: {}", userId, intent.getDesc());
    }

    /**
     * 清除用户的意图缓存
     *
     * @param userId 用户ID
     */
    public void clearIntentCache(String userId) {
        if (userId != null && !userId.isEmpty()) {
            userIntentCache.remove(userId);
            log.info("清除用户 {} 的意图缓存", userId);
        }
    }
}
