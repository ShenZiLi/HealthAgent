package com.healthagent.service;

import ai.z.openapi.service.model.ChatMessage;
import com.healthagent.agent.ConversationState;
import com.healthagent.common.IntentType;
import com.healthagent.dto.ExaminationIntentData;
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
    private final Map<String, ExaminationIntentData> examinationBookingCache = new ConcurrentHashMap<>();
    private final Map<String, ConversationState> conversationStateCache = new ConcurrentHashMap<>();

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

    public ExaminationIntentData getCachedExaminationIntent(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        return examinationBookingCache.get(userId);
    }

    public void updateCachedExaminationIntent(String userId, ExaminationIntentData newData) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        ExaminationIntentData existing = examinationBookingCache.get(userId);
        ExaminationIntentData merged;
        
        if (existing == null) {
            merged = newData;
        } else {
            merged = mergeExaminationIntentData(existing, newData);
        }
        
        examinationBookingCache.put(userId, merged);
        log.info("更新用户 {} 的体检预约缓存: {}", userId, merged);
    }

    private ExaminationIntentData mergeExaminationIntentData(ExaminationIntentData existing, ExaminationIntentData newData) {
        ExaminationIntentData merged = new ExaminationIntentData();
        
        merged.setIntent(newData.getIntent() != null ? newData.getIntent() : existing.getIntent());
        merged.setHospitalName(
            isNotBlank(newData.getHospitalName()) ? newData.getHospitalName() : existing.getHospitalName()
        );
        merged.setHospitalCode(
            isNotBlank(newData.getHospitalCode()) ? newData.getHospitalCode() : existing.getHospitalCode()
        );
        merged.setExaminationDate(
            isNotBlank(newData.getExaminationDate()) ? newData.getExaminationDate() : existing.getExaminationDate()
        );
        merged.setExaminationTime(
            isNotBlank(newData.getExaminationTime()) ? newData.getExaminationTime() : existing.getExaminationTime()
        );
        merged.setPackageType(
            isNotBlank(newData.getPackageType()) ? newData.getPackageType() : existing.getPackageType()
        );
        merged.setNotes(
            isNotBlank(newData.getNotes()) ? newData.getNotes() : existing.getNotes()
        );
        merged.setConfidence(newData.getConfidence() != null ? newData.getConfidence() : existing.getConfidence());
        merged.setNeedsMoreInfo(newData.getNeedsMoreInfo() != null ? newData.getNeedsMoreInfo() : existing.getNeedsMoreInfo());
        merged.setMissingFields(newData.getMissingFields() != null ? newData.getMissingFields() : existing.getMissingFields());
        merged.setBookingReady(
            isNotBlank(merged.getHospitalName()) && isNotBlank(merged.getExaminationDate())
        );
        
        return merged;
    }

    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public void clearExaminationBookingCache(String userId) {
        if (userId != null && !userId.isEmpty()) {
            examinationBookingCache.remove(userId);
            log.info("清除用户 {} 的体检预约缓存", userId);
        }
    }

    public ConversationState getOrCreateConversationState(String userId) {
        return conversationStateCache.computeIfAbsent(userId, k -> new ConversationState());
    }

    public void saveConversationState(String userId, ConversationState state) {
        conversationStateCache.put(userId, state);
        log.info("保存用户 {} 的会话状态", userId);
    }

    public void clearConversationState(String userId) {
        if (userId != null && !userId.isEmpty()) {
            conversationStateCache.remove(userId);
            log.info("清除用户 {} 的会话状态", userId);
        }
    }
}
