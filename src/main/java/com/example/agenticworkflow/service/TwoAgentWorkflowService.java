package com.example.agenticworkflow.service;

import com.example.agenticworkflow.model.AgentWorkflowRequest;
import com.example.agenticworkflow.model.AgentWorkflowResponse;
import com.example.agenticworkflow.model.IndianMarketAnalysisResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class TwoAgentWorkflowService {

    private final ChatClient researchAgentClient;
    private final ChatClient writerAgentClient;

    public TwoAgentWorkflowService(
            @Qualifier("researchAgentClient") ChatClient researchAgentClient,
            @Qualifier("writerAgentClient") ChatClient writerAgentClient) {
        this.researchAgentClient = researchAgentClient;
        this.writerAgentClient = writerAgentClient;
    }

    public Mono<AgentWorkflowResponse> runWorkflow(AgentWorkflowRequest request) {
        return Mono.fromCallable(() -> buildWorkflowResponse(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<IndianMarketAnalysisResponse> runIndianMarketAnalysis() {
        return Mono.fromCallable(this::buildIndianMarketAnalysisResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private AgentWorkflowResponse buildWorkflowResponse(AgentWorkflowRequest request) {
        String researchOutput = researchAgentClient.prompt()
                .user("""
                        Perform stock research for this request:

                        %s

                        Requirements:
                        1. Use MCP finance tools to fetch current stock and company data.        
                        2. Focus on valuation, momentum, recent price context, and major risks.
                        3. Include macro context that can impact this stock: interest rates, inflation, currency, and crude/commodity trends if relevant.
                        4. Include geopolitical context such as war/conflict, sanctions, trade disruptions, and supply-chain impact if relevant.
                        5. Include a short "Tool Evidence" section listing key fetched values.
                        6. If a required value cannot be fetched, explicitly mark it as unavailable.
                        7. List all tools used.
                        """.formatted(request.task()))
                .call()
                .content();

        String writerOutput = writerAgentClient.prompt()
                .user("""
                        Stock request:
                        %s

                        Research notes:
                        %s

                        Produce the final output in this exact format:
                        SIGNAL: <BUY|SELL>
                        CONFIDENCE: <0-100>
                        SUMMARY: <2-4 sentences>
                        KEY DRIVERS:
                        - <driver 1>
                        - <driver 2>
                        - <driver 3>
                        RISKS:
                        - <risk 1>
                        - <risk 2>
                        MACRO & GEOPOLITICAL FACTORS:
                        - <factor 1>
                        - <factor 2>
                        """.formatted(request.task(), researchOutput))
                .call()
                .content();

        return new AgentWorkflowResponse(request.task(), researchOutput, writerOutput);
    }

    private IndianMarketAnalysisResponse buildIndianMarketAnalysisResponse() {
        String marketScope = "Indian stock market (NSE/BSE)";

        String researchOutput = researchAgentClient.prompt()
                .user("""
                        Perform an overall analysis of the Indian stock market (NSE/BSE).

                        Requirements:
                        1. Use MCP finance tools to fetch current index, price, valuation, and company context where possible.
                        2. Cover macro trend, sector leadership, market breadth, and key near-term risks.
                        3. Explicitly include macroeconomic context: RBI policy stance, inflation trend, bond yields, USD/INR, and crude oil direction.
                        4. Explicitly include current geopolitical context: wars/conflicts, sanctions, shipping and energy disruptions, and likely impact on Indian sectors.
                        5. Propose candidate Indian stocks with evidence.
                        6. Include a short "Tool Evidence" section with the fetched values.
                        7. If any required data is unavailable, explicitly mention the gap.
                        """)
                .call()
                .content();

        String writerOutput = writerAgentClient.prompt()
                .user("""
                        Scope:
                        %s

                        Research notes:
                        %s

                        Produce a recommendation report in this exact format:
                        MARKET VIEW: <BULLISH|NEUTRAL|BEARISH>
                        CONFIDENCE: <0-100>
                        SUMMARY: <3-5 sentences>
                        TOP 5 STOCKS TO BUY:
                        1. <stock name + ticker> - <short reason>
                        2. <stock name + ticker> - <short reason>
                        3. <stock name + ticker> - <short reason>
                        4. <stock name + ticker> - <short reason>
                        5. <stock name + ticker> - <short reason>
                        RISKS:
                        - <risk 1>
                        - <risk 2>
                        MACRO & GEOPOLITICAL WATCHLIST:
                        - <factor 1>
                        - <factor 2>
                        - <factor 3>
                        DISCLAIMER: <not financial advice>
                        """.formatted(marketScope, researchOutput))
                .call()
                .content();

        return new IndianMarketAnalysisResponse(marketScope, researchOutput, writerOutput);
    }
}
