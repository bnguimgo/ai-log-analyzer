package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import com.bnguimgo.ailoganalyzer.domain.ai.SearchRequest;

public record ToolExecutionResult(
        String callId,
        String output,
        SearchRequest searchRequest) {

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

        if (searchRequest == null) {
            throw new IllegalArgumentException(
                    "searchRequest must not be null"
            );
        }
    }
}