package com.bnguimgo.ailoganalyzer.infrastructure.ai;

import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;

import java.util.List;

public class MockAiClient implements AiClient {

    @Override
    public AiResponse generateAnalysis(
            String prompt,
            String model) {

        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "prompt must not be null or empty"
            );
        }

        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "model must not be null or empty"
            );
        }

        AiResponse response = new AiResponse();

        response.setType(AiResponse.Type.TEXT);
        response.setText(
                "Analyse IA simulée avec le modèle : " + model
        );

        return response;
    }

    @Override
    public AiResponse continueAnalysis(
            AiResponse functionCall,
            List<ToolExecutionResult> toolResults,
            String model) {

        if (functionCall == null) {
            throw new IllegalArgumentException(
                    "functionCall must not be null"
            );
        }

        if (functionCall.getType()
                != AiResponse.Type.FUNCTION_CALL) {

            throw new IllegalArgumentException(
                    "functionCall must be a FUNCTION_CALL"
            );
        }

        if (toolResults == null || toolResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "toolResults must not be null or empty"
            );
        }

        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "model must not be null or empty"
            );
        }

        AiResponse response = new AiResponse();

        response.setType(AiResponse.Type.TEXT);
        response.setText(
                "Analyse IA simulée après exécution des tools : "
                        + toolResults.size()
        );

        return response;
    }
}