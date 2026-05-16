package com.healthagent.agent;

import com.healthagent.common.IntentType;
import com.healthagent.dto.SmartChatRequest;
import com.healthagent.dto.SmartChatResponse;
import com.healthagent.service.IntentRecognitionService;
import com.healthagent.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ReActAgentOrchestrator {
    private final IntentRecognitionService intentRecognitionService;
    private final SessionManager sessionManager;
    private final ReActLoop reActLoop;

    private static final Set<String> SWITCH_KEYWORDS = Set.of(
        "换一个", "换个话题", "我想", "我要", "查询", "预约",
        "另外", "其他", "别的", "重新"
    );

    public ReActAgentOrchestrator(IntentRecognitionService intentRecognitionService,
                                   SessionManager sessionManager,
                                   ReActLoop reActLoop) {
        this.intentRecognitionService = intentRecognitionService;
        this.sessionManager = sessionManager;
        this.reActLoop = reActLoop;
    }

    public SmartChatResponse execute(SmartChatRequest request) {
        String userId = request.getUserId();
        String userMessage = request.getMessage();

        log.info("Processing request for user: {}, message: {}", userId, userMessage);

        ConversationState state = sessionManager.getOrCreateConversationState(userId);

        state.addMessage(new ChatMessage("user", userMessage));

        if (shouldSwitchIntent(userMessage, state)) {
            log.info("Switching intent for user: {}", userId);
            state.resetTaskState();
            sessionManager.clearIntentCache(userId);
        }

        IntentType intent = state.getCurrentIntent();
        if (intent == null) {
            intent = intentRecognitionService.recognizeIntent(userMessage);
            state.setCurrentIntent(intent);
            sessionManager.cacheIntent(userId, intent);
        }

        SmartChatResponse response;
        if (shouldUseReAct(intent)) {
            log.info("Using ReAct mode for intent: {}", intent);
            response = executeReAct(state, request);
        } else {
            log.info("Using simple mode for intent: {}", intent);
            response = executeSimple(state, request);
        }

        state.addMessage(new ChatMessage("assistant", response.getMessage()));
        sessionManager.saveConversationState(userId, state);

        return response;
    }

    private boolean shouldSwitchIntent(String message, ConversationState state) {
        if (state.getCurrentIntent() == null) {
            return false;
        }
        return SWITCH_KEYWORDS.stream().anyMatch(message::contains);
    }

    private boolean shouldUseReAct(IntentType intent) {
        return intent == IntentType.QUERY_POLICY || intent == IntentType.BOOK_EXAMINATION;
    }

    private SmartChatResponse executeReAct(ConversationState state, SmartChatRequest request) {
        UserInput input = new UserInput();
        input.setUserId(request.getUserId());
        input.setContent(request.getMessage());

        ReActResult result = reActLoop.run(state, input);

        SmartChatResponse response = new SmartChatResponse();
        response.setIntent(state.getCurrentIntent() != null ?
            state.getCurrentIntent().getCode() : null);
        response.setMessage(result.getFinalAnswer());
        response.setAction("react_completed");
        response.setMessageType("text");

        List<String> stepLogs = result.getSteps().stream()
            .map(step -> step.getType() + ": " + step.getContent())
            .toList();
        response.setData(stepLogs);

        return response;
    }

    private SmartChatResponse executeSimple(ConversationState state, SmartChatRequest request) {
        SmartChatResponse response = new SmartChatResponse();
        response.setIntent(state.getCurrentIntent() != null ?
            state.getCurrentIntent().getCode() : null);
        response.setMessage("这是一个简单回复。您可以问我关于保单查询或体检预约的问题。");
        response.setAction("simple_response");
        response.setMessageType("text");
        return response;
    }
}
