package com.bnguimgo.ailoganalyzer.infrastructure.ai.openai;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
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

import static org.junit.jupiter.api.Assertions.*;

class OpenAiClientTest {

    private HttpServer server;
    private String receivedAuthorization;
    private final List<String> receivedBodies = new ArrayList<>();
    private int requestCount;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private OpenAiClient openAiClient;

    @BeforeEach
    void setUp() throws IOException {

        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/v1/responses", this::handleRequest);

        server.start();

        openAiClient = new OpenAiClient(
                "test-api-key",
                "http://localhost:" + server.getAddress().getPort() + "/v1/responses",
                HttpClient.newHttpClient(),
                objectMapper );

        requestCount = 0;
        receivedBodies.clear();
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldCallResponsesApiAndExtractText() throws Exception {

        AiResponse result =
                openAiClient.generateAnalysis(
                        "Analyse cet incident",
                        "test-model"
                );

        assertEquals( AiResponse.Type.TEXT, result.getType() );

        assertEquals( "Analyse terminée", result.getText() );

        assertEquals( "Bearer test-api-key", receivedAuthorization );

        assertRequestContainsToolDefinition();
    }

    @Test
    void shouldCreateClientFromProviderProperties() throws Exception {

        AiProviderProperties properties = new AiProviderProperties();
        properties.setType("openai");
        properties.setApiKey( "test-api-key" );
        properties.setApiUrl( "http://localhost:" + server.getAddress().getPort() + "/v1/responses" );
        properties.setModel( "test-model" );
        AiResponse result = openAiClient.generateAnalysis( "Analyse cet incident", properties.getModel() );
        assertEquals( AiResponse.Type.TEXT, result.getType() );
        assertEquals( "Analyse terminée", result.getText() );
        assertEquals( "Bearer test-api-key", receivedAuthorization );
        assertRequestContainsToolDefinition();
    }

    @Test void shouldExecuteTwoStepFunctionCallFlow() throws Exception {


        /* * Premier appel :
        * utilisateur - OpenAI
        * OpenAI répond avec un function_call.
        */
        AiResponse functionCall = openAiClient.generateAnalysis( "Recherche l'erreur dans le log", "test-model" );
        assertEquals( AiResponse.Type.FUNCTION_CALL, functionCall.getType() );
        assertEquals(
                "resp_123",
                functionCall.getResponseId()
        );
        assertEquals( "call_123", functionCall.getCallId() );
        assertEquals( "search_log", functionCall.getFunctionName() );
        assertEquals( "{\"searchTerm\":\"Host name may not be null\"}", functionCall.getArguments() );

        //Résultat produit par notre ToolExecutor.
        ToolExecutionResult toolResult =
                new ToolExecutionResult( functionCall.getCallId(), "Ligne 42 : Host name may not be null" );

        /* * Deuxième appel :
        *ToolExecutor → OpenAI * * avec function_call_output.
        * */
        AiResponse finalResponse =
                openAiClient.continueAnalysis( functionCall, toolResult, "test-model" );
        assertEquals( AiResponse.Type.TEXT, finalResponse.getType() );
        assertEquals( "Analyse terminée après exécution du tool", finalResponse.getText() );

        /* * Nous devons bien avoir effectué exactement deux appels HTTP. */
        assertEquals( 2, requestCount );

        /* * Vérifie que le deuxième appel contenait
        bien le function_call_output attendu. */

        assertSecondRequestContainsFunctionCallOutput(); }

    @Test
    void shouldRejectEmptyPrompt() {

        assertThrows(
                IllegalArgumentException.class,
                () -> openAiClient.generateAnalysis(
                        "",
                        "test-model"
                )
        );
    }

    @Test
    void shouldRejectEmptyModel() {

        assertThrows(
                IllegalArgumentException.class,
                () -> openAiClient.generateAnalysis(
                        "Analyse cet incident",
                        ""
                )
        );
    }

    private void handleRequest(HttpExchange exchange) throws IOException {

        requestCount++;

        receivedAuthorization =
                exchange.getRequestHeaders()
                        .getFirst("Authorization");

        String requestBody =
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

        receivedBodies.add(requestBody);

        byte[] responseBody = getBytesResponse();

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json"
                );

        exchange.sendResponseHeaders(
                200,
                responseBody.length
        );

        try (OutputStream outputStream =
                     exchange.getResponseBody()) {

            outputStream.write(responseBody);
        }
    }

    private void assertRequestContainsToolDefinition() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(receivedBodies.getFirst());
        assertEquals( "test-model", root.path("model").asText() );
        assertEquals( "Analyse cet incident", root.path("input").asText() );
        JsonNode tools = root.path("tools");
        assertEquals( 1, tools.size() );
        JsonNode tool = tools.get(0);
        assertEquals( "function", tool.path("type").asText() );
        assertEquals( "search_log", tool.path("name").asText() );
        assertEquals( "Recherche un texte dans le fichier de log.", tool.path("description").asText() );
        JsonNode parameters = tool.path("parameters");
        assertEquals( "object", parameters.path("type").asText() );
        JsonNode searchTerm = parameters .path("properties") .path("searchTerm");
        assertEquals( "string", searchTerm.path("type").asText() );
    }

    private void assertSecondRequestContainsFunctionCallOutput() throws Exception {
        JsonNode root = objectMapper.readTree(receivedBodies.get(1));
        assertEquals(
                "resp_123",
                root.path("previous_response_id").asText()
        );
        assertEquals( "test-model", root.path("model").asText() );
        JsonNode input = root.path("input");
        assertTrue( input.isArray() );
        assertEquals( 1, input.size() );
        JsonNode functionCallOutput = input.get(0);
        assertEquals( "function_call_output", functionCallOutput .path("type") .asText() );
        assertEquals( "call_123", functionCallOutput .path("call_id") .asText() );
        assertEquals( "Ligne 42 : Host name may not be null", functionCallOutput .path("output") .asText() );
    }

    private byte[] getBytesResponse() throws IOException {

        JsonNode request = objectMapper.readTree(receivedBodies.getLast());

        JsonNode input = request.path("input");

        /*
         * Deuxième appel :
         * input est un tableau contenant function_call_output.
         */
        if (input.isArray()) {

            return """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "Analyse terminée après exécution du tool"
                        }
                      ]
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
        }

        /*
         * Appel demandant explicitement une recherche.
         */
        if ("Recherche l'erreur dans le log"
                .equals(input.asText())) {

            return """
                {
                  "id": "resp_123",
                  "output": [
                    {
                      "type": "function_call",
                      "call_id": "call_123",
                      "name": "search_log",
                      "arguments": "{\\"searchTerm\\":\\"Host name may not be null\\"}"
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
        }

        /*
         * Réponse TEXT par défaut pour les autres tests.
         */
        return """
            {
              "output": [
                {
                  "type": "message",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "Analyse terminée"
                    }
                  ]
                }
              ]
            }
            """.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void shouldRejectFunctionCallWithoutResponseId() {
        AiResponse functionCall = new AiResponse();

        functionCall.setType(AiResponse.Type.FUNCTION_CALL);
        functionCall.setResponseId(null);
        functionCall.setCallId("call_123");
        functionCall.setFunctionName("search_log");
        functionCall.setArguments("{\"searchTerm\":\"test\"}");

        ToolExecutionResult toolResult =
                new ToolExecutionResult("call_123", "result");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> openAiClient.continueAnalysis(
                        functionCall,
                        toolResult,
                        "test-model"
                )
        );

        assertEquals(
                "functionCall.responseId must not be null or empty",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectFunctionCallWithoutCallId() {
        AiResponse functionCall = new AiResponse();

        functionCall.setType(AiResponse.Type.FUNCTION_CALL);
        functionCall.setResponseId("resp_123");
        functionCall.setCallId(null);
        functionCall.setFunctionName("search_log");
        functionCall.setArguments("{\"searchTerm\":\"test\"}");

        ToolExecutionResult toolResult =
                new ToolExecutionResult("call_123", "result");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> openAiClient.continueAnalysis(
                        functionCall,
                        toolResult,
                        "test-model"
                )
        );

        assertEquals(
                "functionCall.callId must not be null or empty",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectToolResultWithDifferentCallId() {

        AiResponse functionCall = new AiResponse();

        functionCall.setType(AiResponse.Type.FUNCTION_CALL);
        functionCall.setResponseId("resp_123");
        functionCall.setCallId("call_123");
        functionCall.setFunctionName("search_log");
        functionCall.setArguments("{\"searchTerm\":\"test\"}");

        ToolExecutionResult toolResult =
                new ToolExecutionResult(
                        "call_456",
                        "result"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> openAiClient.continueAnalysis(
                        functionCall,
                        toolResult,
                        "test-model"
                )
        );

        assertEquals(
                "functionCall.callId and toolResult.callId must match",
                exception.getMessage()
        );

        assertEquals(
                0,
                requestCount
        );
    }
}