package com.example.agenticworkflow.model;

public record SingleStockFourAgentResponse(
        String stockInput,
        String macroScannerOutput,
        String sectorMapperOutput,
        String stockScreenerOutput,
        String riskEvaluatorOutput) {
}
