package com.bnguimgo.ailoganalyzer.domain.ai.analyzer;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.*;
import com.bnguimgo.ailoganalyzer.domain.ai.prompt.AiPromptBuilder;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.DefaultLogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultAiAnalyzerTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    private AiClient aiClient;
    private AiPromptBuilder promptBuilder;
    private AiProviderProperties properties;
    private ToolExecutor toolExecutor;

    @BeforeEach
    void setUp() {
        aiClient = mock(AiClient.class);
        promptBuilder = mock(AiPromptBuilder.class);
        properties = createProperties();
        toolExecutor = mock(ToolExecutor.class);
        when(promptBuilder.build(any(StructuredContext.class)))
                .thenReturn("Analyse du log");
    }

    @Test
    void shouldAnalyzeStructuredContext() throws IOException {

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
    void shouldExecuteToolAndContinueAnalysis() throws IOException {

        StructuredContext context =
                new StructuredContext(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        LOG_FILE_PATH
                );

        when(promptBuilder.build(context))
                .thenReturn("PROMPT_TEST");

        //Premier appel de l'IA : l'IA demande l'exécution de search_log.
        AiResponse functionCall = createValidFunctionCall("resp_123", "call_123");

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
                anyList(),
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
                                        toolResult.getFirst().callId()
                                )
                                        && toolResult.getFirst().output()
                                        .contains(
                                                "Host name may not be null"
                                        )
                        ),
                        eq("test-model")
                );
    }

    @Test
    void shouldExecuteMultipleToolCallsBeforeFinalResponse() throws IOException {

        StructuredContext context = createContext();

        /*
         * Premier appel de l'IA :
         * l'IA demande un premier search_log.
         */
        AiResponse firstFunctionCall = createValidFunctionCall("resp_1", "call_1");

        AiFunctionCall firstCall = firstFunctionCall.getFunctionCalls().getFirst();
        /*
         * Deuxième appel de l'IA :
         * après le premier résultat du tool,
         * l'IA demande un deuxième search_log.
         */
        AiResponse secondFunctionCall = createValidFunctionCall("resp_2", "call_2");

        AiFunctionCall secondCall = secondFunctionCall.getFunctionCalls().getFirst();

        /*
         * Troisième réponse de l'IA :
         * après le deuxième résultat du tool,
         * l'IA fournit enfin son analyse.
         */
        AiResponse finalResponse = new AiResponse();

        finalResponse.setType(AiResponse.Type.TEXT);

        finalResponse.setText(
                "Analyse finale après deux tool calls."
        );

        when(aiClient.generateAnalysis(
                "Analyse du log",
                "test-model"))
                .thenReturn(firstFunctionCall);

        when(aiClient.continueAnalysis(
                any(AiResponse.class),
                anyList(),
                eq("test-model")))
                .thenReturn(
                        secondFunctionCall,
                        finalResponse
                );

        ToolExecutionResult toolExecutionResult = createToolResult(
                "call_1",
                "Résultat : Host name may not be null");
        when(toolExecutor.execute(
                same(firstCall),
                eq(LOG_FILE_PATH)))
                .thenReturn(toolExecutionResult);

        ToolExecutionResult toolExecutionResult2 = createToolResult(
                "call_2",
                "Résultat : CustomHttpResponseException");
        when(toolExecutor.buildSearchRequest(same(firstCall)))
                .thenReturn(
                        new SearchRequest(
                                "Host name may not be null",
                                "Vérifier cet élément dans le log"
                        )
                );

        when(toolExecutor.buildSearchRequest(same(secondCall)))
                .thenReturn(
                        new SearchRequest(
                                "CustomHttpResponseException",
                                "Vérifier cet élément dans le log"
                        )
                );
        when(toolExecutor.execute(
                same(secondCall),
                eq(LOG_FILE_PATH)))
                .thenReturn(toolExecutionResult2);

        DefaultAiAnalyzer analyzer =
                new DefaultAiAnalyzer(
                        aiClient,
                        promptBuilder,
                        properties,
                        toolExecutor
                );

        AiAnalysisResponse response = analyzer.analyze(context);

        assertNotNull(response);

        assertEquals(
                "Analyse finale après deux tool calls.",
                response.getSummary()
        );

        /*
         * Le premier tool doit avoir été exécuté.
         */
        verify(toolExecutor)
                .execute(
                        same(firstCall),
                        eq(LOG_FILE_PATH)
                );

        /*
         * Le deuxième tool doit également avoir été exécuté.
         */
        verify(toolExecutor)
                .execute(
                        same(secondCall),
                        eq(LOG_FILE_PATH)
                );

        /*
         * L'IA doit avoir été rappelée deux fois :
         * 1. après le premier tool
         * 2. après le deuxième tool
         */
        verify(aiClient)
                .continueAnalysis(
                        same(firstFunctionCall),
                        argThat(toolResults ->
                                toolResults.size() == 1
                                        && "call_1".equals(toolResults.getFirst().callId())
                        ),
                        eq("test-model")
                );

        verify(aiClient)
                .continueAnalysis(
                        same(secondFunctionCall),
                        argThat(toolResults ->
                                "call_2".equals(toolResults.getFirst().callId())
                        ),
                        eq("test-model")
                );
    }

    @Test
    void shouldExecuteMultipleToolCallsFromSameResponse() throws Exception {

        /* * Premier appel : * * application → OpenAI * * OpenAI répond avec DEUX function calls * dans la même réponse. */
        AiResponse functionCallResponse = new AiResponse();
        functionCallResponse.setType(AiResponse.Type.FUNCTION_CALL);
        functionCallResponse.setResponseId("resp_multi");
        AiFunctionCall firstCall = new AiFunctionCall();
        firstCall.setCallId("call_1");
        firstCall.setFunctionName("search_log");
        firstCall.setArguments("{\"searchTerm\":\"Host name may not be null\"}");
        AiFunctionCall secondCall = new AiFunctionCall();
        secondCall.setCallId("call_2");
        secondCall.setFunctionName("search_log");
        secondCall.setArguments("{\"searchTerm\":\"CustomHttpResponseException\"}");
        functionCallResponse.setFunctionCalls(List.of(firstCall, secondCall));

        /* * Résultats produits par les deux outils. */
        ToolExecutionResult toolExecutionResult1 = createToolResult(
                "call_1",
                "résultat pour Host name may not be null");
        ToolExecutionResult toolExecutionResult2 = createToolResult(
                "call_2",
                "résultat pour CustomHttpResponseException");

        AiResponse finalResponse = new AiResponse();
        finalResponse.setType(AiResponse.Type.TEXT);
        finalResponse.setText("Analyse terminée après deux function calls");

        /* * Configuration des mocks. */
        when(toolExecutor.buildSearchRequest(same(firstCall))).thenReturn(new SearchRequest("Host name may not be null", "Vérifier cet élément dans le log"));
        when(toolExecutor.buildSearchRequest(same(secondCall))).thenReturn(new SearchRequest("CustomHttpResponseException", "Vérifier cet élément dans le log"));
        when(aiClient.generateAnalysis(anyString(), eq("test-model"))).thenReturn(functionCallResponse);
        when(toolExecutor.execute(same(firstCall), eq(LOG_FILE_PATH))).thenReturn(toolExecutionResult1);
        when(toolExecutor.execute(same(secondCall), eq(LOG_FILE_PATH))).thenReturn(toolExecutionResult2);
        when(aiClient.continueAnalysis(same(functionCallResponse), anyList(), eq("test-model"))).thenReturn(finalResponse);

        /* * Exécution de l'analyse. */
        DefaultAiAnalyzer analyzer = createAnalyzer(aiClient, promptBuilder, properties, toolExecutor);
        AiAnalysisResponse response = analyzer.analyze(createContext());

        /* * Vérification du résultat final. */
        assertEquals("Analyse terminée après deux function calls", response.getSummary());

        /* * Les deux function calls ont bien été exécutés. */
        verify(toolExecutor, times(1)).execute(same(firstCall), eq(LOG_FILE_PATH));
        verify(toolExecutor, times(1)).execute(same(secondCall), eq(LOG_FILE_PATH));

        /* * Les deux résultats sont transmis * dans UN SEUL appel à continueAnalysis(). * * Et surtout : * call_1 puis call_2. */
        verify(aiClient, times(1))
                .continueAnalysis(same(functionCallResponse),
                        argThat(toolResults -> toolResults.size() == 2
                                && "call_1".equals(toolResults.getFirst().callId())
                                && "call_2".equals(toolResults.get(1).callId())
                                && "résultat pour Host name may not be null".equals(toolResults.getFirst().output())
                                && "résultat pour CustomHttpResponseException".equals(toolResults.get(1).output())), eq("test-model"));
    }

    @Test
    void shouldRejectToolExecutionResultWhenCallIdIsMissing() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolExecutionResult(
                        null,
                        "Host name may not be null",
                        new SearchRequest(
                                "Host name may not be null",
                                "Vérifier cet élément dans le log"
                        )
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"responseId", "callId", "functionName", "arguments"})
    void shouldRejectFunctionCallWhenRequiredFieldIsMissing(String missingField) throws IOException {

        StructuredContext context = createContext();
        when(promptBuilder.build(context)).thenReturn("PROMPT_TEST");
        AiResponse functionCall = createValidFunctionCall("resp_1", "call_1");
        clearField(functionCall, missingField);
        when(aiClient.generateAnalysis("PROMPT_TEST", "test-model"))
                .thenReturn(functionCall);
        DefaultAiAnalyzer analyzer = createAnalyzer(aiClient, promptBuilder, properties, createToolExecutor());
        assertThrows(IllegalStateException.class, () -> analyzer.analyze(context));
        verify(aiClient).generateAnalysis("PROMPT_TEST", "test-model");

        /* Une FUNCTION_CALL invalide ne doit jamais provoquer le deuxième appel à l'IA. */
        verify(aiClient, never()).continueAnalysis(any(), anyList(), any());
    }

    @Test
    void shouldStopAfterMaximumNumberOfToolCalls() throws IOException {

        AiResponse call1 = createValidFunctionCall("resp_1", "call_1");

        when(toolExecutor.buildSearchRequest(any(AiFunctionCall.class)))
                .thenAnswer(invocation -> {
                    AiFunctionCall functionCall = invocation.getArgument(0);
                    return new SearchRequest(functionCall.getCallId(), "Tester le maximum de recherches");
                });
        when(aiClient.generateAnalysis(anyString(), eq("test-model")))
                .thenReturn(call1);

        ToolExecutionResult toolExecutionResult = createToolResult(
                "call_1",
                "Résultat du tool");

        when(toolExecutor.execute(
                any(AiFunctionCall.class),
                eq(LOG_FILE_PATH)))
                .thenAnswer(invocation -> toolExecutionResult);

        AiResponse call2 = createValidFunctionCall("resp_2", "call_2");

        AiResponse call3 = createValidFunctionCall("resp_3", "call_3");

        AiResponse call4 = createValidFunctionCall("resp_4", "call_4");

        AiResponse call5 = createValidFunctionCall("resp_5", "call_5");

        AiResponse call6 = createValidFunctionCall("resp_6", "call_6");

        AiResponse call7 = createValidFunctionCall("resp_7", "call_7");

        AiResponse call8 = createValidFunctionCall("resp_8", "call_8");

        AiResponse call9 = createValidFunctionCall("resp_9", "call_9");

        AiResponse call10 = createValidFunctionCall("resp_10", "call_10");

        AiResponse call11 = createValidFunctionCall("resp_11", "call_11");

        when(aiClient.continueAnalysis(
                any(AiResponse.class),
                anyList(),
                eq("test-model")))
                .thenReturn(
                        call2,
                        call3,
                        call4,
                        call5,
                        call6,
                        call7,
                        call8,
                        call9,
                        call10,
                        call11
                );

        DefaultAiAnalyzer analyzer = new DefaultAiAnalyzer(
                aiClient,
                promptBuilder,
                properties,
                toolExecutor
        );

        assertThrows(
                IllegalStateException.class,
                () -> analyzer.analyze(createContext())
        );

        verify(toolExecutor, times(10))
                .execute(any(AiFunctionCall.class), eq(LOG_FILE_PATH));

        verify(aiClient, times(10))
                .continueAnalysis(
                        any(AiResponse.class),
                        anyList(),
                        eq("test-model")
                );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotExecuteSameSearchTwice() throws IOException {

        AiResponse firstResponse = createValidFunctionCall("resp_1", "call_1");

        AiResponse secondResponse = createValidFunctionCall("resp_2", "call_2");

        AiResponse finalResponse = new AiResponse();
        finalResponse.setType(AiResponse.Type.TEXT);
        finalResponse.setResponseId("resp_3");
        finalResponse.setText("Analyse terminée");

        when(aiClient.generateAnalysis(anyString(), eq("test-model")))
                .thenReturn(firstResponse);

        when(aiClient.continueAnalysis(
                any(AiResponse.class),
                anyList(),
                eq("test-model")))
                .thenReturn(secondResponse, finalResponse);

        ToolExecutionResult toolExecutionResult = createToolResult(
                "call_1",
                "Résultat du tool");

        SearchRequest searchRequest =
                new SearchRequest(
                        "Host name may not be null",
                        "Vérifier cet élément dans le log"
                );

        when(toolExecutor.buildSearchRequest(
                any(AiFunctionCall.class)))
                .thenReturn(searchRequest);

        when(toolExecutor.execute(
                any(AiFunctionCall.class),
                eq(LOG_FILE_PATH)))
                .thenAnswer(invocation -> toolExecutionResult);

        DefaultAiAnalyzer analyzer = new DefaultAiAnalyzer(
                aiClient,
                promptBuilder,
                properties,
                toolExecutor
        );

        analyzer.analyze(createContext());

        verify(toolExecutor, times(1))
                .execute(
                        any(AiFunctionCall.class),
                        eq(LOG_FILE_PATH)
                );

        ArgumentCaptor<List<ToolExecutionResult>> toolResultsCaptor = ArgumentCaptor.forClass(List.class);

        verify(aiClient, times(2))
                .continueAnalysis(
                        any(AiResponse.class),
                        toolResultsCaptor.capture(),
                        eq("test-model")
                );

        List<List<ToolExecutionResult>> allToolResults =
                toolResultsCaptor.getAllValues();

        assertEquals(2, allToolResults.size());

        List<ToolExecutionResult> firstResults = allToolResults.get(0);

        assertEquals(1, firstResults.size());
        assertEquals("call_1", firstResults.get(0).callId());
        assertEquals(
                "Résultat du tool",
                firstResults.get(0).output()
        );

        List<ToolExecutionResult> secondResults = allToolResults.get(1);

        assertEquals(1, secondResults.size());
        assertEquals("call_2", secondResults.get(0).callId());
        assertEquals(
                "Résultat du tool",
                secondResults.get(0).output()
        );
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

    private AiResponse createValidFunctionCall(String responseId, String callId) {

        AiResponse functionCall = new AiResponse();

        functionCall.setType(AiResponse.Type.FUNCTION_CALL);
        functionCall.setResponseId(responseId);

        AiFunctionCall call = new AiFunctionCall();
        call.setCallId(callId);
        call.setFunctionName("search_log");
        call.setArguments(
                "{\"searchTerm\":\"Host name may not be null\","
                        + "\"objective\":\"Vérifier cet élément dans le log\"}"
        );

        functionCall.setFunctionCalls(
                Collections.singletonList(call)
        );

        return functionCall;
    }

    private DefaultAiAnalyzer createAnalyzer(
            AiClient aiClient,
            AiPromptBuilder promptBuilder,
            AiProviderProperties properties,
            ToolExecutor toolExecutor) {

        return new DefaultAiAnalyzer(
                aiClient,
                promptBuilder,
                properties,
                toolExecutor
        );
    }

    private AiProviderProperties createProperties() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setModel("test-model");
        return properties;
    }

    private StructuredContext createContext() {
        return new StructuredContext(Collections.emptyList(),
                Collections.emptyList(),
                LOG_FILE_PATH);
    }

    private void clearField(AiResponse functionCall, String field) {
        switch (field) {
            case "responseId":
                functionCall.setResponseId(null);
                break;
            case "callId":
                functionCall.getFunctionCalls().getFirst().setCallId(null);
                break;
            case "functionName":
                functionCall.getFunctionCalls().getFirst().setFunctionName(null);
                break;
            case "arguments":
                functionCall.getFunctionCalls().getFirst().setArguments(null);
                break;
            default:
                throw new IllegalArgumentException("Unknown field: " + field);
        }
    }

    private ToolExecutionResult createToolResult(
            String callId,
            String output) {

        return new ToolExecutionResult(
                callId,
                output,
                new SearchRequest(
                        "Host name may not be null",
                        "Vérifier cet élément dans le log"
                )
        );
    }

}