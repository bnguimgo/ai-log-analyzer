package com.bnguimgo.ailoganalyzer.domain.ai.analyzer;

import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;

import java.io.IOException;

public interface AiAnalyzer {

    AiAnalysisResponse analyze(StructuredContext context) throws IOException;
}