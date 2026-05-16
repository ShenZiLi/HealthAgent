# ReAct Agent 重构设计文档

> 版本：v1.0  
> 日期：2026-05-16  
> 项目：HealthAgent 智慧健康助手

---

## 1. 概述

### 1.1 背景

HealthAgent 现有智能对话模块采用简单的意图识别 + 分支处理模式，虽然能满足基本需求，但在以下方面存在不足：

- 多轮对话状态管理不够灵活
- 工具调用能力有限，难以支持复杂任务
- 缺乏透明的推理过程
- 扩展新功能时需要修改核心逻辑

为了解决这些问题，我们引入 ReAct (Reasoning + Acting) Agent 设计模式进行重构。

### 1.2 目标

- 支持自然的多轮对话
- 提供强大的工具调用能力
- 实现透明的推理过程
- 保持向后兼容
- 提供良好的扩展性

### 1.3 范围

本次重构范围包括：

- SmartChat 模块的核心逻辑
- 会话管理（SessionManager）增强
- 工具（Tool）系统实现
- ReAct 循环引擎实现

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    SmartChatController                   │
└────────────────────────────┬────────────────────────────┘
                             │
                ┌────────────▼────────────┐
                │   ReActAgentOrchestrator │  ← 新增
                └────────────┬────────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
    ┌───────▼───────┐ ┌────▼────┐ ┌─────────▼─────────┐
    │ IntentRouter  │ │ Session │ │  ToolRegistry     │
    │ (现有增强)   │ │ Manager │ │                   │
    └───────┬───────┘ └────┬────┘ └─────────┬─────────┘
            │               │               │
            │       ┌───────▼───────┐       │
            └──────►│  ReActLoop   │◄──────┘
                    └───────┬───────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
    ┌───────▼───────┐ ┌─────▼─────┐ ┌─────▼──────┐
    │ ThoughtStep  │ │ActionStep │ │Observation │
    └───────────────┘ └───────────┘ └────────────┘
                            │
                    ┌───────▼────────┐
                    │  ToolExecutor  │
                    └───────┬────────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
    ┌───────▼───────┐ ┌─────▼─────┐ ┌─────▼──────┐
    │ PolicyQuery   │ │ExamBooking│ │  ...Others  │
    │    Tool       │ │   Tool    │ │             │
    └───────────────┘ └───────────┘ └────────────┘
```

### 2.2 设计原则

1. **混合架构**：保留现有意图识别作为快速路径，复杂任务使用 ReAct
2. **向后兼容**：保持现有 API 接口不变
3. **可扩展**：新工具只需注册到 ToolRegistry
4. **透明**：记录完整的推理过程便于调试
5. **容错**：完善的错误处理和降级机制

---

## 3. 核心组件设计

### 3.1 Tool 系统

#### 3.1.1 Tool 接口

```java
package com.healthagent.agent.tool;

/**
 * 工具接口，所有可调用工具必须实现此接口
 */
public interface Tool {
    /**
     * 获取工具名称
     */
    String getName();

    /**
     * 获取工具描述
     */
    String getDescription();

    /**
     * 获取工具参数 Schema（JSON Schema 格式）
     */
    ToolSchema getSchema();

    /**
     * 执行工具
     * @param input 工具输入参数
     * @return 工具执行结果
     */
    ToolResult execute(ToolInput input);

    /**
     * 是否需要用户确认（高危操作）
     */
    default boolean requiresConfirmation() {
        return false;
    }
}
```

#### 3.1.2 ToolSchema 类

```java
package com.healthagent.agent.tool;

import lombok.Data;
import java.util.Map;

/**
 * 工具参数 Schema 定义
 */
@Data
public class ToolSchema {
    private String type = "object";
    private Map<String, Property> properties;
    private java.util.List<String> required;

    @Data
    public static class Property {
        private String type;
        private String description;
        private java.util.List<String> enumValues;
        private Object defaultValue;
    }
}
```

#### 3.1.3 ToolRegistry 类

```java
package com.healthagent.agent.tool;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表，管理所有可用工具
 */
@Component
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    /**
     * 注册工具
     */
    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * 获取工具
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有可用工具的定义（用于 LLM）
     */
    public List<ToolDefinition> getToolDefinitions() {
        return tools.values().stream()
            .map(this::toToolDefinition)
            .toList();
    }

    private ToolDefinition toToolDefinition(Tool tool) {
        ToolDefinition def = new ToolDefinition();
        def.setName(tool.getName());
        def.setDescription(tool.getDescription());
        def.setParameters(tool.getSchema());
        return def;
    }

    @lombok.Data
    public static class ToolDefinition {
        private String name;
        private String description;
        private ToolSchema parameters;
    }
}
```

### 3.2 ReActLoop 核心循环

#### 3.2.1 ReActLoop 类

```java
package com.healthagent.agent;

import com.healthagent.agent.tool.*;
import com.healthagent.service.chat.AbstractChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ReAct 循环引擎
 * 执行 Thought → Action → Observation 循环
 */
@Slf4j
@Component
public class ReActLoop {
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final AbstractChatClient chatClient;

    private static final int MAX_STEPS = 10;
    private static final String REACT_SYSTEM_PROMPT = """
        你是一个健康助手 AI，擅长通过思考-行动-观察的循环来帮助用户解决问题。

        可用工具：
        {tools}

        工作流程：
        1. Thought：分析用户问题，思考需要做什么
        2. Action：选择合适的工具并调用
        3. Observation：观察工具执行结果
        4. 重复上述步骤直到问题解决
        5. Finish：给出最终答案

        输出格式（严格遵守）：
        Thought: <你的思考>
        Action: <工具名>
        Action Input: <JSON格式的参数>
        或者当问题解决时：
        Thought: <总结思考>
        Finish: <最终答案>
        """;

    public ReActLoop(ToolRegistry toolRegistry, ToolExecutor toolExecutor,
                     AbstractChatClient chatClient) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.chatClient = chatClient;
    }

    /**
     * 执行 ReAct 循环
     */
    public ReActResult run(ConversationState state, UserInput input) {
        ReActResult result = new ReActResult();
        List<ReActStep> steps = new ArrayList<>();

        for (int step = 0; step < MAX_STEPS; step++) {
            log.info("ReAct step {}", step + 1);

            // 1. 生成 Thought
            Thought thought = generateThought(state, input, steps);
            steps.add(new ReActStep(StepType.THOUGHT, thought.getContent()));

            // 2. 判断是否结束
            if (thought.isFinish()) {
                result.setFinalAnswer(thought.getFinishAnswer());
                result.setSteps(steps);
                return result;
            }

            // 3. 决定 Action
            Action action = parseAction(thought);
            steps.add(new ReActStep(StepType.ACTION, action.toString()));

            // 4. 执行 Action
            Observation observation = executeAction(action);
            steps.add(new ReActStep(StepType.OBSERVATION, observation.getContent()));

            // 5. 更新状态
            state.addSteps(steps.subList(steps.size() - 3, steps.size()));
        }

        // 超出最大步数
        result.setFinalAnswer("抱歉，我需要更多信息来帮助您。让我们换一种方式试试？");
        result.setSteps(steps);
        return result;
    }

    private Thought generateThought(ConversationState state, UserInput input,
                                    List<ReActStep> previousSteps) {
        String prompt = buildPrompt(state, input, previousSteps);
        String response = chatClient.chat(prompt, REACT_SYSTEM_PROMPT, input.getUserId());
        return parseThought(response);
    }

    private String buildPrompt(ConversationState state, UserInput input,
                               List<ReActStep> previousSteps) {
        StringBuilder sb = new StringBuilder();

        // 添加对话历史
        for (ChatMessage msg : state.getHistory()) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        // 添加当前用户输入
        sb.append("User: ").append(input.getContent()).append("\n");

        // 添加之前的 ReAct 步骤
        for (ReActStep step : previousSteps) {
            sb.append(step.getType()).append(": ").append(step.getContent()).append("\n");
        }

        return sb.toString();
    }

    private Thought parseThought(String response) {
        // 解析 LLM 输出，提取 Thought/Action/Finish
        // 实现略...
        return new Thought();
    }

    private Action parseAction(Thought thought) {
        // 从 Thought 中解析 Action 和参数
        // 实现略...
        return new Action();
    }

    private Observation executeAction(Action action) {
        ToolResult toolResult = toolExecutor.execute(action.getToolName(), action.getParameters());
        return new Observation(toolResult.getOutput());
    }
}
```

#### 3.2.2 数据类

```java
package com.healthagent.agent;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class ReActResult {
    private String finalAnswer;
    private List<ReActStep> steps;
}

@Data
public class ReActStep {
    private StepType type;
    private String content;

    public ReActStep(StepType type, String content) {
        this.type = type;
        this.content = content;
    }
}

public enum StepType {
    THOUGHT, ACTION, OBSERVATION
}

@Data
public class Thought {
    private String content;
    private boolean finish;
    private String finishAnswer;
    private String actionName;
    private Map<String, Object> actionParameters;
}

@Data
public class Action {
    private String toolName;
    private Map<String, Object> parameters;
}

@Data
public class Observation {
    private String content;
}

@Data
public class UserInput {
    private String userId;
    private String content;
    private Map<String, Object> metadata;
}
```

### 3.3 ReActAgentOrchestrator 编排器

```java
package com.healthagent.agent;

import com.healthagent.common.IntentType;
import com.healthagent.dto.SmartChatRequest;
import com.healthagent.dto.SmartChatResponse;
import com.healthagent.service.IntentRecognitionService;
import com.healthagent.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ReAct Agent 编排器
 * 协调意图识别、ReAct 循环、会话管理
 */
@Slf4j
@Service
public class ReActAgentOrchestrator {
    private final IntentRecognitionService intentRecognitionService;
    private final SessionManager sessionManager;
    private final ReActLoop reActLoop;

    public ReActAgentOrchestrator(IntentRecognitionService intentRecognitionService,
                                   SessionManager sessionManager,
                                   ReActLoop reActLoop) {
        this.intentRecognitionService = intentRecognitionService;
        this.sessionManager = sessionManager;
        this.reActLoop = reActLoop;
    }

    /**
     * 处理智能对话请求
     */
    public SmartChatResponse execute(SmartChatRequest request) {
        String userId = request.getUserId();
        String userMessage = request.getMessage();

        log.info("Processing request for user: {}, message: {}", userId, userMessage);

        // 1. 获取或创建会话状态
        ConversationState state = sessionManager.getOrCreateConversationState(userId);

        // 2. 检查是否需要切换意图
        if (shouldSwitchIntent(userMessage, state)) {
            log.info("Switching intent for user: {}", userId);
            state.resetTaskState();
        }

        // 3. 识别意图
        IntentType intent = state.getCurrentIntent();
        if (intent == null) {
            intent = intentRecognitionService.recognizeIntent(userMessage);
            state.setCurrentIntent(intent);
        }

        // 4. 判断是否使用 ReAct
        if (shouldUseReAct(intent)) {
            log.info("Using ReAct mode for intent: {}", intent);
            return executeReAct(state, request);
        } else {
            log.info("Using simple mode for intent: {}", intent);
            return executeSimple(state, request);
        }
    }

    private boolean shouldSwitchIntent(String message, ConversationState state) {
        // 检测是否有关键词表示用户想切换话题
        Set<String> switchKeywords = Set.of(
            "换一个", "换个话题", "我想", "我要", "查询", "预约"
        );
        return switchKeywords.stream().anyMatch(message::contains);
    }

    private boolean shouldUseReAct(IntentType intent) {
        return intent == IntentType.QUERY_POLICY || intent == IntentType.BOOK_EXAMINATION;
    }

    private SmartChatResponse executeReAct(ConversationState state, SmartChatRequest request) {
        UserInput input = new UserInput();
        input.setUserId(request.getUserId());
        input.setContent(request.getMessage());

        ReActResult result = reActLoop.run(state, input);

        // 保存状态
        sessionManager.saveConversationState(request.getUserId(), state);

        // 构建响应
        SmartChatResponse response = new SmartChatResponse();
        response.setIntent(state.getCurrentIntent().getCode());
        response.setMessage(result.getFinalAnswer());
        response.setAction("react_completed");
        response.setMessageType("text");

        // 可选：返回推理步骤用于调试
        response.setData(result.getSteps());

        return response;
    }

    private SmartChatResponse executeSimple(ConversationState state, SmartChatRequest request) {
        // 使用原有的简单对话逻辑
        // 实现略...
        return new SmartChatResponse();
    }
}
```

### 3.4 SessionManager 增强

```java
package com.healthagent.service;

import com.healthagent.agent.ConversationState;
import com.healthagent.agent.ReActStep;
import com.healthagent.common.IntentType;
import com.healthagent.dto.ExaminationIntentData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器（增强版）
 */
@Slf4j
@Service
public class SessionManager {
    // 原有的缓存
    private final Map<String, IntentType> userIntentCache = new ConcurrentHashMap<>();
    private final Map<String, ExaminationIntentData> examinationBookingCache = new ConcurrentHashMap<>();

    // 新增：会话状态缓存
    private final Map<String, ConversationState> conversationStateCache = new ConcurrentHashMap<>();

    /**
     * 获取或创建会话状态
     */
    public ConversationState getOrCreateConversationState(String userId) {
        return conversationStateCache.computeIfAbsent(userId, k -> new ConversationState());
    }

    /**
     * 保存会话状态
     */
    public void saveConversationState(String userId, ConversationState state) {
        conversationStateCache.put(userId, state);
    }

    // ... 保持原有方法不变
}

/**
 * 会话状态
 */
@Data
class ConversationState {
    private IntentType currentIntent;
    private List<ChatMessage> history = new ArrayList<>();
    private List<ReActStep> reActSteps = new ArrayList<>();
    private Map<String, Object> taskState = new HashMap<>();  // 任务相关状态
    private long lastActiveTime;

    public ConversationState() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    public void addMessage(ChatMessage message) {
        history.add(message);
        // 保留最近 50 条消息
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

@Data
class ChatMessage {
    private String role;  // user/assistant
    private String content;
    private long timestamp;
}
```

### 3.5 工具实现

#### 3.5.1 保单查询工具

```java
package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.dto.PolicyInfo;
import com.healthagent.service.PolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 保单查询工具
 */
@Slf4j
@Component
public class PolicyQueryTool implements Tool {
    private final PolicyService policyService;

    public PolicyQueryTool(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public String getName() {
        return "query_policy";
    }

    @Override
    public String getDescription() {
        return "查询用户的保单信息，可以按保单号、状态筛选";
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setType("object");

        Map<String, ToolSchema.Property> properties = new HashMap<>();

        ToolSchema.Property userIdProp = new ToolSchema.Property();
        userIdProp.setType("string");
        userIdProp.setDescription("用户ID");
        properties.put("userId", userIdProp);

        ToolSchema.Property policyNoProp = new ToolSchema.Property();
        policyNoProp.setType("string");
        policyNoProp.setDescription("保单号（可选）");
        properties.put("policyNo", policyNoProp);

        ToolSchema.Property statusProp = new ToolSchema.Property();
        statusProp.setType("string");
        statusProp.setEnumValues(List.of("active", "expired", "all"));
        statusProp.setDescription("保单状态（可选）");
        properties.put("status", statusProp);

        schema.setProperties(properties);
        schema.setRequired(List.of("userId"));

        return schema;
    }

    @Override
    public ToolResult execute(ToolInput input) {
        try {
            String userId = (String) input.getParameters().get("userId");
            String policyNo = (String) input.getParameters().get("policyNo");
            String status = (String) input.getParameters().getOrDefault("status", "active");

            log.info("Executing PolicyQueryTool: userId={}, policyNo={}, status={}",
                    userId, policyNo, status);

            List<PolicyInfo> policies;

            if (policyNo != null && !policyNo.isBlank()) {
                Optional<PolicyInfo> policy = policyService.getPolicyById(userId, policyNo);
                policies = policy.map(Collections::singletonList).orElse(Collections.emptyList());
            } else {
                if ("all".equalsIgnoreCase(status)) {
                    policies = policyService.getAllUserPolicies(userId);
                } else {
                    policies = policyService.getUserPolicies(userId);
                }
            }

            if (policies.isEmpty()) {
                return ToolResult.success("未找到相关保单信息");
            }

            // 格式化结果
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(policies.size()).append(" 份保单：\n\n");
            for (int i = 0; i < policies.size(); i++) {
                PolicyInfo p = policies.get(i);
                sb.append(i + 1).append(". ").append(p.getPolicyName()).append("\n");
                sb.append("   保单号: ").append(p.getPolicyId()).append("\n");
                sb.append("   保险公司: ").append(p.getInsuranceCompany()).append("\n");
                sb.append("   保额: ").append(p.getCoverage()).append("\n");
                sb.append("   状态: ").append(p.getStatus()).append("\n\n");
            }

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("PolicyQueryTool execution failed", e);
            return ToolResult.error("查询保单失败：" + e.getMessage());
        }
    }
}
```

#### 3.5.2 体检预约相关工具

```java
package com.healthagent.agent.tool.impl;

import com.healthagent.agent.tool.*;
import com.healthagent.dto.*;
import com.healthagent.service.ExaminationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 查询医院列表工具
 */
@Slf4j
@Component
public class ListHospitalsTool implements Tool {
    private final ExaminationService examinationService;

    public ListHospitalsTool(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @Override
    public String getName() {
        return "list_hospitals";
    }

    @Override
    public String getDescription() {
        return "查询可用的体检医院列表";
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
            List<ExaminationHospitalDTO> hospitals = examinationService.getAvailableHospitals();

            StringBuilder sb = new StringBuilder();
            sb.append("可用医院列表：\n\n");
            for (ExaminationHospitalDTO hospital : hospitals) {
                sb.append("- ").append(hospital.getName());
                if (hospital.getLevel() != null) {
                    sb.append(" (").append(hospital.getLevel()).append(")");
                }
                sb.append("\n");
            }

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("ListHospitalsTool execution failed", e);
            return ToolResult.error("查询医院列表失败：" + e.getMessage());
        }
    }
}

/**
 * 查询套餐列表工具
 */
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
                sb.append("\n");
            }

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("ListPackagesTool execution failed", e);
            return ToolResult.error("查询套餐列表失败：" + e.getMessage());
        }
    }
}

/**
 * 预约体检工具
 */
@Slf4j
@Component
public class BookExaminationTool implements Tool {
    private final ExaminationService examinationService;

    public BookExaminationTool(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @Override
    public String getName() {
        return "book_examination";
    }

    @Override
    public String getDescription() {
        return "创建体检预约";
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setType("object");

        Map<String, ToolSchema.Property> properties = new HashMap<>();

        ToolSchema.Property userIdProp = new ToolSchema.Property();
        userIdProp.setType("string");
        userIdProp.setDescription("用户ID");
        properties.put("userId", userIdProp);

        ToolSchema.Property hospitalIdProp = new ToolSchema.Property();
        hospitalIdProp.setType("integer");
        hospitalIdProp.setDescription("医院ID");
        properties.put("hospitalId", hospitalIdProp);

        ToolSchema.Property packageIdProp = new ToolSchema.Property();
        packageIdProp.setType("integer");
        packageIdProp.setDescription("套餐ID");
        properties.put("packageId", packageIdProp);

        ToolSchema.Property dateProp = new ToolSchema.Property();
        dateProp.setType("string");
        dateProp.setDescription("预约日期 (YYYY-MM-DD)");
        properties.put("date", dateProp);

        ToolSchema.Property nameProp = new ToolSchema.Property();
        nameProp.setType("string");
        nameProp.setDescription("预约人姓名");
        properties.put("name", nameProp);

        ToolSchema.Property phoneProp = new ToolSchema.Property();
        phoneProp.setType("string");
        phoneProp.setDescription("联系电话");
        properties.put("phone", phoneProp);

        schema.setProperties(properties);
        schema.setRequired(List.of("userId", "hospitalId", "packageId", "date", "name", "phone"));

        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        return true;  // 预约需要确认
    }

    @Override
    public ToolResult execute(ToolInput input) {
        try {
            ExaminationBookingRequestDTO request = new ExaminationBookingRequestDTO();
            request.setUserId((String) input.getParameters().get("userId"));
            request.setHospitalId(((Number) input.getParameters().get("hospitalId")).longValue());
            request.setPackageId(((Number) input.getParameters().get("packageId")).longValue());
            request.setScheduleDate(java.time.LocalDate.parse((String) input.getParameters().get("date")));
            request.setBookerName((String) input.getParameters().get("name"));
            request.setBookerPhone((String) input.getParameters().get("phone"));

            ExaminationBookingDTO booking = examinationService.bookExamination(request);

            return ToolResult.success(String.format("""
                预约成功！
                预约号：%s
                医院：%s
                套餐：%s
                日期：%s
                """,
                booking.getBookingNo(),
                booking.getHospitalName(),
                booking.getPackageName(),
                booking.getScheduleDate()
            ));
        } catch (Exception e) {
            log.error("BookExaminationTool execution failed", e);
            return ToolResult.error("预约失败：" + e.getMessage());
        }
    }
}
```

---

## 4. 集成方案

### 4.1 修改 SmartChatController

保持接口不变，内部调用 ReActAgentOrchestrator

```java
@RestController
@RequestMapping("/api/smart-chat")
public class SmartChatController {
    @Autowired
    private ReActAgentOrchestrator orchestrator;

    @PostMapping("/send")
    public Result<SmartChatResponse> sendSmartMessage(@Valid @RequestBody SmartChatRequest request) {
        try {
            log.info("Received smart chat request: {}", request);
            SmartChatResponse response = orchestrator.execute(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("Smart chat processing failed", e);
            return Result.error("对话处理失败：" + e.getMessage());
        }
    }
}
```

### 4.2 工具注册

在启动时自动注册所有工具

```java
package com.healthagent.config;

import com.healthagent.agent.tool.Tool;
import com.healthagent.agent.tool.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ToolRegistrationRunner implements CommandLineRunner {
    private final ToolRegistry toolRegistry;
    private final List<Tool> tools;

    public ToolRegistrationRunner(ToolRegistry toolRegistry, List<Tool> tools) {
        this.toolRegistry = toolRegistry;
        this.tools = tools;
    }

    @Override
    public void run(String... args) {
        log.info("Registering {} tools...", tools.size());
        for (Tool tool : tools) {
            toolRegistry.registerTool(tool);
            log.info("Registered tool: {}", tool.getName());
        }
    }
}
```

---

## 5. 测试计划

### 5.1 单元测试

- Tool 实现的独立测试
- ReActLoop 步骤解析测试
- SessionManager 状态管理测试

### 5.2 集成测试

- 完整的保单查询流程
- 完整的体检预约流程
- 意图切换测试
- 多轮对话测试

### 5.3 端到端测试

- 完整用户对话场景测试
- 性能测试（响应时间）

---

## 6. 迁移计划

1. **第一阶段**：实现核心组件（Tool 系统、ReActLoop）
2. **第二阶段**：实现具体工具（保单查询、体检预约）
3. **第三阶段**：集成并测试
4. **第四阶段**：灰度发布，观察效果

---

## 7. 风险与应对

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| LLM 输出格式不稳定 | 高 | 中 | 增加格式校验和容错处理 |
| ReAct 循环超时 | 中 | 低 | 设置最大步数和超时时间 |
| 工具调用错误 | 高 | 中 | 完善错误处理和用户友好提示 |
| 性能下降 | 中 | 中 | 保留快速路径，监控性能指标 |

---

## 8. 后续优化方向

- 支持更多工具（健康咨询、用药提醒等）
- 增加用户反馈收集
- 优化 Prompt 设计
- 支持多语言
- 增加工具调用的用户确认机制

---

**文档结束**
