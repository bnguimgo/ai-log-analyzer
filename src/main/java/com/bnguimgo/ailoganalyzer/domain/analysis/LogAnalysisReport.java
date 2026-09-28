package com.bnguimgo.ailoganalyzer.domain.analysis;

import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;

public record LogAnalysisReport(LogAnalysisResult analysisResult, StructuredContext structuredContext,
                                AiAnalysisResponse aiAnalysisResponse) {

}