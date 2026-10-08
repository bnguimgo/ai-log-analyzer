package com.bnguimgo.ailoganalyzer.infrastructure.ai.openai;

import com.bnguimgo.ailoganalyzer.domain.ai.AiFunctionCall;
import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.SearchRequest;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiClientIntegrationTest {

    private static final String API_KEY =
            "test-api-key";

    private static final String MODEL =
            "test-model";

    private HttpServer server;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> requestBodies = new ArrayList<>();

    private final List<String> authorizationHeaders = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {

        //Création et démarrage d'un serveur HTTP local fonctionnant sur un port aléatoire
        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext(
                "/v1/responses",
                this::handleRequest
        );

        server.start();
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldExecuteCompleteResponsesApiFlow() throws JsonProcessingException {

        String apiUrl =
                "http://localhost:"
                        + server.getAddress().getPort()
                        + "/v1/responses";

        OpenAiClient client =
                new OpenAiClient(
                        API_KEY,
                        apiUrl,
                        HttpClient.newHttpClient(),
                        objectMapper
                );

        /*
         * ---------------------------------------------------------
         * 1. Premier appel : demande d'analyse
         * ---------------------------------------------------------
         */

        AiResponse functionCall =
                client.generateAnalysis(
                        "Analyse ce log",
                        MODEL
                );

        assertNotNull(functionCall);

        assertEquals(
                AiResponse.Type.FUNCTION_CALL,
                functionCall.getType()
        );

        assertEquals(
                "resp_test_123",
                functionCall.getResponseId()
        );

        AiFunctionCall aiFunctionCall = functionCall.getFunctionCalls().getFirst();

        assertEquals(
                "call_test_123",
                aiFunctionCall.getCallId()
        );

        assertEquals(
                "search_log",
                aiFunctionCall.getFunctionName()
        );

        assertEquals(
                "{\"searchTerm\":\"Host name may not be null\"}",
                aiFunctionCall.getArguments()
        );

        /*
         * ---------------------------------------------------------
         * Vérification du premier HTTP request
         * ---------------------------------------------------------
         */

        assertEquals(
                1,
                requestBodies.size()
        );

        JsonNode firstRequest =
                objectMapper.readTree(
                        requestBodies.getFirst()
                );

        assertEquals(
                MODEL,
                firstRequest.path("model").asText()
        );

        assertEquals(
                "Analyse ce log",
                firstRequest.path("input").asText()
        );

        assertTrue(
                firstRequest.has("tools")
        );

        assertTrue(
                firstRequest.path("tools").isArray()
        );

        assertEquals(
                1,
                firstRequest.path("tools").size()
        );

        JsonNode tool =
                firstRequest
                        .path("tools")
                        .get(0);

        assertEquals(
                "function",
                tool.path("type").asText()
        );

        assertEquals(
                "search_log",
                tool.path("name").asText()
        );

        assertEquals(
                "Bearer " + API_KEY,
                authorizationHeaders.getFirst()
        );

        /*
         * ---------------------------------------------------------
         * 2. Exécution simulée de la fonction
         * ---------------------------------------------------------
         */

        ToolExecutionResult toolResult = createToolResult(
                aiFunctionCall.getCallId()
        );

        /*
         * ---------------------------------------------------------
         * 3. Deuxième appel : function_call_output
         * ---------------------------------------------------------
         */

        AiResponse finalResponse =
                client.continueAnalysis(
                        functionCall,
                        List.of(toolResult),
                        MODEL
                );

        assertNotNull(finalResponse);

        assertEquals(
                AiResponse.Type.TEXT,
                finalResponse.getType()
        );

        assertEquals(
                "Le problème vient de la configuration du proxy.",
                finalResponse.getText()
        );

        /*
         * ---------------------------------------------------------
         * Vérification du deuxième HTTP request
         * ---------------------------------------------------------
         */

        assertEquals(
                2,
                requestBodies.size()
        );

        JsonNode secondRequest =
                objectMapper.readTree(
                        requestBodies.get(1)
                );

        assertEquals(
                MODEL,
                secondRequest.path("model").asText()
        );

        assertEquals(
                "resp_test_123",
                secondRequest
                        .path("previous_response_id")
                        .asText()
        );

        assertEquals(
                "Bearer " + API_KEY,
                authorizationHeaders.get(1)
        );

        JsonNode input =
                secondRequest.path("input");

        assertTrue(
                input.isArray()
        );

        assertEquals(
                1,
                input.size()
        );

        JsonNode functionCallOutput =
                input.get(0);

        assertEquals(
                "function_call_output",
                functionCallOutput
                        .path("type")
                        .asText()
        );

        assertEquals(
                "call_test_123",
                functionCallOutput
                        .path("call_id")
                        .asText()
        );

        assertEquals(
                "Host name may not be null",
                functionCallOutput
                        .path("output")
                        .asText()
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

            JsonNode request =
                    objectMapper.readTree(requestBody);

            boolean continuation =
                    request.has("previous_response_id");

            String responseBody =
                    continuation
                            ? createFinalTextResponse()
                            : createFunctionCallResponse();

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

    private String createFunctionCallResponse() {

        return objectMapper.createObjectNode()
                .put(
                        "id",
                        "resp_test_123"
                )
                .set(
                        "output",
                        objectMapper.createArrayNode()
                                .add(
                                        objectMapper.createObjectNode()
                                                .put(
                                                        "type",
                                                        "function_call"
                                                )
                                                .put(
                                                        "call_id",
                                                        "call_test_123"
                                                )
                                                .put(
                                                        "name",
                                                        "search_log"
                                                )
                                                .put(
                                                        "arguments",
                                                        "{\"searchTerm\":\"Host name may not be null\"}"
                                                )
                                )
                )
                .toString();
    }

    private String createFinalTextResponse() {

        return objectMapper.createObjectNode()
                .put(
                        "id",
                        "resp_test_456"
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
                                                        objectMapper.createArrayNode()
                                                                .add(
                                                                        objectMapper.createObjectNode()
                                                                                .put(
                                                                                        "type",
                                                                                        "output_text"
                                                                                )
                                                                                .put(
                                                                                        "text",
                                                                                        "Le problème vient de la configuration du proxy."
                                                                                )
                                                                )
                                                )
                                )
                )
                .toString();
    }

    private ToolExecutionResult createToolResult(
            String callId) {

        return new ToolExecutionResult(
                callId,
                "Host name may not be null",
                new SearchRequest(
                        "Host name may not be null",
                        "Vérifier cet élément dans le log"
                )
        );
    }
}
