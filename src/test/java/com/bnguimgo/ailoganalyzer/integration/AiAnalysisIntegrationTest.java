package com.bnguimgo.ailoganalyzer.integration;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.AiPromptBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.DefaultAiAnalyzer;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.DefaultLogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiAnalysisIntegrationTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldExecuteCompleteAiAnalysisFlow() {

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

        /* * ============================================================ *
        3. Première réponse de l'IA * * L'IA demande l'exécution du tool search_log.
        * ============================================================ */

        AiResponse functionCall = new AiResponse();
        functionCall.setType(AiResponse.Type.FUNCTION_CALL);
        functionCall.setResponseId("resp_123");
        functionCall.setCallId("call_123");
        functionCall.setFunctionName("search_log");
        functionCall.setArguments("{\"searchTerm\":\"Host name may not be null\"}");

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
        when(aiClient.continueAnalysis(same(functionCall), any(ToolExecutionResult.class), eq("test-model")))
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
                argThat(toolResult -> "call_123".equals(toolResult.callId())
                        && toolResult.output().contains("Host name may not be null")), eq("test-model"));

        /* * ============================================================ *
        10. Vérification du nombre d'appels
        * ============================================================ */

        verify(aiClient, times(1)).generateAnalysis(any(String.class), eq("test-model"));
        verify(aiClient, times(1)).continueAnalysis(any(AiResponse.class), any(ToolExecutionResult.class), eq("test-model"));

    }

}
