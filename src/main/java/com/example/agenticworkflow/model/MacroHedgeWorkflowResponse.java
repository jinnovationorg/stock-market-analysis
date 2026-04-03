package com.example.agenticworkflow.model;

public record MacroHedgeWorkflowResponse(
        String inputPrompt,
        String macroScannerOutput,
        String sectorMapperOutput,
        String stockScreenerOutput,
        String riskEvaluatorOutput) {
}
