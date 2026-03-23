package com.example.agenticworkflow.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentClientConfig {

    @Bean
    ChatClient researchAgentClient(ChatClient.Builder builder, ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        ChatClient.Builder researchBuilder = builder.clone()
                .defaultSystem("""
                        You are the Research Agent.
                        Your job is to research a stock using available MCP finance tools.
                        Always use the finance tools when they are available before finalizing your answer.
                        Return only factual, current market and company data.
                        Keep the response concise and structured.
                        """);

        List<ToolCallbackProvider> providers = toolCallbackProviders.orderedStream().toList();
        if (!providers.isEmpty()) {
            researchBuilder.defaultToolCallbacks(providers.toArray(ToolCallbackProvider[]::new));
        }

        return researchBuilder.build();
    }

    @Bean
    ChatClient writerAgentClient(ChatClient.Builder builder) {
        return builder.clone()
                .defaultSystem("""
                        You are a Trading Signal Agent.
                        Use only the research notes provided by the research agent.
                        Produce a final signal as either BUY or SELL with clear reasoning.
                        If data is insufficient or contradictory, choose SELL and explain the risk.
                        """)
                .build();
    }
}
