package com.bnguimgo.ailoganalyzer.infrastructure.ai;

import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockAiClientTest {

    private final MockAiClient client =
            new MockAiClient();

    @Test
    void shouldGenerateTextResponse() {

        AiResponse response =
                client.generateAnalysis(
                        "Analyse ce log",
                        "mock-model"
                );

        assertEquals(
                AiResponse.Type.TEXT,
                response.getType()
        );

        assertEquals(
                "Analyse IA simulée avec le modèle : mock-model",
                response.getText()
        );
    }

    @Test
    void shouldRejectEmptyPrompt() {

        assertThrows(
                IllegalArgumentException.class,
                () -> client.generateAnalysis(
                        "",
                        "mock-model"
                )
        );
    }

    @Test
    void shouldRejectEmptyModel() {

        assertThrows(
                IllegalArgumentException.class,
                () -> client.generateAnalysis(
                        "Analyse ce log",
                        ""
                )
        );
    }

    @Test
    void shouldContinueAnalysisAfterToolExecution() {

        AiResponse functionCall =
                new AiResponse();

        functionCall.setType(
                AiResponse.Type.FUNCTION_CALL
        );
        functionCall.setCallId("call_123");
        functionCall.setFunctionName("search_log");
        functionCall.setArguments(
                "{\"searchTerm\":\"Host name may not be null\"}"
        );

        ToolExecutionResult toolResult =
                new ToolExecutionResult(
                        "call_123",
                        "Ligne 42 : Host name may not be null"
                );

        AiResponse response =
                client.continueAnalysis(
                        functionCall,
                        List.of(toolResult),
                        "mock-model"
                );

        assertEquals(
                AiResponse.Type.TEXT,
                response.getType()
        );

        assertEquals(
                "Analyse IA simulée après exécution des tools : 1",
                response.getText()
        );
    }

    @Test
    void shouldRejectNullFunctionCall() {

        ToolExecutionResult toolResult =
                new ToolExecutionResult(
                        "call_123",
                        "Résultat du tool"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.continueAnalysis(
                        null,
                        List.of(toolResult),
                        "mock-model"
                )
        );
    }

    @Test
    void shouldRejectTextResponseAsFunctionCall() {

        AiResponse response =
                new AiResponse();

        response.setType(
                AiResponse.Type.TEXT
        );

        ToolExecutionResult toolResult =
                new ToolExecutionResult(
                        "call_123",
                        "Résultat du tool"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.continueAnalysis(
                        response,
                        List.of(toolResult),
                        "mock-model"
                )
        );
    }

    @Test
    void shouldRejectNullToolResult() {

        AiResponse functionCall =
                new AiResponse();

        functionCall.setType(
                AiResponse.Type.FUNCTION_CALL
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.continueAnalysis(
                        functionCall,
                        null,
                        "mock-model"
                )
        );
    }

    @Test
    void shouldRejectEmptyModelWhenContinuingAnalysis() {

        AiResponse functionCall =
                new AiResponse();

        functionCall.setType(
                AiResponse.Type.FUNCTION_CALL
        );

        ToolExecutionResult toolResult =
                new ToolExecutionResult(
                        "call_123",
                        "Résultat du tool"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.continueAnalysis(
                        functionCall,
                        List.of(toolResult),
                        ""
                )
        );
    }
}