package com.bnguimgo.ailoganalyzer.infrastructure.ai;

import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;

public interface AiClient {

    AiResponse generateAnalysis(
            String prompt,
            String model
    );

    AiResponse continueAnalysis(
            AiResponse functionCall,
            ToolExecutionResult toolResult,
            String model);
}