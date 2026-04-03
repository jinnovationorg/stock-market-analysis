package com.example.agenticworkflow.model;

public record IntradayMarketAnalysisResponse(
        String marketScope,
        String researchAgentOutput,
        String writerAgentOutput) {
}
