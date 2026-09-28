package com.bnguimgo.ailoganalyzer.domain.ai.analyzer;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.AiPromptBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.DefaultLogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultAiAnalyzerTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldAnalyzeStructuredContext() {

        AiClient aiClient = mock(AiClient.class);
        AiPromptBuilder promptBuilder = mock(AiPromptBuilder.class);

        AiProviderProperties properties =
                new AiProviderProperties();

        properties.setModel("test-model");

        StructuredContext context =
                new StructuredContext(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        LOG_FILE_PATH
                );

        when(promptBuilder.build(context))
                .thenReturn("PROMPT_TEST");

        AiResponse aiResponse = new AiResponse();
        aiResponse.setType(AiResponse.Type.TEXT);
        aiResponse.setText("Analyse générée");

        when(aiClient.generateAnalysis(
                "PROMPT_TEST",
                "test-model"))
                .thenReturn(aiResponse);

        ToolExecutor toolExecutor =
                createToolExecutor();

        DefaultAiAnalyzer analyzer =
                new DefaultAiAnalyzer(
                        aiClient,
                        promptBuilder,
                        properties,
                        toolExecutor
                );

        AiAnalysisResponse response =
                analyzer.analyze(context);

        assertNotNull(response);

        assertEquals(
                "Analyse générée",
                response.getSummary()
        );

        verify(promptBuilder)
                .build(context);

        verify(aiClient)
                .generateAnalysis(
                        "PROMPT_TEST",
                        "test-model"
                );

        verify(aiClient, never())
                .continueAnalysis(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldRejectNullContext() {

        AiClient aiClient = mock(AiClient.class);
        AiPromptBuilder promptBuilder = mock(AiPromptBuilder.class);

        AiProviderProperties properties =
                new AiProviderProperties();

        properties.setModel("test-model");

        ToolExecutor toolExecutor =
                createToolExecutor();

        DefaultAiAnalyzer analyzer =
                new DefaultAiAnalyzer(
                        aiClient,
                        promptBuilder,
                        properties,
                        toolExecutor
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> analyzer.analyze(null)
        );

        verifyNoInteractions(aiClient);
        verifyNoInteractions(promptBuilder);
    }

    @Test
    void shouldExecuteToolAndContinueAnalysis() {

        AiClient aiClient = mock(AiClient.class);
        AiPromptBuilder promptBuilder = mock(AiPromptBuilder.class);

        AiProviderProperties properties =
                new AiProviderProperties();

        properties.setModel("test-model");

        StructuredContext context =
                new StructuredContext(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        LOG_FILE_PATH
                );

        when(promptBuilder.build(context))
                .thenReturn("PROMPT_TEST");

        //Premier appel de l'IA : l'IA demande l'exécution de search_log.
        AiResponse functionCall =
                new AiResponse();

        functionCall.setType(
                AiResponse.Type.FUNCTION_CALL
        );

        functionCall.setResponseId(
                "resp_123"
        );

        functionCall.setCallId(
                "call_123"
        );

        functionCall.setFunctionName(
                "search_log"
        );

        functionCall.setArguments(
                "{\"searchTerm\":\"Host name may not be null\"}"
        );

        /*
         * Deuxième réponse de l'IA :
         * après réception du résultat du tool,
         * l'IA fournit son analyse finale.
         */
        AiResponse finalResponse =
                new AiResponse();

        finalResponse.setType(
                AiResponse.Type.TEXT
        );

        finalResponse.setText(
                "Le problème vient de la configuration du proxy."
        );

        when(aiClient.generateAnalysis(
                "PROMPT_TEST",
                "test-model"))
                .thenReturn(functionCall);

        when(aiClient.continueAnalysis(
                same(functionCall),
                any(ToolExecutionResult.class),
                eq("test-model")))
                .thenReturn(finalResponse);

        ToolExecutor toolExecutor =
                createToolExecutor();

        DefaultAiAnalyzer analyzer =
                new DefaultAiAnalyzer(
                        aiClient,
                        promptBuilder,
                        properties,
                        toolExecutor
                );

        AiAnalysisResponse response =
                analyzer.analyze(context);

        assertNotNull(response);

        assertEquals(
                "Le problème vient de la configuration du proxy.",
                response.getSummary()
        );

        /*
         * Vérifie la construction du prompt.
         */
        verify(promptBuilder)
                .build(context);

        /*
         * Vérifie le premier appel à l'IA.
         */
        verify(aiClient)
                .generateAnalysis(
                        "PROMPT_TEST",
                        "test-model"
                );

        /*
         * Vérifie que l'IA a bien été rappelée
         * après l'exécution du tool.
         */
        verify(aiClient)
                .continueAnalysis(
                        same(functionCall),
                        argThat(toolResult ->
                                "call_123".equals(
                                        toolResult.callId()
                                )
                                        && toolResult.output()
                                        .contains(
                                                "Host name may not be null"
                                        )
                        ),
                        eq("test-model")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = { "responseId", "callId", "functionName", "arguments" })
    void shouldRejectFunctionCallWhenRequiredFieldIsMissing(String missingField) {

        AiClient aiClient = mock(AiClient.class);
        AiPromptBuilder promptBuilder = mock(AiPromptBuilder.class);
        AiProviderProperties properties = createProperties();
        StructuredContext context = createContext();
        when(promptBuilder.build(context)) .thenReturn("PROMPT_TEST");
        AiResponse functionCall = createValidFunctionCall();
        clearField( functionCall, missingField );
        when(aiClient.generateAnalysis( "PROMPT_TEST", "test-model"))
                .thenReturn(functionCall);
        DefaultAiAnalyzer analyzer = createAnalyzer( aiClient, promptBuilder, properties );
        assertThrows( IllegalStateException.class, () -> analyzer.analyze(context) );
        verify(aiClient) .generateAnalysis( "PROMPT_TEST", "test-model" );

        /* Une FUNCTION_CALL invalide ne doit jamais provoquer le deuxième appel à l'IA. */
        verify(aiClient, never()) .continueAnalysis( any(), any(), any() );
    }

    private ToolExecutor createToolExecutor() {

        LogSearchTool logSearchTool =
                new DefaultLogSearchTool();

        ObjectMapper objectMapper =
                new ObjectMapper();

        return new ToolExecutor(
                logSearchTool,
                objectMapper
        );
    }

    private AiResponse createValidFunctionCall() {

        AiResponse functionCall = new AiResponse();

        functionCall.setType(AiResponse.Type.FUNCTION_CALL);
        functionCall.setResponseId("resp_123");
        functionCall.setCallId("call_123");
        functionCall.setFunctionName("search_log");
        functionCall.setArguments(
                "{\"searchTerm\":\"Host name may not be null\"}"
        );

        return functionCall;
    }

    private DefaultAiAnalyzer createAnalyzer(
            AiClient aiClient,
            AiPromptBuilder promptBuilder,
            AiProviderProperties properties) {

        ToolExecutor toolExecutor = createToolExecutor();

        return new DefaultAiAnalyzer(
                aiClient,
                promptBuilder,
                properties,
                toolExecutor
        );
    }

    private AiProviderProperties createProperties() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setModel("test-model"); return properties;
    }

    private StructuredContext createContext() {
        return new StructuredContext( Collections.emptyList(),
                Collections.emptyList(),
                LOG_FILE_PATH );
    }

    private void clearField(AiResponse functionCall, String field) {
        switch (field) {
            case "responseId": functionCall.setResponseId(null);
            break;
            case "callId": functionCall.setCallId(null);
            break;
            case "functionName": functionCall.setFunctionName(null);
            break;
            case "arguments": functionCall.setArguments(null);
            break;
            default: throw new IllegalArgumentException( "Unknown field: " + field );
        }
    }

}