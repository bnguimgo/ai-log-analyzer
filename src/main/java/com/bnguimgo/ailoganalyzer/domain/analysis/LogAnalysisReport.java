package com.bnguimgo.ailoganalyzer.domain.analysis;

import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;

public class LogAnalysisReport {

    private final LogAnalysisResult analysisResult;
    private final StructuredContext structuredContext;
    private final AiAnalysisResponse aiAnalysisResponse;

    public LogAnalysisReport(
            LogAnalysisResult analysisResult,
            StructuredContext structuredContext,
            AiAnalysisResponse aiAnalysisResponse) {

        this.analysisResult = analysisResult;
        this.structuredContext = structuredContext;
        this.aiAnalysisResponse = aiAnalysisResponse;
    }

    public LogAnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public StructuredContext getStructuredContext() {
        return structuredContext;
    }

    public AiAnalysisResponse getAiAnalysisResponse() {
        return aiAnalysisResponse;
    }
}