package com.healthagent.engine.executor.impl;

import org.springframework.stereotype.Component;

/**
 * 通用 LLM 节点执行器
 */
@Component
public class LlmNodeExecutor extends AbstractLLMNodeExecutor {

    @Override
    protected String getNodeType() {
        return "llm";
    }
}
