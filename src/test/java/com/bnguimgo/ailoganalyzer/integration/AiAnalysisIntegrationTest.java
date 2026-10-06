package com.bnguimgo.ailoganalyzer.integration;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.*;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.DefaultAiAnalyzer;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.DefaultLogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiAnalysisIntegrationTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldExecuteCompleteAiAnalysisFlow() throws IOException {

        /* * ============================================================ *
        1. Composants réels * ============================================================
        */

        AiPromptBuilder promptBuilder = new AiPromptBuilder();
        LogSearchTool logSearchTool = new DefaultLogSearchTool();
        ToolExecutor toolExecutor = new ToolExecutor(logSearchTool, new ObjectMapper());

        /* * Le client IA reste mocké : * nous voulons tester l'orchestration sans dépendre * de l'API OpenAI. */

        AiClient aiClient = mock(AiClient.class);
        AiProviderProperties properties = new AiProviderProperties();
        properties.setModel("test-model");
        DefaultAiAnalyzer analyzer = new DefaultAiAnalyzer(
                aiClient, promptBuilder, properties, toolExecutor);

        /* * ============================================================ *
        2. Contexte réel de l'analyse
        * ============================================================ */

        StructuredContext context = new StructuredContext(
                Collections.emptyList(),
                Collections.emptyList(),
                LOG_FILE_PATH);

        /* ============================================================
        3. Première réponse de l'IA * * L'IA demande l'exécution du tool search_log.
        * ============================================================ */

        AiResponse functionCall = new AiResponse();
        functionCall.setType(AiResponse.Type.FUNCTION_CALL);
        functionCall.setResponseId("resp_123");
        AiFunctionCall aiFunctionCall = new AiFunctionCall();
        aiFunctionCall.setCallId("call_123");
        aiFunctionCall.setFunctionName("search_log");
        aiFunctionCall.setArguments("{\"searchTerm\":\"Host name may not be null\"}");
        functionCall.setFunctionCalls(Collections.singletonList(aiFunctionCall));

        /* * ============================================================ *
        4. Réponse finale simulée de l'IA
        * ============================================================ */

        AiResponse finalResponse = new AiResponse();
        finalResponse.setType(AiResponse.Type.TEXT);
        finalResponse.setText("Le problème vient de la configuration du proxy.");

        /* * ============================================================ *
        5. Simulation des deux appels IA
        * ============================================================ */

        when(aiClient.generateAnalysis(any(String.class), eq("test-model")))
                .thenReturn(functionCall);
        when(aiClient.continueAnalysis(same(functionCall), anyList(), eq("test-model")))
                .thenReturn(finalResponse);

        /* * ============================================================ *
        6. Exécution du flux complet * *
        AiPromptBuilder réel * ↓ *
        AiClient.generateAnalysis() * ↓ *
        FUNCTION_CALL * ↓ *
        ToolExecutor réel * ↓ *
        DefaultLogSearchTool réel * ↓ *
        fichier log réel * ↓ *
        AiClient.continueAnalysis() * ↓ *
        TEXT
        * ============================================================ */

        AiAnalysisResponse response = analyzer.analyze(context);

        /* * ============================================================ *
        7. Vérification du résultat final
        * ============================================================ */

        assertNotNull(response);

        assertEquals("Le problème vient de la configuration du proxy.", response.getSummary());

        /* * ============================================================ *
        8. Vérification du premier appel IA * * Le prompt est construit par le vrai AiPromptBuilder.
        * ============================================================ */

        verify(aiClient).generateAnalysis(
                argThat(prompt -> prompt.contains("Tu es un expert en analyse de logs Java/Spring.")
                        && prompt.contains("=== INCIDENTS ===")
                        && prompt.contains("=== RELATIONS ENTRE INCIDENTS ===")
                        && prompt.contains("=== ATTENDU ===")), eq("test-model"));

        /* * ============================================================ *
        9. Vérification du résultat réel du tool *
        * Le ToolExecutor et le DefaultLogSearchTool sont réels.
        * Le résultat doit donc provenir du fichier log.
        * ============================================================ */

        verify(aiClient).continueAnalysis(
                same(functionCall),
                argThat(toolResults -> "call_123".equals(toolResults.getFirst().callId())
                        && toolResults.getFirst().output().contains("Host name may not be null")), eq("test-model"));

        /* * ============================================================ *
        10. Vérification du nombre d'appels
        * ============================================================ */

        verify(aiClient, times(1)).generateAnalysis(any(String.class), eq("test-model"));
        verify(aiClient, times(1)).continueAnalysis(any(AiResponse.class), anyList(), eq("test-model"));

    }

    @Test
    void shouldExecuteCompleteAiAnalysisFlowWithMultipleTools() throws IOException { /* ============================================================ * 1. Composants réels * ============================================================ */
        AiPromptBuilder promptBuilder = new AiPromptBuilder();
        LogSearchTool logSearchTool = new DefaultLogSearchTool();
        ToolExecutor toolExecutor = new ToolExecutor(logSearchTool, new ObjectMapper()); /* * Le client IA reste mocké. * * On teste ici l'orchestration complète de l'agent * sans dépendre de l'API OpenAI. */
        AiClient aiClient = mock(AiClient.class);
        AiProviderProperties properties = new AiProviderProperties();
        properties.setModel("test-model");
        DefaultAiAnalyzer analyzer = new DefaultAiAnalyzer(aiClient, promptBuilder, properties, toolExecutor); /* ============================================================ * 2. Contexte réel de l'analyse * ============================================================ */
        StructuredContext context = new StructuredContext(Collections.emptyList(), Collections.emptyList(), LOG_FILE_PATH); /* ============================================================ * 3. Première réponse de l'IA * * OpenAI demande DEUX recherches dans la même réponse. * ============================================================ */

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
        functionCallResponse.setFunctionCalls(List.of(firstCall, secondCall)); /* ============================================================ * 4. Réponse finale simulée de l'IA * ============================================================ */
        AiResponse finalResponse = new AiResponse();
        finalResponse.setType(AiResponse.Type.TEXT);
        finalResponse.setText("Analyse terminée après deux recherches."); /* ============================================================ * 5. Simulation des deux appels IA * * Premier appel : * generateAnalysis() * ↓ * FUNCTION_CALL * ├── call_1 * └── call_2 * * Second appel : * continueAnalysis() * ↓ * TEXT * ============================================================ */
        when(aiClient.generateAnalysis(any(String.class), eq("test-model"))).thenReturn(functionCallResponse);
        when(aiClient.continueAnalysis(same(functionCallResponse), anyList(), eq("test-model"))).thenReturn(finalResponse); /* ============================================================ * 6. Exécution du flux complet * * AiPromptBuilder réel * ↓ * AiClient.generateAnalysis() * ↓ * FUNCTION_CALL x2 * ↓ * ToolExecutor réel * ↓ * DefaultLogSearchTool réel * ↓ * fichier log réel * ↓ * ToolExecutionResult x2 * ↓ * AiClient.continueAnalysis() * ↓ * TEXT * ============================================================ */
        AiAnalysisResponse response = analyzer.analyze(context);
        /* ============================================================ *
        7. Vérification du résultat final
        * ============================================================ */
        assertNotNull(response);
        assertEquals("Analyse terminée après deux recherches.", response.getSummary());

        /* ============================================================ *
        8. Vérification du premier appel IA * * Le prompt est construit par le vrai AiPromptBuilder.
        * ============================================================ */
        verify(aiClient).generateAnalysis(argThat(prompt -> prompt.contains("Tu es un expert en analyse de logs Java/Spring.") && prompt.contains("=== INCIDENTS ===") && prompt.contains("=== RELATIONS ENTRE INCIDENTS ===") && prompt.contains("=== ATTENDU ===")), eq("test-model")); /* ============================================================ * 9. Vérification des deux résultats réels du tool * * Le ToolExecutor et le DefaultLogSearchTool sont réels. * Les résultats doivent donc provenir du fichier log. * ============================================================ */
        verify(aiClient).continueAnalysis(same(functionCallResponse),
                argThat(toolResults -> toolResults.size() == 2
                && "call_1".equals(toolResults.getFirst().callId())
                        && toolResults.getFirst().output().contains("Host name may not be null")
                        && "call_2".equals(toolResults.get(1).callId())
                        && toolResults.get(1).output().contains("CustomHttpResponseException")), eq("test-model"));
        /* ============================================================ *
        10. Vérification du nombre d'appels
        * ============================================================ */
        verify(aiClient, times(1)).generateAnalysis(any(String.class), eq("test-model"));
        verify(aiClient, times(1)).continueAnalysis(any(AiResponse.class), anyList(), eq("test-model"));
    }

}
