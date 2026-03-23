const taskInput = document.getElementById("task");
const tabWorkflow = document.getElementById("tabWorkflow");
const tabIndia = document.getElementById("tabIndia");
const workflowTab = document.getElementById("workflowTab");
const indiaTab = document.getElementById("indiaTab");
const runGetBtn = document.getElementById("runGetBtn");
const runPostBtn = document.getElementById("runPostBtn");
const clearBtn = document.getElementById("clearBtn");
const refreshToolsBtn = document.getElementById("refreshToolsBtn");
const indiaAnalysisBtn = document.getElementById("indiaAnalysisBtn");
const runStatus = document.getElementById("runStatus");
const indiaStatus = document.getElementById("indiaStatus");
const toolsStatus = document.getElementById("toolsStatus");
const researchOutput = document.getElementById("researchOutput");
const signalOutput = document.getElementById("signalOutput");
const indiaResearchOutput = document.getElementById("indiaResearchOutput");
const indiaRecommendationsOutput = document.getElementById("indiaRecommendationsOutput");
const toolsOutput = document.getElementById("toolsOutput");

function switchTab(target) {
    const showingWorkflow = target === "workflow";
    tabWorkflow.classList.toggle("active", showingWorkflow);
    tabIndia.classList.toggle("active", !showingWorkflow);
    workflowTab.classList.toggle("active", showingWorkflow);
    indiaTab.classList.toggle("active", !showingWorkflow);
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

function safeStringify(value) {
    return JSON.stringify(value, null, 2);
}

function clearOutputs() {
    researchOutput.textContent = "";
    signalOutput.textContent = "";
    setRunStatus("Output cleared.");
}

async function runWorkflow(method) {
    const task = taskInput.value.trim();
    if (!task) {
        setRunStatus("Task is required.", true);
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
        indiaResearchOutput.textContent = data.researchAgentOutput || "(No research output)";
        indiaRecommendationsOutput.textContent = data.writerAgentOutput || "(No recommendation output)";
        setIndiaStatus("Indian market analysis completed.");
    } catch (error) {
        setIndiaStatus(`Failed to run India analysis: ${error.message}`, true);
    } finally {
        indiaAnalysisBtn.disabled = false;
    }
}

runGetBtn.addEventListener("click", () => runWorkflow("GET"));
runPostBtn.addEventListener("click", () => runWorkflow("POST"));
clearBtn.addEventListener("click", clearOutputs);
refreshToolsBtn.addEventListener("click", loadTools);
indiaAnalysisBtn.addEventListener("click", runIndianMarketAnalysis);
tabWorkflow.addEventListener("click", () => switchTab("workflow"));
tabIndia.addEventListener("click", () => switchTab("india"));

loadTools();
