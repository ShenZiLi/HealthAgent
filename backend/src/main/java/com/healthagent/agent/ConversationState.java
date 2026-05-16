package com.healthagent.agent;

import com.healthagent.common.IntentType;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ConversationState {
    private IntentType currentIntent;
    private List<ChatMessage> history = new ArrayList<>();
    private List<ReActStep> reActSteps = new ArrayList<>();
    private Map<String, Object> taskState = new HashMap<>();
    private long lastActiveTime;

    public ConversationState() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    public void addMessage(ChatMessage message) {
        history.add(message);
        if (history.size() > 50) {
            history = history.subList(history.size() - 50, history.size());
        }
        lastActiveTime = System.currentTimeMillis();
    }

    public void addSteps(List<ReActStep> steps) {
        reActSteps.addAll(steps);
        lastActiveTime = System.currentTimeMillis();
    }

    public void resetTaskState() {
        this.taskState.clear();
        this.currentIntent = null;
    }
}
