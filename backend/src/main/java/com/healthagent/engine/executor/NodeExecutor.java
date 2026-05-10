package com.healthagent.engine.executor;

import com.healthagent.dto.ExecutionEvent;
import com.healthagent.engine.model.WorkflowNode;

import java.util.Map;
import java.util.function.Consumer;

public interface NodeExecutor {
    
    Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception;
    
    default Map<String, Object> execute(WorkflowNode node, Map<String, Object> input, Consumer<ExecutionEvent> progressCallback) throws Exception {
        return execute(node, input);
    }
    
    String getSupportedNodeType();
}
