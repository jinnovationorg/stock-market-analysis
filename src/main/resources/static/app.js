const taskInput = document.getElementById("task");
const tabButtons = Array.from(document.querySelectorAll(".tab-btn"));
const tabPanels = Array.from(document.querySelectorAll(".tab-panel"));
const runGetBtn = document.getElementById("runGetBtn");
const runPostBtn = document.getElementById("runPostBtn");
const clearBtn = document.getElementById("clearBtn");
const refreshToolsBtn = document.getElementById("refreshToolsBtn");
const indiaAnalysisBtn = document.getElementById("indiaAnalysisBtn");
const intradayAnalysisBtn = document.getElementById("intradayAnalysisBtn");
const macroRunBtn = document.getElementById("macroRunBtn");
const macroTaskInput = document.getElementById("macroTask");
const singleStockRunBtn = document.getElementById("singleStockRunBtn");
const singleStockTaskInput = document.getElementById("singleStockTask");
const runStatus = document.getElementById("runStatus");
const indiaStatus = document.getElementById("indiaStatus");
const intradayStatus = document.getElementById("intradayStatus");
const macroStatus = document.getElementById("macroStatus");
const singleStockStatus = document.getElementById("singleStockStatus");
const toolsStatus = document.getElementById("toolsStatus");
const researchOutput = document.getElementById("researchOutput");
const signalOutput = document.getElementById("signalOutput");
const indiaResearchOutput = document.getElementById("indiaResearchOutput");
const indiaRecommendationsOutput = document.getElementById("indiaRecommendationsOutput");
const intradayResearchOutput = document.getElementById("intradayResearchOutput");
const intradayRecommendationsOutput = document.getElementById("intradayRecommendationsOutput");
const macroScannerOutput = document.getElementById("macroScannerOutput");
const sectorMapperOutput = document.getElementById("sectorMapperOutput");
const stockScreenerOutput = document.getElementById("stockScreenerOutput");
const riskEvaluatorOutput = document.getElementById("riskEvaluatorOutput");
const singleStockMacroScannerOutput = document.getElementById("singleStockMacroScannerOutput");
const singleStockSectorMapperOutput = document.getElementById("singleStockSectorMapperOutput");
const singleStockStockScreenerOutput = document.getElementById("singleStockStockScreenerOutput");
const singleStockRiskEvaluatorOutput = document.getElementById("singleStockRiskEvaluatorOutput");
const toolsOutput = document.getElementById("toolsOutput");

function switchTab(targetPanelId) {
    tabButtons.forEach((button) => {
        button.classList.toggle("active", button.dataset.tab === targetPanelId);
    });

    tabPanels.forEach((panel) => {
        panel.classList.toggle("active", panel.id === targetPanelId);
    });
}

function setRunStatus(message, isError = false) {
    runStatus.textContent = message;
    runStatus.style.color = isError ? "#9f1d1d" : "";
}

function setToolsStatus(message, isError = false) {
    toolsStatus.textContent = message;
    toolsStatus.style.color = isError ? "#9f1d1d" : "";
}

function setIndiaStatus(message, isError = false) {
    indiaStatus.textContent = message;
    indiaStatus.style.color = isError ? "#9f1d1d" : "";
}

function setIntradayStatus(message, isError = false) {
    intradayStatus.textContent = message;
    intradayStatus.style.color = isError ? "#9f1d1d" : "";
}

function setMacroStatus(message, isError = false) {
    if (!macroStatus) {
        return;
    }
    macroStatus.textContent = message;
    macroStatus.style.color = isError ? "#9f1d1d" : "";
}

function setSingleStockStatus(message, isError = false) {
    if (!singleStockStatus) {
        return;
    }
    singleStockStatus.textContent = message;
    singleStockStatus.style.color = isError ? "#9f1d1d" : "";
}

function safeStringify(value) {
    return JSON.stringify(value, null, 2);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function signalToneClass(value) {
    const normalized = (value || "").trim().toUpperCase();
    if (normalized.includes("BULLISH") || normalized.includes("BUY")) {
        return "is-positive";
    }
    if (normalized.includes("BEARISH") || normalized.includes("SELL")) {
        return "is-negative";
    }
    return "is-neutral";
}

function parseIntradaySections(content) {
    const lines = (content || "").split("\n");
    const sections = {};
    let currentSection = null;

    for (const rawLine of lines) {
        const line = rawLine.trim();
        if (!line) {
            continue;
        }

        if (line.includes(":") && !line.startsWith("-") && !/^\d+\./.test(line)) {
            const [key, ...rest] = line.split(":");
            const value = rest.join(":").trim();
            const upperKey = key.trim().toUpperCase();

            if ([
                "INTRADAY VIEW",
                "CONFIDENCE",
                "SUMMARY"
            ].includes(upperKey)) {
                sections[upperKey] = value;
                currentSection = null;
                continue;
            }

            currentSection = upperKey;
            if (!sections[currentSection]) {
                sections[currentSection] = [];
            }
            if (value) {
                sections[currentSection].push(value);
            }
            continue;
        }

        if (currentSection) {
            if (!sections[currentSection]) {
                sections[currentSection] = [];
            }
            sections[currentSection].push(line.replace(/^\d+\.\s*/, ""));
        }
    }

    return sections;
}

function parseIndiaSections(content) {
    const lines = (content || "").split("\n");
    const sections = {};
    let currentSection = null;

    for (const rawLine of lines) {
        const line = rawLine.trim();
        if (!line) {
            continue;
        }

        if (line.includes(":") && !line.startsWith("-") && !/^\d+\./.test(line)) {
            const [key, ...rest] = line.split(":");
            const value = rest.join(":").trim();
            const upperKey = key.trim().toUpperCase();

            if ([
                "MARKET VIEW",
                "CONFIDENCE",
                "SUMMARY",
                "DISCLAIMER"
            ].includes(upperKey)) {
                sections[upperKey] = value;
                currentSection = null;
                continue;
            }

            currentSection = upperKey;
            if (!sections[currentSection]) {
                sections[currentSection] = [];
            }
            if (value) {
                sections[currentSection].push(value);
            }
            continue;
        }

        if (currentSection) {
            if (!sections[currentSection]) {
                sections[currentSection] = [];
            }
            sections[currentSection].push(line.replace(/^\d+\.\s*/, ""));
        }
    }

    return sections;
}

function extractSectionHeading(line) {
    const trimmed = line.trim();

    if (/^#{1,6}\s+.+/.test(trimmed)) {
        return trimmed.replace(/^#{1,6}\s+/, "").trim();
    }

    if (/^\*\*[^*]+\*\*$/.test(trimmed)) {
        return trimmed.replace(/\*\*/g, "").trim();
    }

    if (/^[A-Za-z][A-Za-z0-9 &/()'-]{1,48}:$/.test(trimmed)) {
        return trimmed.slice(0, -1).trim();
    }

    return null;
}

function parseNarrativeSections(content) {
    const lines = (content || "").split("\n");
    const overview = [];
    const sections = [];
    let currentSection = null;

    for (const rawLine of lines) {
        const line = rawLine.trim();
        if (!line) {
            continue;
        }

        const heading = extractSectionHeading(line);
        if (heading) {
            currentSection = { title: heading, items: [] };
            sections.push(currentSection);
            continue;
        }

        const normalizedLine = line.replace(/^\d+\.\s*/, "").replace(/^-+\s*/, "").trim();
        if (!normalizedLine) {
            continue;
        }

        if (currentSection) {
            currentSection.items.push(normalizedLine);
        } else {
            overview.push(normalizedLine);
        }
    }

    return { overview, sections };
}

function renderListItems(items, itemClass = "") {
    if (!items || !items.length) {
        return '<div class="intraday-empty">No items available.</div>';
    }

    return items
        .map((item) => `<div class="intraday-list-item ${itemClass}">${escapeHtml(item)}</div>`)
        .join("");
}

function renderIntradayIdeas(items) {
    if (!items || !items.length) {
        return '<div class="intraday-empty">No intraday ideas available.</div>';
    }

    return items.map((item) => {
        const parts = item.split(" - ").map((part) => part.trim());
        const instrument = parts[0] || "";
        const signal = parts[1] || "N/A";
        const detail = parts.slice(2).join(" - ");

        return `
            <article class="idea-card">
                <div class="idea-top-row">
                    <h3>${escapeHtml(instrument)}</h3>
                    <span class="signal-pill ${signalToneClass(signal)}">${escapeHtml(signal)}</span>
                </div>
                <p class="idea-detail">${escapeHtml(detail || item)}</p>
            </article>
        `;
    }).join("");
}

function renderIndiaIdeas(items) {
    if (!items || !items.length) {
        return '<div class="intraday-empty">No Nifty 20 recommendations available.</div>';
    }

    return items.map((item) => {
        const parts = item.split(" - ").map((part) => part.trim());
        const instrument = parts[0] || "";
        const detail = parts.slice(1).join(" - ");

        return `
            <article class="idea-card">
                <div class="idea-top-row">
                    <h3>${escapeHtml(instrument)}</h3>
                    <span class="signal-pill is-neutral">NIFTY 20</span>
                </div>
                <p class="idea-detail">${escapeHtml(detail || item)}</p>
            </article>
        `;
    }).join("");
}

function renderResearchDashboard(targetElement, content, emptyMessage) {
    if (!targetElement) {
        return;
    }

    if (!content) {
        targetElement.innerHTML = `<div class="intraday-empty">${escapeHtml(emptyMessage)}</div>`;
        return;
    }

    const { overview, sections } = parseNarrativeSections(content);

    targetElement.innerHTML = `
        <section class="intraday-section">
            <h3>Market Readout</h3>
            <div class="intraday-list">
                ${overview.length
                    ? renderListItems(overview, "is-research")
                    : '<div class="intraday-empty">No overview available.</div>'}
            </div>
        </section>
        ${sections.length ? `
            <section class="intraday-section-grid">
                ${sections.map((section) => `
                    <section class="intraday-section section-card">
                        <h3>${escapeHtml(section.title)}</h3>
                        <div class="intraday-list">
                            ${renderListItems(section.items, "is-research")}
                        </div>
                    </section>
                `).join("")}
            </section>
        ` : ""}
    `;
}

function renderIntradayResearch(content) {
    renderResearchDashboard(intradayResearchOutput, content, "No research output.");
}

function renderIntradayDashboard(content) {
    if (!intradayRecommendationsOutput) {
        return;
    }

    if (!content) {
        intradayRecommendationsOutput.innerHTML = '<div class="intraday-empty">No recommendation output.</div>';
        return;
    }

    const sections = parseIntradaySections(content);
    const intradayView = sections["INTRADAY VIEW"] || "N/A";
    const confidence = sections["CONFIDENCE"] || "N/A";
    const summary = sections["SUMMARY"] || "No summary available.";
    const criteria = sections["SIGNAL CRITERIA"] || [];
    const ideas = sections["TOP 5 INTRADAY IDEAS"] || [];
    const catalysts = sections["INTRADAY CATALYSTS"] || [];
    const risks = sections["RISKS"] || [];
    const watchlist = sections["OPENING WATCHLIST"] || [];

    intradayRecommendationsOutput.innerHTML = `
        <div class="intraday-summary-grid">
            <section class="summary-card">
                <span class="summary-label">Intraday View</span>
                <strong class="summary-value ${signalToneClass(intradayView)}">${escapeHtml(intradayView)}</strong>
            </section>
            <section class="summary-card">
                <span class="summary-label">Confidence</span>
                <strong class="summary-value">${escapeHtml(confidence)}</strong>
            </section>
        </div>
        <section class="intraday-section">
            <h3>Summary</h3>
            <p class="intraday-summary-text">${escapeHtml(summary)}</p>
        </section>
        <section class="intraday-section">
            <h3>Signal Criteria</h3>
            <div class="intraday-list">${renderListItems(criteria)}</div>
        </section>
        <section class="intraday-section">
            <h3>Top 5 Intraday Ideas</h3>
            <div class="idea-grid">${renderIntradayIdeas(ideas)}</div>
        </section>
        <section class="intraday-meta-grid">
            <div class="intraday-section">
                <h3>Intraday Catalysts</h3>
                <div class="intraday-list">${renderListItems(catalysts)}</div>
            </div>
            <div class="intraday-section">
                <h3>Risks</h3>
                <div class="intraday-list">${renderListItems(risks)}</div>
            </div>
            <div class="intraday-section">
                <h3>Opening Watchlist</h3>
                <div class="intraday-list">${renderListItems(watchlist)}</div>
            </div>
        </section>
    `;
}

function renderIndiaResearch(content) {
    renderResearchDashboard(indiaResearchOutput, content, "No India market research output.");
}

function renderMacroOutputs(data) {
    renderResearchDashboard(macroScannerOutput, data.macroScannerOutput || "", "No macro scanner output.");
    renderResearchDashboard(sectorMapperOutput, data.sectorMapperOutput || "", "No sector mapper output.");
    renderResearchDashboard(stockScreenerOutput, data.stockScreenerOutput || "", "No stock screener output.");
    renderResearchDashboard(riskEvaluatorOutput, data.riskEvaluatorOutput || "", "No risk evaluator output.");
}

function renderSingleStockOutputs(data) {
    renderResearchDashboard(singleStockMacroScannerOutput, data.macroScannerOutput || "", "No macro scanner output.");
    renderResearchDashboard(singleStockSectorMapperOutput, data.sectorMapperOutput || "", "No sector mapper output.");
    renderResearchDashboard(singleStockStockScreenerOutput, data.stockScreenerOutput || "", "No stock screener output.");
    renderResearchDashboard(singleStockRiskEvaluatorOutput, data.riskEvaluatorOutput || "", "No risk evaluator output.");
}

function renderIndiaDashboard(content) {
    if (!indiaRecommendationsOutput) {
        return;
    }

    if (!content) {
        indiaRecommendationsOutput.innerHTML = '<div class="intraday-empty">No recommendation output.</div>';
        return;
    }

    const sections = parseIndiaSections(content);
    const marketView = sections["MARKET VIEW"] || "N/A";
    const confidence = sections["CONFIDENCE"] || "N/A";
    const summary = sections["SUMMARY"] || "No summary available.";
    const ideas = sections["TOP NIFTY 20 STOCKS TO BUY"]
        || sections["TOP 20 STOCKS TO BUY"]
        || sections["TOP STOCKS TO BUY"]
        || [];
    const risks = sections["RISKS"] || [];
    const watchlist = sections["MACRO & GEOPOLITICAL WATCHLIST"] || [];
    const disclaimer = sections["DISCLAIMER"] || "Not financial advice.";

    indiaRecommendationsOutput.innerHTML = `
        <div class="intraday-summary-grid">
            <section class="summary-card">
                <span class="summary-label">Market View</span>
                <strong class="summary-value ${signalToneClass(marketView)}">${escapeHtml(marketView)}</strong>
            </section>
            <section class="summary-card">
                <span class="summary-label">Confidence</span>
                <strong class="summary-value">${escapeHtml(confidence)}</strong>
            </section>
        </div>
        <section class="intraday-section">
            <h3>Summary</h3>
            <p class="intraday-summary-text">${escapeHtml(summary)}</p>
        </section>
        <section class="intraday-section">
            <h3>Top Nifty 20 Recommendations</h3>
            <div class="idea-grid">${renderIndiaIdeas(ideas)}</div>
        </section>
        <section class="intraday-meta-grid">
            <div class="intraday-section">
                <h3>Risks</h3>
                <div class="intraday-list">${renderListItems(risks)}</div>
            </div>
            <div class="intraday-section">
                <h3>Macro & Geopolitical Watchlist</h3>
                <div class="intraday-list">${renderListItems(watchlist)}</div>
            </div>
            <div class="intraday-section">
                <h3>Disclaimer</h3>
                <div class="intraday-list">${renderListItems([disclaimer])}</div>
            </div>
        </section>
    `;
}

function clearOutputs() {
    researchOutput.textContent = "(Run workflow to view research output.)";
    signalOutput.textContent = "(Run workflow to view signal output.)";
    if (indiaResearchOutput) {
        indiaResearchOutput.innerHTML = '<div class="intraday-empty">Run Indian market analysis to view formatted research.</div>';
    }
    if (indiaRecommendationsOutput) {
        indiaRecommendationsOutput.innerHTML = '<div class="intraday-empty">Run Indian market analysis to view formatted Nifty 20 recommendations.</div>';
    }
    if (intradayResearchOutput) {
        intradayResearchOutput.innerHTML = '<div class="intraday-empty">Run intraday analysis to view formatted market research.</div>';
    }
    if (intradayRecommendationsOutput) {
        intradayRecommendationsOutput.innerHTML = '<div class="intraday-empty">Run intraday analysis to view formatted signals, deltas, and trade ideas.</div>';
    }
    if (macroScannerOutput) {
        macroScannerOutput.innerHTML = '<div class="intraday-empty">Run workflow to view output.</div>';
    }
    if (sectorMapperOutput) {
        sectorMapperOutput.innerHTML = '<div class="intraday-empty">Run workflow to view output.</div>';
    }
    if (stockScreenerOutput) {
        stockScreenerOutput.innerHTML = '<div class="intraday-empty">Run workflow to view output.</div>';
    }
    if (riskEvaluatorOutput) {
        riskEvaluatorOutput.innerHTML = '<div class="intraday-empty">Run workflow to view output.</div>';
    }
    if (singleStockMacroScannerOutput) {
        singleStockMacroScannerOutput.innerHTML = '<div class="intraday-empty">Run workflow to view output.</div>';
    }
    if (singleStockSectorMapperOutput) {
        singleStockSectorMapperOutput.innerHTML = '<div class="intraday-empty">Run workflow to view output.</div>';
    }
    if (singleStockStockScreenerOutput) {
        singleStockStockScreenerOutput.innerHTML = '<div class="intraday-empty">Run workflow to view output.</div>';
    }
    if (singleStockRiskEvaluatorOutput) {
        singleStockRiskEvaluatorOutput.innerHTML = '<div class="intraday-empty">Run workflow to view output.</div>';
    }
    setSingleStockStatus("Output cleared.");
    setMacroStatus("Output cleared.");
    setRunStatus("Output cleared.");
}

async function runWorkflow(method) {
    const task = taskInput.value.trim();
    if (!task) {
        setRunStatus("stock to analyze is required.", true);
        return;
    }

    setRunStatus(`Running workflow via ${method}...`);
    runGetBtn.disabled = true;
    runPostBtn.disabled = true;

    try {
        let response;
        if (method === "GET") {
            const query = new URLSearchParams({ task });
            response = await fetch(`/api/agents/run?${query.toString()}`);
        } else {
            response = await fetch("/api/agents/run", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ task })
            });
        }

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        researchOutput.textContent = data.researchAgentOutput || "(No research output)";
        signalOutput.textContent = data.writerAgentOutput || "(No signal output)";
        setRunStatus("Workflow completed.");
    } catch (error) {
        setRunStatus(`Failed to run workflow: ${error.message}`, true);
    } finally {
        runGetBtn.disabled = false;
        runPostBtn.disabled = false;
    }
}

async function loadTools() {
    setToolsStatus("Loading MCP tools...");
    refreshToolsBtn.disabled = true;

    try {
        const response = await fetch("/api/agents/mcp/tools");
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        const data = await response.json();
        toolsOutput.textContent = safeStringify(data);
        setToolsStatus(`Loaded ${data.count || 0} MCP tools.`);
    } catch (error) {
        setToolsStatus(`Failed to load tools: ${error.message}`, true);
    } finally {
        refreshToolsBtn.disabled = false;
    }
}

async function runIndianMarketAnalysis() {
    setIndiaStatus("Running Indian market analysis...");
    indiaAnalysisBtn.disabled = true;

    try {
        const response = await fetch("/api/agents/india/analysis");
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        const data = await response.json();
        renderIndiaResearch(data.researchAgentOutput || "");
        renderIndiaDashboard(data.writerAgentOutput || "");
        setIndiaStatus("Indian market analysis completed.");
    } catch (error) {
        setIndiaStatus(`Failed to run India analysis: ${error.message}`, true);
    } finally {
        indiaAnalysisBtn.disabled = false;
    }
}

async function runIntradayMarketAnalysis() {
    setIntradayStatus("Running intraday market analysis...");
    intradayAnalysisBtn.disabled = true;

    try {
        const response = await fetch("/api/agents/intraday/analysis");
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        const data = await response.json();
        renderIntradayResearch(data.researchAgentOutput || "");
        renderIntradayDashboard(data.writerAgentOutput || "");
        setIntradayStatus("Intraday market analysis completed.");
    } catch (error) {
        setIntradayStatus(`Failed to run intraday analysis: ${error.message}`, true);
    } finally {
        intradayAnalysisBtn.disabled = false;
    }
}

async function runMacroWorkflow() {
    const task = macroTaskInput ? macroTaskInput.value.trim() : "";
    setMacroStatus("Running 4-agent macro workflow...");
    if (macroRunBtn) {
        macroRunBtn.disabled = true;
    }

    try {
        const response = await fetch("/api/agents/macro/workflow", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ task: task || "Focus on India-centric macro and sector opportunities." })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        renderMacroOutputs(data);

        setMacroStatus("4-agent macro workflow completed.");
    } catch (error) {
        setMacroStatus(`Failed to run macro workflow: ${error.message}`, true);
    } finally {
        if (macroRunBtn) {
            macroRunBtn.disabled = false;
        }
    }
}

async function runSingleStockWorkflow() {
    const task = singleStockTaskInput ? singleStockTaskInput.value.trim() : "";
    if (!task) {
        setSingleStockStatus("Single stock input is required.", true);
        return;
    }

    setSingleStockStatus("Running single-stock 4-agent workflow...");
    if (singleStockRunBtn) {
        singleStockRunBtn.disabled = true;
    }

    try {
        const response = await fetch("/api/agents/stock/workflow", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ task })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        renderSingleStockOutputs(data);
        setSingleStockStatus("Single-stock 4-agent workflow completed.");
    } catch (error) {
        setSingleStockStatus(`Failed to run single-stock workflow: ${error.message}`, true);
    } finally {
        if (singleStockRunBtn) {
            singleStockRunBtn.disabled = false;
        }
    }
}

if (runGetBtn) {
    runGetBtn.addEventListener("click", () => runWorkflow("GET"));
}

if (runPostBtn) {
    runPostBtn.addEventListener("click", () => runWorkflow("POST"));
}

if (clearBtn) {
    clearBtn.addEventListener("click", clearOutputs);
}

if (refreshToolsBtn) {
    refreshToolsBtn.addEventListener("click", loadTools);
}

if (indiaAnalysisBtn) {
    indiaAnalysisBtn.addEventListener("click", runIndianMarketAnalysis);
}

if (intradayAnalysisBtn) {
    intradayAnalysisBtn.addEventListener("click", runIntradayMarketAnalysis);
}

if (macroRunBtn) {
    macroRunBtn.addEventListener("click", runMacroWorkflow);
}

if (singleStockRunBtn) {
    singleStockRunBtn.addEventListener("click", runSingleStockWorkflow);
}

tabButtons.forEach((button) => {
    button.addEventListener("click", () => {
        if (button.dataset.tab) {
            switchTab(button.dataset.tab);
        }
    });
});

loadTools();
