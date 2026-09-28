package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

public record ToolExecutionResult(String callId, String output) {

    public ToolExecutionResult {

        if (callId == null || callId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "callId must not be null or empty"
            );
        }

        if (output == null) {
            throw new IllegalArgumentException(
                    "output must not be null"
            );
        }

    }
}