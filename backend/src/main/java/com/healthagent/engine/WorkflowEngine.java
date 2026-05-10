package com.healthagent.engine;

import com.alibaba.fastjson2.JSON;
import com.healthagent.dto.ExecutionEvent;
import com.healthagent.dto.ExecutionResponse;
import com.healthagent.engine.dag.DAGParser;
import com.healthagent.engine.executor.NodeExecutor;
import com.healthagent.engine.executor.NodeExecutorFactory;
import com.healthagent.engine.model.WorkflowConfig;
import com.healthagent.engine.model.WorkflowNode;
import com.healthagent.entity.ExecutionRecord;
import com.healthagent.entity.Workflow;
import com.healthagent.mapper.ExecutionRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class WorkflowEngine implements WorkflowExecutor {
    
    @Autowired
    private DAGParser dagParser;
    
    @Autowired
    private NodeExecutorFactory executorFactory;
    
    @Autowired
    private ExecutionRecordMapper executionRecordMapper;
    
    @Override
    public ExecutionResponse execute(Workflow workflow, String inputData) {
        return executeWithCallback(workflow, inputData, null);
    }
    
    @Override
    public ExecutionResponse executeWithCallback(Workflow workflow, String inputData, Consumer<ExecutionEvent> eventCallback) {
        long startTime = System.currentTimeMillis();
        
        WorkflowConfig config = JSON.parseObject(workflow.getFlowData(), WorkflowConfig.class);
        List<WorkflowNode> sortedNodes = dagParser.parse(config);
        
        List<ExecutionResponse.NodeResult> nodeResults = new ArrayList<>();
        Map<String, Map<String, Object>> nodeOutputs = new HashMap<>();
        
        Map<String, Object> currentInput = new HashMap<>();
        currentInput.put("input", inputData);
        
        String status = "SUCCESS";
        String errorMessage = null;
        String outputData = null;
        
        ExecutionRecord record = new ExecutionRecord();
        
        try {
            if (eventCallback != null) {
                eventCallback.accept(ExecutionEvent.workflowStart(null));
            }
            
            for (WorkflowNode node : sortedNodes) {
                long nodeStartTime = System.currentTimeMillis();
                
                if (eventCallback != null) {
                    eventCallback.accept(ExecutionEvent.nodeStart(node.getId(), node.getType()));
                }
                
                ExecutionResponse.NodeResult nodeResult = new ExecutionResponse.NodeResult();
                nodeResult.setNodeId(node.getId());
                nodeResult.setNodeName(node.getType());
                nodeResult.setInput(JSON.toJSONString(currentInput));
                
                try {
                    NodeExecutor executor = executorFactory.getExecutor(node.getType());
                    Map<String, Object> output = executor.execute(node, currentInput, eventCallback);
                    
                    nodeOutputs.put(node.getId(), output);
                    
                    nodeResult.setStatus("SUCCESS");
                    nodeResult.setOutput(JSON.toJSONString(output));
                    
                    long nodeEndTime = System.currentTimeMillis();
                    int nodeDuration = (int) (nodeEndTime - nodeStartTime);
                    nodeResult.setDuration(nodeDuration);
                    
                    if (eventCallback != null) {
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("input", currentInput);
                        eventData.put("output", output);
                        eventData.put("duration", nodeDuration);
                        eventCallback.accept(ExecutionEvent.nodeSuccess(node.getId(), node.getType(), eventData, nodeDuration));
                    }
                    
                    currentInput = output;
                    
                } catch (Exception e) {
                    log.error("节点执行失败: {}", node.getId(), e);
                    nodeResult.setStatus("FAILED");
                    nodeResult.setError(e.getMessage());
                    status = "FAILED";
                    errorMessage = "节点 " + node.getId() + " 执行失败: " + e.getMessage();
                    
                    if (eventCallback != null) {
                        eventCallback.accept(ExecutionEvent.nodeError(node.getId(), node.getType(), e.getMessage()));
                    }
                    
                    throw e;
                } finally {
                    long nodeEndTime = System.currentTimeMillis();
                    nodeResult.setDuration((int) (nodeEndTime - nodeStartTime));
                    nodeResults.add(nodeResult);
                }
            }
            
            outputData = JSON.toJSONString(currentInput);
            
        } catch (Exception e) {
            status = "FAILED";
            if (errorMessage == null) {
                errorMessage = e.getMessage();
            }
        }
        
        long endTime = System.currentTimeMillis();
        int duration = (int) (endTime - startTime);
        
        if (eventCallback != null) {
            eventCallback.accept(ExecutionEvent.workflowComplete(status, currentInput, duration));
        }
        
        record.setFlowId(workflow.getId());
        Map<String, Object> inputDataMap = new HashMap<>();
        inputDataMap.put("input", inputData);
        String inputDataJson = JSON.toJSONString(inputDataMap);
        log.info("保存执行记录 - inputData: {}", inputDataJson);
        log.info("保存执行记录 - outputData: {}", outputData);
        record.setInputData(inputDataJson);
        record.setOutputData(outputData);
        record.setStatus(status);
        record.setNodeResults(JSON.toJSONString(nodeResults));
        record.setErrorMessage(errorMessage);
        record.setDuration(duration);
        executionRecordMapper.insert(record);
        
        ExecutionResponse response = new ExecutionResponse();
        response.setExecutionId(record.getId());
        response.setStatus(status);
        response.setInputData(inputDataJson);
        response.setNodeResults(nodeResults);
        response.setOutputData(outputData);
        response.setDuration(duration);
        
        return response;
    }
    
    @Override
    public String getEngineType() {
        return "dag";
    }
}
