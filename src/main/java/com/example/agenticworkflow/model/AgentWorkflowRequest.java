package com.example.agenticworkflow.model;

import jakarta.validation.constraints.NotBlank;

public record AgentWorkflowRequest(@NotBlank(message = "task must not be blank") String task) {
}
