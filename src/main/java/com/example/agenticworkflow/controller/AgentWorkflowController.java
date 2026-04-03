package com.example.agenticworkflow.controller;

import com.example.agenticworkflow.model.AgentWorkflowRequest;
import com.example.agenticworkflow.model.AgentWorkflowResponse;
import com.example.agenticworkflow.model.IndianMarketAnalysisResponse;
import com.example.agenticworkflow.model.IntradayMarketAnalysisResponse;
import com.example.agenticworkflow.model.MacroHedgeWorkflowResponse;
import com.example.agenticworkflow.model.McpToolsResponse;
import com.example.agenticworkflow.model.SingleStockFourAgentResponse;
import com.example.agenticworkflow.service.McpToolRegistryService;
import com.example.agenticworkflow.service.TwoAgentWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agents")
public class AgentWorkflowController {

    private final TwoAgentWorkflowService workflowService;
    private final McpToolRegistryService mcpToolRegistryService;

    public AgentWorkflowController(TwoAgentWorkflowService workflowService, McpToolRegistryService mcpToolRegistryService) {
        this.workflowService = workflowService;
        this.mcpToolRegistryService = mcpToolRegistryService;
    }

    @GetMapping(value = "/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AgentWorkflowResponse> runWithQueryParam(@RequestParam("task") String task) {
        return workflowService.runWorkflow(new AgentWorkflowRequest(task));
    }

    @PostMapping(value = "/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AgentWorkflowResponse> runWithBody(@Valid @RequestBody AgentWorkflowRequest request) {
        return workflowService.runWorkflow(request);
    }

    @GetMapping(value = "/india/analysis", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IndianMarketAnalysisResponse> runIndianMarketAnalysis() {
        return workflowService.runIndianMarketAnalysis();
    }

    @GetMapping(value = "/intraday/analysis", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IntradayMarketAnalysisResponse> runIntradayMarketAnalysis() {
        return workflowService.runIntradayMarketAnalysis();
    }

    @PostMapping(value = "/macro/workflow", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MacroHedgeWorkflowResponse> runMacroWorkflow(@Valid @RequestBody AgentWorkflowRequest request) {
        return workflowService.runMacroHedgeWorkflow(request);
    }

    @PostMapping(value = "/stock/workflow", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<SingleStockFourAgentResponse> runSingleStockWorkflow(@Valid @RequestBody AgentWorkflowRequest request) {
        return workflowService.runSingleStockFourAgentWorkflow(request);
    }

    @GetMapping(value = "/mcp/tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<McpToolsResponse> listRegisteredMcpTools() {
        return mcpToolRegistryService.listRegisteredMcpTools();
    }
}
