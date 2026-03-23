package com.example.agenticworkflow.service;

import com.example.agenticworkflow.model.McpToolInfo;
import com.example.agenticworkflow.model.McpToolsResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class McpToolRegistryService {

    private final ObjectProvider<ToolCallbackProvider> toolCallbackProviders;

    public McpToolRegistryService(ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        this.toolCallbackProviders = toolCallbackProviders;
    }

    public Mono<McpToolsResponse> listRegisteredMcpTools() {
        return Mono.fromSupplier(this::buildResponse);
    }

    private McpToolsResponse buildResponse() {
        List<McpToolInfo> tools = toolCallbackProviders.orderedStream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks())
                        .filter(this::isMcpToolCallback)
                        .map(callback -> toToolInfo(provider, callback)))
                .sorted(Comparator.comparing(McpToolInfo::name))
                .toList();

        return new McpToolsResponse(!tools.isEmpty(), tools.size(), tools);
    }

    private McpToolInfo toToolInfo(ToolCallbackProvider provider, ToolCallback callback) {
        return new McpToolInfo(
                callback.getToolDefinition().name(),
                callback.getToolDefinition().description(),
                callback.getClass().getName(),
                provider.getClass().getName());
    }

    private boolean isMcpToolCallback(ToolCallback callback) {
        String className = callback.getClass().getName().toLowerCase();
        return className.contains(".mcp.") || className.contains("mcp");
    }
}
