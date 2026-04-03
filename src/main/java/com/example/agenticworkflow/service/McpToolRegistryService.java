package com.example.agenticworkflow.service;

import com.example.agenticworkflow.model.McpToolInfo;
import com.example.agenticworkflow.model.McpToolsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class McpToolRegistryService {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistryService.class);

    private final ObjectProvider<ToolCallbackProvider> toolCallbackProviders;

    public McpToolRegistryService(ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        this.toolCallbackProviders = toolCallbackProviders;
    }

    public Mono<McpToolsResponse> listRegisteredMcpTools() {
        return Mono.fromCallable(this::buildResponse)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(exception -> {
                    log.warn("Failed to list MCP tools. Returning empty tool registry.", exception);
                    return Mono.just(new McpToolsResponse(false, 0, List.of()));
                });
    }

    private McpToolsResponse buildResponse() {
        List<McpToolInfo> tools = toolCallbackProviders.orderedStream()
                .flatMap(provider -> safeToolCallbacks(provider).stream()
                        .filter(this::isMcpToolCallback)
                        .map(callback -> toToolInfo(provider, callback))
                        .filter(toolInfo -> toolInfo != null))
                .sorted(Comparator.comparing(McpToolInfo::name, Comparator.nullsLast(String::compareTo)))
                .toList();

        return new McpToolsResponse(!tools.isEmpty(), tools.size(), tools);
    }

    private List<ToolCallback> safeToolCallbacks(ToolCallbackProvider provider) {
        try {
            return Arrays.stream(provider.getToolCallbacks()).toList();
        } catch (Exception exception) {
            log.warn("Skipping tool provider {} due to callback initialization failure.",
                    provider.getClass().getName(), exception);
            return List.of();
        }
    }

    private McpToolInfo toToolInfo(ToolCallbackProvider provider, ToolCallback callback) {
        try {
            return new McpToolInfo(
                    callback.getToolDefinition().name(),
                    callback.getToolDefinition().description(),
                    callback.getClass().getName(),
                    provider.getClass().getName());
        } catch (Exception exception) {
            log.warn("Skipping MCP callback {} due to tool-definition error.",
                    callback.getClass().getName(), exception);
            return null;
        }
    }

    private boolean isMcpToolCallback(ToolCallback callback) {
        String className = callback.getClass().getName().toLowerCase();
        return className.contains(".mcp.") || className.contains("mcp");
    }
}
