package com.example.agenticworkflow.model;

public record AgentWorkflowResponse(
        String task,
        String researchAgentOutput,
        String writerAgentOutput) {
}
