package com.example.agenticworkflow.model;

import java.util.List;

public record McpToolsResponse(
        boolean enabled,
        int count,
        List<McpToolInfo> tools) {
}
