package com.bnguimgo.ailoganalyzer.integration;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.prompt.AiPromptBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.DefaultAiAnalyzer;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.openai.OpenAiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.DefaultLogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiAnalysisOpenAiIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String MODEL = "test-model";

    private static final Path LOG_FILE_PATH =
            Paths.get("src/test/resources/logs/manageo-cognito.log");

    private HttpServer server;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final List<String> requestBodies =
            new ArrayList<>();

    private final List<String> authorizationHeaders =
            new ArrayList<>();

    private int requestCount;

    private boolean duplicateSearchScenario;

    @BeforeEach
    void setUp() throws IOException {

        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext(
                "/v1/responses",
                this::handleRequest
        );

        server.start();

        requestCount = 0;
        requestBodies.clear();
        authorizationHeaders.clear();
        duplicateSearchScenario = false;
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldExecuteCompleteAgentFlowWithMultipleTools()
            throws IOException {

        /*
         * ---------------------------------------------------------
         * 1. Création du vrai OpenAiClient
         * ---------------------------------------------------------
         */

        String apiUrl =
                "http://localhost:"
                        + server.getAddress().getPort()
                        + "/v1/responses";

        AiClient aiClient =
                new OpenAiClient(
                        API_KEY,
                        apiUrl,
                        HttpClient.newHttpClient(),
                        objectMapper
                );

        /*
         * ---------------------------------------------------------
         * 2. Création des vrais composants de l'agent
         * ---------------------------------------------------------
         */

        AiAnalysisResponse response = getAiAnalysisResponse(aiClient);

        /*
         * ---------------------------------------------------------
         * 5. Vérification de la réponse finale
         * ---------------------------------------------------------
         */

        assertNotNull(response);

        assertEquals(
                "Analyse terminée après deux recherches.",
                response.getSummary()
        );

        /*
         * ---------------------------------------------------------
         * 6. Vérification du nombre d'appels HTTP
         * ---------------------------------------------------------
         */

        assertEquals(
                2,
                requestCount
        );

        assertEquals(
                2,
                requestBodies.size()
        );

        assertEquals(
                2,
                authorizationHeaders.size()
        );

        /*
         * ---------------------------------------------------------
         * 7. Vérification du premier appel HTTP
         * ---------------------------------------------------------
         */

        JsonNode firstRequest =
                objectMapper.readTree(
                        requestBodies.getFirst()
                );

        assertEquals(
                MODEL,
                firstRequest.path("model").asText()
        );

        assertTrue(
                firstRequest.path("input")
                        .asText()
                        .contains(
                                "Tu es un expert en analyse de logs Java/Spring."
                        )
        );

        assertTrue(
                firstRequest.has("tools")
        );

        assertEquals(
                1,
                firstRequest.path("tools").size()
        );

        assertEquals(
                "search_log",
                firstRequest
                        .path("tools")
                        .get(0)
                        .path("name")
                        .asText()
        );

        assertEquals(
                "Bearer " + API_KEY,
                authorizationHeaders.getFirst()
        );

        /*
         * ---------------------------------------------------------
         * 8. Vérification du deuxième appel HTTP
         * ---------------------------------------------------------
         */

        JsonNode secondRequest =
                objectMapper.readTree(
                        requestBodies.get(1)
                );

        assertEquals(
                MODEL,
                secondRequest.path("model").asText()
        );

        assertEquals(
                "resp_multi",
                secondRequest
                        .path("previous_response_id")
                        .asText()
        );

        assertEquals(
                "Bearer " + API_KEY,
                authorizationHeaders.get(1)
        );

        /*
         * ---------------------------------------------------------
         * 9. Vérification des deux function_call_output
         * ---------------------------------------------------------
         */

        JsonNode input =
                secondRequest.path("input");

        assertTrue(
                input.isArray()
        );

        assertEquals(
                2,
                input.size()
        );

        JsonNode firstToolOutput =
                input.get(0);

        assertEquals(
                "function_call_output",
                firstToolOutput
                        .path("type")
                        .asText()
        );

        assertEquals(
                "call_1",
                firstToolOutput
                        .path("call_id")
                        .asText()
        );

        assertTrue(
                firstToolOutput
                        .path("output")
                        .asText()
                        .contains(
                                "Host name may not be null"
                        )
        );

        JsonNode secondToolOutput =
                input.get(1);

        assertEquals(
                "function_call_output",
                secondToolOutput
                        .path("type")
                        .asText()
        );

        assertEquals(
                "call_2",
                secondToolOutput
                        .path("call_id")
                        .asText()
        );

        assertTrue(
                secondToolOutput
                        .path("output")
                        .asText()
                        .contains(
                                "CustomHttpResponseException"
                        )
        );
    }

    @Test
    void shouldReuseResultForDuplicateToolCall()
            throws IOException {

        duplicateSearchScenario = true;

        String apiUrl =
                "http://localhost:"
                        + server.getAddress().getPort()
                        + "/v1/responses";

        AiClient aiClient =
                new OpenAiClient(
                        API_KEY,
                        apiUrl,
                        HttpClient.newHttpClient(),
                        objectMapper
                );

        AiAnalysisResponse response =
                getAiAnalysisResponse(aiClient);

        assertNotNull(response);

        assertEquals(
                "Analyse terminée après une recherche dupliquée.",
                response.getSummary()
        );

        /*
         * Deux appels HTTP :
         *
         * 1. OpenAI demande deux recherches identiques
         * 2. L'application renvoie les deux function_call_output
         */
        assertEquals(
                2,
                requestCount
        );

        assertEquals(
                2,
                requestBodies.size()
        );

        /*
         * Vérification du deuxième appel vers OpenAI.
         */
        JsonNode secondRequest =
                objectMapper.readTree(
                        requestBodies.get(1)
                );

        assertEquals(
                "resp_duplicate",
                secondRequest
                        .path("previous_response_id")
                        .asText()
        );

        JsonNode input =
                secondRequest.path("input");

        assertTrue(input.isArray());

        assertEquals(
                2,
                input.size()
        );

        /*
         * Premier function_call.
         */
        JsonNode firstToolOutput =
                input.get(0);

        assertEquals(
                "function_call_output",
                firstToolOutput
                        .path("type")
                        .asText()
        );

        assertEquals(
                "call_1",
                firstToolOutput
                        .path("call_id")
                        .asText()
        );

        /*
         * Deuxième function_call.
         *
         * Il doit recevoir le même résultat,
         * mais avec son propre callId.
         */
        JsonNode secondToolOutput =
                input.get(1);

        assertEquals(
                "function_call_output",
                secondToolOutput
                        .path("type")
                        .asText()
        );

        assertEquals(
                "call_2",
                secondToolOutput
                        .path("call_id")
                        .asText()
        );

        assertEquals(
                firstToolOutput
                        .path("output")
                        .asText(),
                secondToolOutput
                        .path("output")
                        .asText()
        );

        /*
         * Le point essentiel du test :
         *
         * les deux function_calls sont identiques,
         * mais la recherche réelle ne doit être exécutée
         * qu'une seule fois.
         */
        assertTrue(
                firstToolOutput
                        .path("output")
                        .asText()
                        .contains(
                                "Host name may not be null"
                        )
        );
    }

    private AiAnalysisResponse getAiAnalysisResponse(AiClient aiClient) throws IOException {
        DefaultAiAnalyzer analyzer = getDefaultAiAnalyzer(aiClient);

        /*
         * ---------------------------------------------------------
         * 3. Contexte réel avec le vrai fichier de log
         * ---------------------------------------------------------
         */

        StructuredContext context =
                new StructuredContext(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        LOG_FILE_PATH
                );

        /*
         * ---------------------------------------------------------
         * 4. Exécution complète de l'agent
         * ---------------------------------------------------------
         *
         * C'est ici que toute la chaîne est réellement exécutée :
         *
         * DefaultAiAnalyzer
         *        ↓
         * OpenAiClient
         *        ↓
         * Mock HTTP OpenAI
         *        ↓
         * 2 function_calls
         *        ↓
         * ToolExecutor
         *        ↓
         * DefaultLogSearchTool
         *        ↓
         * vrai fichier de log
         *        ↓
         * OpenAiClient
         *        ↓
         * Mock HTTP OpenAI
         *        ↓
         * réponse TEXT
         */

        return analyzer.analyze(context);
    }

    private DefaultAiAnalyzer getDefaultAiAnalyzer(AiClient aiClient) {
        AiPromptBuilder promptBuilder =
                new AiPromptBuilder();

        LogSearchTool logSearchTool =
                new DefaultLogSearchTool();

        ToolExecutor toolExecutor =
                new ToolExecutor(
                        logSearchTool,
                        objectMapper
                );

        AiProviderProperties properties =
                new AiProviderProperties();

        properties.setModel(MODEL);

        return new DefaultAiAnalyzer(
                        aiClient,
                        promptBuilder,
                        properties,
                        toolExecutor
                );
    }

    private void handleRequest(
            HttpExchange exchange) throws IOException {

        try (exchange) {

            assertEquals(
                    "POST",
                    exchange.getRequestMethod()
            );

            assertEquals(
                    "application/json",
                    exchange.getRequestHeaders()
                            .getFirst("Content-Type")
            );

            authorizationHeaders.add(
                    exchange.getRequestHeaders()
                            .getFirst("Authorization")
            );

            String requestBody =
                    new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            requestBodies.add(requestBody);

            requestCount++;

            String responseBody;

            if (duplicateSearchScenario) {

                if (requestCount == 1) {
                    responseBody = createDuplicateFunctionCallResponse();
                } else if (requestCount == 2) {
                    responseBody = createDuplicateFinalTextResponse();
                } else {
                    throw new IllegalStateException(
                            "Unexpected HTTP request count: "
                                    + requestCount
                    );
                }

            } else {

                if (requestCount == 1) {
                    responseBody =
                            createMultipleFunctionCallResponse();
                } else if (requestCount == 2) {
                    responseBody =
                            createFinalTextResponse();
                } else {
                    throw new IllegalStateException(
                            "Unexpected HTTP request count: "
                                    + requestCount
                    );
                }
            }

            byte[] responseBytes =
                    responseBody.getBytes(
                            StandardCharsets.UTF_8
                    );

            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "application/json"
                    );

            exchange.sendResponseHeaders(
                    200,
                    responseBytes.length
            );

            try (OutputStream outputStream =
                         exchange.getResponseBody()) {

                outputStream.write(responseBytes);
            }
        }
    }

    private String createMultipleFunctionCallResponse() {

        return objectMapper.createObjectNode()
                .put(
                        "id",
                        "resp_multi"
                )
                .set(
                        "output",
                        objectMapper.createArrayNode()
                                .add(
                                        createFunctionCall(
                                                "call_1",
                                                "Host name may not be null",
                                                "Vérifier la présence de cette exception dans le log"
                                        )
                                )
                                .add(
                                        createFunctionCall(
                                                "call_2",
                                                "CustomHttpResponseException",
                                                "CustomHttpResponseException"
                                        )
                                )
                )
                .toString();
    }

    private JsonNode createFunctionCall(
            String callId,
            String searchTerm,
            String objective) {

        return objectMapper.createObjectNode()
                .put(
                        "type",
                        "function_call"
                )
                .put(
                        "call_id",
                        callId
                )
                .put(
                        "name",
                        "search_log"
                )
                .put(
                        "arguments",
                        "{\"searchTerm\":\"" + searchTerm + "\", \"objective\":\"" + objective + "\"}"
                );
    }

    private String createFinalTextResponse() {

        return objectMapper.createObjectNode()
                .put(
                        "id",
                        "resp_final"
                )
                .set(
                        "output",
                        objectMapper.createArrayNode()
                                .add(
                                        objectMapper.createObjectNode()
                                                .put(
                                                        "type",
                                                        "message"
                                                )
                                                .set(
                                                        "content",
                                                        objectMapper
                                                                .createArrayNode()
                                                                .add(
                                                                        objectMapper
                                                                                .createObjectNode()
                                                                                .put(
                                                                                        "type",
                                                                                        "output_text"
                                                                                )
                                                                                .put(
                                                                                        "text",
                                                                                        "Analyse terminée après deux recherches."
                                                                                )
                                                                )
                                                )
                                )
                )
                .toString();
    }

    private String createDuplicateFunctionCallResponse() {

        return objectMapper.createObjectNode()
                .put(
                        "id",
                        "resp_duplicate"
                )
                .set(
                        "output",
                        objectMapper.createArrayNode()
                                .add(
                                        createFunctionCall(
                                                "call_1",
                                                "Host name may not be null",
                                                "Vérifier la présence de cette exception dans le log"
                                        )
                                )
                                .add(
                                        createFunctionCall(
                                                "call_2",
                                                "Host name may not be null",
                                                "Vérifier la présence de cette exception dans le log"
                                        )
                                )
                )
                .toString();
    }

    private String createDuplicateFinalTextResponse() {

        return objectMapper.createObjectNode()
                .put(
                        "id",
                        "resp_duplicate_final"
                )
                .set(
                        "output",
                        objectMapper.createArrayNode()
                                .add(
                                        objectMapper.createObjectNode()
                                                .put(
                                                        "type",
                                                        "message"
                                                )
                                                .set(
                                                        "content",
                                                        objectMapper
                                                                .createArrayNode()
                                                                .add(
                                                                        objectMapper
                                                                                .createObjectNode()
                                                                                .put(
                                                                                        "type",
                                                                                        "output_text"
                                                                                )
                                                                                .put(
                                                                                        "text",
                                                                                        "Analyse terminée après une recherche dupliquée."
                                                                                )
                                                                )
                                                )
                                )
                )
                .toString();
    }

}