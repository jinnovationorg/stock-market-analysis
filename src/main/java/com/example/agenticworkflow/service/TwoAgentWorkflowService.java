package com.example.agenticworkflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.agenticworkflow.model.AgentWorkflowRequest;
import com.example.agenticworkflow.model.AgentWorkflowResponse;
import com.example.agenticworkflow.model.IndianMarketAnalysisResponse;
import com.example.agenticworkflow.model.IntradayMarketAnalysisResponse;
import com.example.agenticworkflow.model.MacroHedgeWorkflowResponse;
import com.example.agenticworkflow.model.SingleStockFourAgentResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TwoAgentWorkflowService {

    private static final String DEFAULT_MACRO_WORKFLOW_PROMPT = """
            Act as a global macro hedge fund analyst.

            Analyze current geopolitical, macroeconomic, and sectoral trends to identify high-potential stocks.

            Consider the following factors:
            1. Geopolitical events:
               - Wars, trade tensions, elections, sanctions
               - Supply chain disruptions
               - Energy and commodity dependencies

            2. Macroeconomic indicators:
               - Interest rates (US Fed, RBI, ECB)
               - Inflation trends
               - Currency movements
               - GDP growth outlook

            3. Sectoral impact:
               - Which sectors benefit or suffer from these conditions?
               - Emerging themes (AI, defense, energy transition, semiconductors, etc.)

            4. Regional analysis:
               - Which countries/regions are gaining or losing advantage?

            5. Company-level filtering:
               - Strong fundamentals (revenue growth, margins, low debt)
               - Market leadership or strategic advantage
               - Exposure to benefiting sectors

            Output:
            - Top 5–10 stocks (preferably Indian + global)
            - For each stock include:
              - Reason linked to geopolitical/macroeconomic trend
              - Risk factors
              - Short-term vs long-term outlook

            Also include:
            - 2 sectors to avoid and why
            - Key risks that could invalidate this thesis

            Focus more on Indian markets (NSE/BSE) and India's position in global supply chains, China+1 strategy, and government policies (PLI, infra, defense).
            """;

    private static final List<StockUniverseItem> NIFTY_20_UNIVERSE = List.of(
            new StockUniverseItem("Reliance Industries", "RELIANCE.NS"),
            new StockUniverseItem("TCS", "TCS.NS"),
            new StockUniverseItem("HDFC Bank", "HDFCBANK.NS"),
            new StockUniverseItem("ICICI Bank", "ICICIBANK.NS"),
            new StockUniverseItem("Infosys", "INFY.NS"),
            new StockUniverseItem("Bharti Airtel", "BHARTIARTL.NS"),
            new StockUniverseItem("State Bank of India", "SBIN.NS"),
            new StockUniverseItem("Larsen & Toubro", "LT.NS"),
            new StockUniverseItem("ITC", "ITC.NS"),
            new StockUniverseItem("Hindustan Unilever", "HINDUNILVR.NS"),
            new StockUniverseItem("Kotak Mahindra Bank", "KOTAKBANK.NS"),
            new StockUniverseItem("Axis Bank", "AXISBANK.NS"),
            new StockUniverseItem("Bajaj Finance", "BAJFINANCE.NS"),
            new StockUniverseItem("HCL Technologies", "HCLTECH.NS"),
            new StockUniverseItem("Sun Pharmaceutical", "SUNPHARMA.NS"),
            new StockUniverseItem("Maruti Suzuki", "MARUTI.NS"),
            new StockUniverseItem("Mahindra & Mahindra", "M&M.NS"),
            new StockUniverseItem("UltraTech Cement", "ULTRACEMCO.NS"),
            new StockUniverseItem("NTPC", "NTPC.NS"),
            new StockUniverseItem("Power Grid", "POWERGRID.NS")
    );

    private final ChatClient researchAgentClient;
    private final ChatClient writerAgentClient;
    private final ChatClient macroScannerClient;
    private final ChatClient sectorMapperClient;
    private final ChatClient stockScreenerClient;
    private final ChatClient riskEvaluatorClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TwoAgentWorkflowService(
            @Qualifier("researchAgentClient") ChatClient researchAgentClient,
            @Qualifier("writerAgentClient") ChatClient writerAgentClient,
            @Qualifier("macroScannerClient") ChatClient macroScannerClient,
            @Qualifier("sectorMapperClient") ChatClient sectorMapperClient,
            @Qualifier("stockScreenerClient") ChatClient stockScreenerClient,
            @Qualifier("riskEvaluatorClient") ChatClient riskEvaluatorClient) {
        this.researchAgentClient = researchAgentClient;
        this.writerAgentClient = writerAgentClient;
        this.macroScannerClient = macroScannerClient;
        this.sectorMapperClient = sectorMapperClient;
        this.stockScreenerClient = stockScreenerClient;
        this.riskEvaluatorClient = riskEvaluatorClient;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Mono<AgentWorkflowResponse> runWorkflow(AgentWorkflowRequest request) {
        return Mono.fromCallable(() -> buildWorkflowResponse(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<IndianMarketAnalysisResponse> runIndianMarketAnalysis() {
        return Mono.fromCallable(this::buildIndianMarketAnalysisResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<IntradayMarketAnalysisResponse> runIntradayMarketAnalysis() {
        return Mono.fromCallable(this::buildIntradayMarketAnalysisResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<MacroHedgeWorkflowResponse> runMacroHedgeWorkflow(AgentWorkflowRequest request) {
        return Mono.fromCallable(() -> buildMacroHedgeWorkflowResponse(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<SingleStockFourAgentResponse> runSingleStockFourAgentWorkflow(AgentWorkflowRequest request) {
        return Mono.fromCallable(() -> buildSingleStockFourAgentResponse(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private AgentWorkflowResponse buildWorkflowResponse(AgentWorkflowRequest request) {
        String researchOutput = researchAgentClient.prompt()
                .user("""
                        Perform stock research for this request:

                        %s

                        Requirements:
                        1. Use MCP finance tools to fetch current stock and company data.
                        2. Focus on valuation, current momentum, recent price action, and major risks.
                        3. Enrich the context with investor-interest signals such as trading activity, market attention, analyst stance, and whether sentiment appears to be improving or weakening.
                        4. Explain current momentum clearly: trend direction, strength, recent catalysts, and whether the move looks sustained, overheated, or weakening.
                        5. Include macro context that can impact this stock: interest rates, inflation, currency, and crude/commodity trends if relevant.
                        6. Include geopolitical context such as war/conflict, sanctions, trade disruptions, and supply-chain impact if relevant.
                        7. Include a short "Investor View" section summarizing why investors may currently be interested or cautious.
                        8. Include a short "Tool Evidence" section listing key fetched values.
                        9. If a required value cannot be fetched, explicitly mark it as unavailable.
                        10. List all tools used.
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
        String marketScope = "Indian stock market (NSE/BSE) using a fixed Nifty 20 universe";
        String nifty20Universe = buildNifty20UniverseContext();

        String researchOutput = researchAgentClient.prompt()
                .user("""
                        Perform an overall analysis of the Indian stock market (NSE/BSE).

                        Analyze only this hardcoded Nifty 20 universe:
                        %s

                        Requirements:
                        1. Analyze only the 20 hardcoded stocks listed above. Do not introduce stocks outside this universe.
                        2. Use MCP finance tools to fetch current index, price, valuation, sector, and company context where possible.
                        3. Focus on market valuation, current momentum, recent price action, sector leadership, market breadth, and major near-term risks.
                        4. Enrich the context with investor-interest signals such as participation, market attention, institutional or retail interest where inferable, analyst stance, and whether sentiment appears to be improving or weakening.
                        5. Explain current momentum clearly: trend direction, strength, recent catalysts, and whether the move looks sustained, overheated, or weakening for the overall market and leading sectors.
                        6. Explicitly include macroeconomic context: RBI policy stance, inflation trend, bond yields, USD/INR, liquidity conditions, and crude oil direction.
                        7. Explicitly include current geopolitical context: wars/conflicts, sanctions, shipping and energy disruptions, and likely impact on Indian sectors.
                        8. Include a short "Investor View" section summarizing why investors may currently be interested in or cautious on Indian equities.
                        9. Rank the 20 hardcoded stocks by current market setup quality before selecting recommendations.
                        10. Propose candidate Indian stocks with evidence and explain why they fit the current market setup.
                        11. Include a short "Tool Evidence" section with the fetched values.
                        12. If any required data is unavailable, explicitly mention the gap.
                        13. List all tools used.
                        """.formatted(nifty20Universe))
                .call()
                .content();

        String writerOutput = writerAgentClient.prompt()
                .user("""
                        Scope:
                        %s

                        Hardcoded Nifty 20 universe:
                        %s

                        Research notes:
                        %s

                        Rules:
                        - Evaluate only the 20 hardcoded stocks.
                        - Choose final recommendations only from this list.
                        - If a stock lacks reliable support in the research notes, avoid recommending it.

                        Produce a recommendation report in this exact format:
                        MARKET VIEW: <BULLISH|NEUTRAL|BEARISH>
                        CONFIDENCE: <0-100>
                        SUMMARY: <3-5 sentences>
                        TOP NIFTY 20 STOCKS TO BUY:
                        1. <stock name + ticker> - <short reason>
                        2. <stock name + ticker> - <short reason>
                        3. <stock name + ticker> - <short reason>
                        4. <stock name + ticker> - <short reason>
                        5. <stock name + ticker> - <short reason>
                        6. <stock name + ticker> - <short reason>
                        7. <stock name + ticker> - <short reason>
                        8. <stock name + ticker> - <short reason>
                        9. <stock name + ticker> - <short reason>
                        10. <stock name + ticker> - <short reason>
                        11. <stock name + ticker> - <short reason>
                        12. <stock name + ticker> - <short reason>
                        13. <stock name + ticker> - <short reason>
                        14. <stock name + ticker> - <short reason>
                        15. <stock name + ticker> - <short reason>
                        16. <stock name + ticker> - <short reason>
                        17. <stock name + ticker> - <short reason>
                        18. <stock name + ticker> - <short reason>
                        19. <stock name + ticker> - <short reason>
                        20. <stock name + ticker> - <short reason>
                        RISKS:
                        - <risk 1>
                        - <risk 2>
                        MACRO & GEOPOLITICAL WATCHLIST:
                        - <factor 1>
                        - <factor 2>
                        - <factor 3>
                        DISCLAIMER: <not financial advice>
                        """.formatted(marketScope, nifty20Universe, researchOutput))
                .call()
                .content();

        return new IndianMarketAnalysisResponse(marketScope, researchOutput, writerOutput);
    }

    private IntradayMarketAnalysisResponse buildIntradayMarketAnalysisResponse() {
        String marketScope = "Indian stock market intraday setup (NSE/BSE) for the hardcoded Nifty 20 stocks";
        String intradayUniverse = buildNifty20UniverseContext();
        String intradaySnapshotContext = buildIntradaySnapshotContext();

        String researchOutput = researchAgentClient.prompt()
                .user("""
                        Perform an intraday analysis of the Indian stock market (NSE/BSE).

                        Analyze only this hardcoded Nifty 20 universe:
                        %s

                        Server-fetched intraday snapshot data:
                        %s

                        Requirements:
                        1. Analyze only the 20 hardcoded Nifty stocks listed above. Do not introduce stocks outside this universe.
                        2. Treat the server-fetched snapshot data above as the source of truth for opening price, current price, absolute delta, and percentage delta. Do not invent or override these numbers.
                        3. Use MCP finance tools only for supporting context such as company context, momentum confirmation, recent news, and analyst stance.
                        4. For each stock, analyze the move from morning/open price to the latest/current price.
                        5. Calculate and report the intraday delta for each candidate as:
                           - opening price
                           - current/latest price
                           - absolute delta
                           - percentage delta
                        6. Use delta as a core signal criterion:
                           - strong positive delta with healthy momentum supports BUY bias
                           - strong negative delta or fading momentum supports SELL bias
                           - flat, noisy, or directionless delta supports AVOID or low-confidence bias
                        7. Focus on intraday momentum, relative strength and weakness, liquidity, volatility, and likely near-term tradeable moves across these 20 names.
                        8. Rank all 20 names from strongest to weakest intraday setup before selecting the final top 5 ideas.
                        9. Explicitly mention if any stock has unreliable, stale, or unavailable pricing in the snapshot data.
                        10. Enrich the context with investor-interest signals such as trading activity, unusual attention, participation, analyst stance where relevant, and whether sentiment appears to be strengthening or fading during the session.
                        11. Explain current momentum clearly: trend direction, strength, recent catalysts, and whether the move looks sustained, stretched, or vulnerable to reversal.
                        12. Explicitly include same-day macro context that can affect intraday trades: RBI cues, inflation or rates commentary, USD/INR, crude oil direction, global index sentiment, and any major scheduled events if relevant.
                        13. Explicitly include current geopolitical context such as wars/conflicts, sanctions, shipping or energy disruptions, and likely same-day impact on Indian sectors.
                        14. Include a short "Investor View" section summarizing why traders and short-term investors may currently be interested in or cautious on Indian equities.
                        15. Propose candidate intraday Indian stocks with evidence and explain why they fit the current session setup.
                        16. Include a short "Tool Evidence" section with the fetched values.
                        17. List all tools used.
                        """.formatted(intradayUniverse, intradaySnapshotContext))
                .call()
                .content();

        String writerOutput = writerAgentClient.prompt()
                .user("""
                        Scope:
                        %s

                        Hardcoded Nifty 20 universe:
                        %s

                        Server-fetched intraday snapshot data:
                        %s

                        Research notes:
                        %s

                        Rules:
                        - Evaluate only the 20 hardcoded stocks.
                        - Use the provided opening, current, delta, and percentage delta values exactly as given when writing the final ideas.
                        - Prefer stocks with the cleanest intraday momentum and most reliable prices.
                        - If a stock has unreliable or unavailable prices, mark it as AVOID instead of guessing.
                        - Choose the final 5 ideas only from these 20 stocks.

                        Produce an intraday recommendation report in this exact format:
                        INTRADAY VIEW: <BULLISH|NEUTRAL|BEARISH>
                        CONFIDENCE: <0-100>
                        SUMMARY: <3-5 sentences>
                        SIGNAL CRITERIA:
                        - Compare morning/open price vs current price
                        - Use delta and delta percentage as the primary signal driver
                        - Mention when the move looks strong, weakening, or noisy
                        TOP 5 INTRADAY IDEAS:
                        1. <stock name + ticker> - <BUY|SELL|AVOID> - Open: <price> - Current: <price> - Delta: <absolute and %%> - <short setup reason>
                        2. <stock name + ticker> - <BUY|SELL|AVOID> - Open: <price> - Current: <price> - Delta: <absolute and %%> - <short setup reason>
                        3. <stock name + ticker> - <BUY|SELL|AVOID> - Open: <price> - Current: <price> - Delta: <absolute and %%> - <short setup reason>
                        4. <stock name + ticker> - <BUY|SELL|AVOID> - Open: <price> - Current: <price> - Delta: <absolute and %%> - <short setup reason>
                        5. <stock name + ticker> - <BUY|SELL|AVOID> - Open: <price> - Current: <price> - Delta: <absolute and %%> - <short setup reason>
                        INTRADAY CATALYSTS:
                        - <catalyst 1>
                        - <catalyst 2>
                        RISKS:
                        - <risk 1>
                        - <risk 2>
                        OPENING WATCHLIST:
                        - <factor 1>
                        - <factor 2>
                        - <factor 3>
                        DISCLAIMER: <not financial advice>
                        """.formatted(marketScope, intradayUniverse, intradaySnapshotContext, researchOutput))
                .call()
                .content();

        return new IntradayMarketAnalysisResponse(marketScope, researchOutput, writerOutput);
    }

    private MacroHedgeWorkflowResponse buildMacroHedgeWorkflowResponse(AgentWorkflowRequest request) {
        String userFocus = request.task() == null || request.task().isBlank()
                ? "No extra user focus provided."
                : request.task().trim();

        String macroScannerOutput = macroScannerClient.prompt()
                .user("""
                        Base brief:
                        %s

                        Additional user focus:
                        %s

                        Output format:
                        MACRO THEMES:
                        - <theme>
                        GEOPOLITICAL DRIVERS:
                        - <driver>
                        RATE/INFLATION/CURRENCY REGIME:
                        - <observation>
                        INDIA POSITIONING:
                        - <insight>
                        REGIONAL WINNERS & LOSERS:
                        - <winner/loser + reason>
                        TOOL EVIDENCE:
                        - <fetched value or unavailable>
                        TOOLS USED:
                        - <tool names>
                        """.formatted(DEFAULT_MACRO_WORKFLOW_PROMPT, userFocus))
                .call()
                .content();

        String sectorMapperOutput = sectorMapperClient.prompt()
                .user("""
                        Macro Scanner notes:
                        %s

                        Map sectors from these notes.

                        Output format:
                        WINNING SECTORS:
                        - <sector + why now>
                        LOSING SECTORS:
                        - <sector + why under pressure>
                        INDIA SUPPLY-CHAIN ADVANTAGE MAP:
                        - <China+1/PLI/infra/defense linkage>
                        THEMES TO TRACK NEXT 1-2 QUARTERS:
                        - <theme>
                        """.formatted(macroScannerOutput))
                .call()
                .content();

        String stockScreenerOutput = stockScreenerClient.prompt()
                .user("""
                        Macro Scanner notes:
                        %s

                        Sector Mapper notes:
                        %s

                        Build a stock shortlist with India-first bias.
                        Include mostly NSE/BSE names and only add global names when thesis-critical.

                        Output format:
                        SCREENED STOCKS (10-14):
                        1. <stock + ticker + region> - <thesis>
                        2. <...>
                        FUNDAMENTAL CHECKPOINTS:
                        - <revenue/margin/debt/moat checks>
                        CONCENTRATION RISKS:
                        - <risk>
                        TOOL EVIDENCE:
                        - <fetched value or unavailable>
                        TOOLS USED:
                        - <tool names>
                        """.formatted(macroScannerOutput, sectorMapperOutput))
                .call()
                .content();

        String riskEvaluatorOutput = riskEvaluatorClient.prompt()
                .user("""
                        Base brief:
                        %s

                        Macro Scanner notes:
                        %s

                        Sector Mapper notes:
                        %s

                        Stock Screener notes:
                        %s

                        Stress-test the picks and produce final recommendations.

                        Output format:
                        FINAL TOP PICKS (5-10):
                        1. <stock + ticker>
                           - Trend-linked rationale: <short>
                           - Risk factors: <short>
                           - Outlook: <short-term vs long-term>
                        2. <...>
                        2 SECTORS TO AVOID:
                        - <sector + why>
                        - <sector + why>
                        THESIS INVALIDATION RISKS:
                        - <risk trigger>
                        - <risk trigger>
                        """.formatted(DEFAULT_MACRO_WORKFLOW_PROMPT, macroScannerOutput, sectorMapperOutput, stockScreenerOutput))
                .call()
                .content();

        return new MacroHedgeWorkflowResponse(
                DEFAULT_MACRO_WORKFLOW_PROMPT + "\n\nAdditional user focus:\n" + userFocus,
                macroScannerOutput,
                sectorMapperOutput,
                stockScreenerOutput,
                riskEvaluatorOutput);
    }

    private SingleStockFourAgentResponse buildSingleStockFourAgentResponse(AgentWorkflowRequest request) {
        String stockInput = request.task() == null || request.task().isBlank()
                ? "RELIANCE.NS"
                : request.task().trim();

        String macroScannerOutput = macroScannerClient.prompt()
                .user("""
                        Analyze this single stock only:
                        %s

                        Do not analyze or recommend other stocks.

                        Output format:
                        STOCK CONTEXT:
                        - <company + sector + primary business>
                        MACRO/GEOPOLITICAL EXPOSURES:
                        - <exposure + why it matters>
                        RATE/INFLATION/CURRENCY SENSITIVITY:
                        - <sensitivity point>
                        INDIA POLICY/SUPPLY-CHAIN LINK:
                        - <PLI, infra, defense, China+1 linkage if relevant>
                        TOOL EVIDENCE:
                        - <fetched value or unavailable>
                        TOOLS USED:
                        - <tool names>
                        """.formatted(stockInput))
                .call()
                .content();

        String sectorMapperOutput = sectorMapperClient.prompt()
                .user("""
                        Single-stock target:
                        %s

                        Macro Scanner notes:
                        %s

                        Output format:
                        SECTOR POSITIONING FOR THIS STOCK:
                        - <tailwind/headwind>
                        DEMAND/CYCLE DRIVERS:
                        - <driver>
                        COMPETITIVE POSITION:
                        - <moat/market-share insight>
                        NEAR-TERM CATALYSTS (1-3 MONTHS):
                        - <catalyst>
                        MEDIUM-TERM CATALYSTS (6-18 MONTHS):
                        - <catalyst>
                        """.formatted(stockInput, macroScannerOutput))
                .call()
                .content();

        String stockScreenerOutput = stockScreenerClient.prompt()
                .user("""
                        Single-stock target:
                        %s

                        Macro Scanner notes:
                        %s

                        Sector Mapper notes:
                        %s

                        Evaluate only this stock against quality filters.

                        Output format:
                        FUNDAMENTAL QUALITY CHECK:
                        - Revenue trend: <assessment>
                        - Margin profile: <assessment>
                        - Balance sheet/debt: <assessment>
                        - Strategic advantage: <assessment>
                        VALUATION SNAPSHOT:
                        - <valuation context>
                        MOMENTUM & PRICE CONTEXT:
                        - <short context>
                        FILTER VERDICT:
                        - <PASS|WATCH|FAIL> with reason
                        TOOL EVIDENCE:
                        - <fetched value or unavailable>
                        TOOLS USED:
                        - <tool names>
                        """.formatted(stockInput, macroScannerOutput, sectorMapperOutput))
                .call()
                .content();

        String riskEvaluatorOutput = riskEvaluatorClient.prompt()
                .user("""
                        Single-stock target:
                        %s

                        Macro Scanner notes:
                        %s

                        Sector Mapper notes:
                        %s

                        Stock Screener notes:
                        %s

                        Stress-test this stock and provide a final actionable view.

                        Output format:
                        FINAL SIGNAL: <BUY|HOLD|SELL>
                        CONFIDENCE: <0-100>
                        CORE THESIS:
                        - <2-4 points>
                        KEY RISKS:
                        - <risk 1>
                        - <risk 2>
                        THESIS INVALIDATION TRIGGERS:
                        - <trigger 1>
                        - <trigger 2>
                        OUTLOOK:
                        - Short-term (1-3 months): <view>
                        - Long-term (1-3 years): <view>
                        """.formatted(stockInput, macroScannerOutput, sectorMapperOutput, stockScreenerOutput))
                .call()
                .content();

        return new SingleStockFourAgentResponse(
                stockInput,
                macroScannerOutput,
                sectorMapperOutput,
                stockScreenerOutput,
                riskEvaluatorOutput);
    }

    private String buildNifty20UniverseContext() {
        return NIFTY_20_UNIVERSE.stream()
                .map(stock -> "- %s (%s)".formatted(stock.name(), stock.symbol()))
                .collect(Collectors.joining("\n"));
    }

    private String buildIntradaySnapshotContext() {
        return NIFTY_20_UNIVERSE.stream()
                .map(this::fetchIntradaySnapshot)
                .sorted(Comparator.comparingDouble((IntradaySnapshot snapshot) ->
                        snapshot.deltaPercent() == null ? Double.NEGATIVE_INFINITY : Math.abs(snapshot.deltaPercent())).reversed())
                .map(snapshot -> "- %s (%s): Open=%s, Current=%s, Delta=%s, Delta%%=%s, Status=%s"
                        .formatted(
                                snapshot.name(),
                                snapshot.symbol(),
                                formatPrice(snapshot.openPrice()),
                                formatPrice(snapshot.currentPrice()),
                                formatSignedPrice(snapshot.deltaAbsolute()),
                                formatSignedPercent(snapshot.deltaPercent()),
                                snapshot.status()))
                .collect(Collectors.joining("\n"));
    }

    private IntradaySnapshot fetchIntradaySnapshot(StockUniverseItem stock) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=5m&range=1d&includePrePost=false"
                            .formatted(stock.symbol())))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("chart").path("result").get(0);

            if (result == null || result.isMissingNode()) {
                return IntradaySnapshot.unavailable(stock, "chart data unavailable");
            }

            JsonNode meta = result.path("meta");
            JsonNode quote = result.path("indicators").path("quote").get(0);

            Double openPrice = readNumeric(meta.path("regularMarketOpen"));
            if (openPrice == null) {
                openPrice = firstNumeric(quote.path("open"));
            }
            if (openPrice == null) {
                openPrice = firstNumeric(quote.path("close"));
            }

            Double currentPrice = readNumeric(meta.path("regularMarketPrice"));
            if (currentPrice == null) {
                currentPrice = lastNumeric(quote.path("close"));
            }

            if (openPrice == null || currentPrice == null) {
                return IntradaySnapshot.unavailable(stock, "open/current unavailable");
            }

            double deltaAbsolute = currentPrice - openPrice;
            double deltaPercent = openPrice == 0.0 ? 0.0 : (deltaAbsolute / openPrice) * 100.0;

            return new IntradaySnapshot(
                    stock.name(),
                    stock.symbol(),
                    openPrice,
                    currentPrice,
                    deltaAbsolute,
                    deltaPercent,
                    "ok");
        } catch (Exception exception) {
            return IntradaySnapshot.unavailable(stock, exception.getClass().getSimpleName());
        }
    }

    private Double readNumeric(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }

    private Double firstNumeric(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return null;
        }
        for (JsonNode item : arrayNode) {
            Double value = readNumeric(item);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Double lastNumeric(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return null;
        }
        for (int i = arrayNode.size() - 1; i >= 0; i--) {
            Double value = readNumeric(arrayNode.get(i));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String formatPrice(Double value) {
        if (value == null) {
            return "UNAVAILABLE";
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private String formatSignedPrice(Double value) {
        if (value == null) {
            return "UNAVAILABLE";
        }
        return String.format(Locale.US, "%+.2f", value);
    }

    private String formatSignedPercent(Double value) {
        if (value == null) {
            return "UNAVAILABLE";
        }
        return String.format(Locale.US, "%+.2f%%", value);
    }

    private record StockUniverseItem(String name, String symbol) {
    }

    private record IntradaySnapshot(
            String name,
            String symbol,
            Double openPrice,
            Double currentPrice,
            Double deltaAbsolute,
            Double deltaPercent,
            String status) {

        private static IntradaySnapshot unavailable(StockUniverseItem stock, String status) {
            return new IntradaySnapshot(stock.name(), stock.symbol(), null, null, null, null, status);
        }
    }
}
