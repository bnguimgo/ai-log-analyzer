package com.bnguimgo.ailoganalyzer.infrastructure.ai;

import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;

import java.io.IOException;
import java.util.List;

public interface AiClient {

    AiResponse generateAnalysis(
            String prompt,
            String model
    );

    AiResponse continueAnalysis(
            AiResponse functionCall,
            List<ToolExecutionResult> toolResults,
            String model) throws IOException;
}