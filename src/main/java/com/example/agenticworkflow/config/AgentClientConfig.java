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

    @Bean
    ChatClient macroScannerClient(ChatClient.Builder builder, ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        ChatClient.Builder macroBuilder = builder.clone()
                .defaultSystem("""
                        You are Agent 1: Macro Scanner.
                        Identify geopolitical and macro regime shifts with a strong India-first lens.
                        Focus on wars, sanctions, elections, supply chains, energy shocks, rates, inflation, currencies, and policy divergence.
                        Prioritize concrete observations and concise structure.
                        Use MCP finance tools when relevant and available.
                        """);

        List<ToolCallbackProvider> providers = toolCallbackProviders.orderedStream().toList();
        if (!providers.isEmpty()) {
            macroBuilder.defaultToolCallbacks(providers.toArray(ToolCallbackProvider[]::new));
        }

        return macroBuilder.build();
    }

    @Bean
    ChatClient sectorMapperClient(ChatClient.Builder builder) {
        return builder.clone()
                .defaultSystem("""
                        You are Agent 2: Sector Mapper.
                        Convert macro/geopolitical trends into sector winners and losers.
                        Emphasize Indian sectors and India's global supply-chain position (China+1, PLI, infra, defense, electronics, manufacturing).
                        Map clear cause-effect links and avoid generic statements.
                        """)
                .build();
    }

    @Bean
    ChatClient stockScreenerClient(ChatClient.Builder builder, ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        ChatClient.Builder screenerBuilder = builder.clone()
                .defaultSystem("""
                        You are Agent 3: Stock Screener.
                        Build a high-conviction stock shortlist from sector maps.
                        Prefer companies with strong revenue quality, margins, manageable leverage, leadership position, and strategic moat.
                        Focus mostly on NSE/BSE names while allowing a few global names if thesis-critical.
                        Use MCP finance tools when relevant and available.
                        """);

        List<ToolCallbackProvider> providers = toolCallbackProviders.orderedStream().toList();
        if (!providers.isEmpty()) {
            screenerBuilder.defaultToolCallbacks(providers.toArray(ToolCallbackProvider[]::new));
        }

        return screenerBuilder.build();
    }

    @Bean
    ChatClient riskEvaluatorClient(ChatClient.Builder builder) {
        return builder.clone()
                .defaultSystem("""
                        You are Agent 4: Risk Evaluator.
                        Stress-test shortlisted stocks against adverse macro/geopolitical scenarios.
                        Surface invalidation triggers, downside pathways, and confidence level.
                        Finalize only risk-adjusted picks with explicit short-term vs long-term framing.
                        """)
                .build();
    }
}
