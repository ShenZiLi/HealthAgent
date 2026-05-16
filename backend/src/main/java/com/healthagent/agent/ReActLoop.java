package com.healthagent.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.healthagent.agent.tool.ToolExecutor;
import com.healthagent.agent.tool.ToolRegistry;
import com.healthagent.agent.tool.ToolResult;
import com.healthagent.service.chat.AbstractChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        %s
        
        工作流程：
        1. 首先分析用户问题，思考需要做什么（Thought）
        2. 如果需要查询信息，选择合适的工具并调用（Action + Action Input）
        3. 观察工具返回的结果（Observation）
        4. 重复上述步骤直到获得足够信息
        5. 最后给出最终答案（Finish）
        
        输出格式要求（必须严格遵守）：
        
        如果需要调用工具：
        Thought: <你的思考过程>
        Action: <工具名称>
        Action Input: <JSON格式的参数>
        
        如果可以直接回答：
        Thought: <总结思考>
        Finish: <最终答案>
        
        注意：
        - 只输出要求的格式，不要有额外解释
        - Action Input 必须是 valid JSON
        - 工具调用只调用一次，不要嵌套或循环调用
        """;

    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.*?)(?=\\s*(Action|Finish|$))", Pattern.DOTALL);
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*(\\w+)");
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(\\{.*?\\})", Pattern.DOTALL);
    private static final Pattern FINISH_PATTERN = Pattern.compile("Finish:\\s*(.*)", Pattern.DOTALL);

    public ReActLoop(ToolRegistry toolRegistry, ToolExecutor toolExecutor, AbstractChatClient chatClient) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.chatClient = chatClient;
    }

    public ReActResult run(ConversationState state, UserInput input) {
        ReActResult result = new ReActResult();
        List<ReActStep> steps = new ArrayList<>();

        for (int step = 0; step < MAX_STEPS; step++) {
            log.info("ReAct step {}/{}", step + 1, MAX_STEPS);

            Thought thought = generateThought(state, input, steps);
            steps.add(new ReActStep(StepType.THOUGHT, thought.getContent()));

            if (thought.isFinish()) {
                result.setFinalAnswer(thought.getFinishAnswer());
                result.setSteps(steps);
                log.info("ReAct finished with answer: {}", thought.getFinishAnswer());
                return result;
            }

            Action action = parseAction(thought);
            if (action.getToolName() == null) {
                log.warn("Could not parse action from thought, falling back to simple response");
                result.setFinalAnswer("让我想想..." + thought.getContent());
                result.setSteps(steps);
                return result;
            }

            steps.add(new ReActStep(StepType.ACTION, action.toString()));

            Observation observation = executeAction(action);
            steps.add(new ReActStep(StepType.OBSERVATION, observation.getContent()));

            state.addSteps(steps.subList(Math.max(0, steps.size() - 3), steps.size()));
        }

        result.setFinalAnswer("抱歉，我需要更多信息来帮助您。让我们换一种方式试试？");
        result.setSteps(steps);
        return result;
    }

    private Thought generateThought(ConversationState state, UserInput input, List<ReActStep> previousSteps) {
        String prompt = buildPrompt(state, input, previousSteps);
        String toolsDesc = buildToolsDescription();
        String systemPrompt = String.format(REACT_SYSTEM_PROMPT, toolsDesc);

        log.debug("Calling LLM with prompt: {}", prompt);
        String response = chatClient.chat(prompt, systemPrompt, input.getUserId());
        log.debug("LLM response: {}", response);

        return parseThought(response);
    }

    private String buildPrompt(ConversationState state, UserInput input, List<ReActStep> previousSteps) {
        StringBuilder sb = new StringBuilder();

        for (ChatMessage msg : state.getHistory()) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        sb.append("User: ").append(input.getContent()).append("\n");

        for (ReActStep step : previousSteps) {
            sb.append(step.getType()).append(": ").append(step.getContent()).append("\n");
        }

        return sb.toString();
    }

    private String buildToolsDescription() {
        StringBuilder sb = new StringBuilder();
        toolRegistry.getToolDefinitions().forEach(tool -> {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        });
        return sb.toString();
    }

    private Thought parseThought(String response) {
        Thought thought = new Thought();
        thought.setContent(response);

        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(response);
        if (thoughtMatcher.find()) {
            thought.setContent(thoughtMatcher.group(1).trim());
        }

        Matcher finishMatcher = FINISH_PATTERN.matcher(response);
        if (finishMatcher.find()) {
            thought.setFinish(true);
            thought.setFinishAnswer(finishMatcher.group(1).trim());
            return thought;
        }

        Matcher actionMatcher = ACTION_PATTERN.matcher(response);
        if (actionMatcher.find()) {
            thought.setActionName(actionMatcher.group(1).trim());
        }

        Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(response);
        if (actionInputMatcher.find()) {
            try {
                Map<String, Object> params = JSON.parseObject(actionInputMatcher.group(1).trim(),
                    new TypeReference<Map<String, Object>>() {});
                thought.setActionParameters(params);
            } catch (Exception e) {
                log.warn("Failed to parse action input as JSON", e);
            }
        }

        return thought;
    }

    private Action parseAction(Thought thought) {
        Action action = new Action();
        action.setToolName(thought.getActionName());
        action.setParameters(thought.getActionParameters() != null ?
            thought.getActionParameters() : Collections.emptyMap());
        return action;
    }

    private Observation executeAction(Action action) {
        log.info("Executing action: {} with params: {}", action.getToolName(), action.getParameters());
        ToolResult result = toolExecutor.execute(action.getToolName(), action.getParameters());

        String content;
        if (result.isSuccess()) {
            content = result.getOutput();
        } else {
            content = "Error: " + result.getError();
        }

        return new Observation(content);
    }
}
