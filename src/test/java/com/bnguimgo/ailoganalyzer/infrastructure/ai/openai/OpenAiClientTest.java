package com.bnguimgo.ailoganalyzer.infrastructure.ai.openai;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiFunctionCall;
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
        AiFunctionCall aiFunctionCall = functionCall.getFunctionCalls().getFirst();
        assertEquals( "call_123", aiFunctionCall.getCallId() );
        assertEquals( "search_log", aiFunctionCall.getFunctionName() );
        assertEquals( "{\"searchTerm\":\"Host name may not be null\"}", aiFunctionCall.getArguments() );

        //Résultat produit par notre ToolExecutor.
        ToolExecutionResult toolResult =
                new ToolExecutionResult( aiFunctionCall.getCallId(), "Ligne 42 : Host name may not be null" );

        /* * Deuxième appel :
        *ToolExecutor → OpenAI * * avec function_call_output.
        * */
        AiResponse finalResponse =
                openAiClient.continueAnalysis( functionCall, List.of(toolResult), "test-model" );
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

        JsonNode root = objectMapper.readTree(receivedBodies.getFirst());
        assertEquals( "test-model", root.path("model").asText() );
        assertEquals( "Analyse cet incident", root.path("input").asText() );
        JsonNode tools = root.path("tools");
        assertEquals( 1, tools.size() );
        JsonNode tool = tools.get(0);
        assertEquals( "function", tool.path("type").asText() );
        assertEquals( "search_log", tool.path("name").asText() );

        String expectedDescription = "Recherche un texte dans le fichier de log. "
                + "Utilise cet outil lorsque tu dois vérifier la présence "
                + "d'un message, d'une exception ou d'un autre élément "
                + "dans le log afin d'obtenir des éléments de preuve.";
        assertEquals( expectedDescription, tool.path("description").asText() );
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

            String firstInput = objectMapper
                    .readTree(receivedBodies.getFirst())
                    .path("input")
                    .asText();

            if ("Recherche plusieurs erreurs dans le log".equals(firstInput)
                    && requestCount == 2) {

                return """
            {
              "id": "resp_2",
              "output": [
                {
                  "type": "function_call",
                  "call_id": "call_2",
                  "name": "search_log",
                  "arguments": "{\\"searchTerm\\":\\"CustomHttpResponseException\\"}"
                }
              ]
            }
            """.getBytes(StandardCharsets.UTF_8);
            }

            if ("Recherche plusieurs erreurs dans le log".equals(firstInput)
                    && requestCount == 3) {

                return """
            {
              "id": "resp_3",
              "output": [
                {
                  "type": "message",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "Analyse terminée après deux tool calls"
                    }
                  ]
                }
              ]
            }
            """.getBytes(StandardCharsets.UTF_8);
            }

            // Ancien scénario : deuxième appel → TEXT
            // Scénario : deux function calls dans une même réponse
        if ("Recherche deux erreurs dans le log".equals(firstInput)
            && requestCount == 2) {

        return """
        {
          "output": [
            {
              "type": "message",
              "content": [
                {
                  "type": "output_text",
                  "text": "Analyse terminée après deux function calls"
                }
              ]
            }
          ]
        }
        """.getBytes(StandardCharsets.UTF_8);
                    }

        // Ancien scénario : un function call → résultat du tool → TEXT
        if (requestCount == 2) {

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
                }

        /*
         * Appel demandant explicitement une recherche.
         */
        if ("Recherche l'erreur dans le log".equals(input.asText())) {

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

        if ("Recherche plusieurs erreurs dans le log".equals(input.asText())) {

            return """
        {
          "id": "resp_1",
          "output": [
            {
              "type": "function_call",
              "call_id": "call_1",
              "name": "search_log",
              "arguments": "{\\"searchTerm\\":\\"Host name may not be null\\"}"
            }
          ]
        }
        """.getBytes(StandardCharsets.UTF_8);
        }

        if ("Recherche deux erreurs dans le log".equals(input.asText())) {

            return """
        {
          "id": "resp_multi",
          "output": [
            {
              "type": "function_call",
              "call_id": "call_123",
              "name": "search_log",
              "arguments": "{\\"searchTerm\\":\\"Host name may not be null\\"}"
            },
            {
              "type": "function_call",
              "call_id": "call_456",
              "name": "search_log",
              "arguments": "{\\"searchTerm\\":\\"CustomHttpResponseException\\"}"
            }
          ]
        }
        """.getBytes(StandardCharsets.UTF_8);
        }

        if ("Réponse avec message puis function calls".equals(input.asText())) {

            return """
        {
          "id": "resp_message_then_calls",
          "output": [
            {
              "type": "message",
              "content": [
                {
                  "type": "output_text",
                  "text": "Je vais effectuer des recherches dans les logs."
                }
              ]
            },
            {
              "type": "function_call",
              "call_id": "call_1",
              "name": "search_log",
              "arguments": "{\\"searchTerm\\":\\"CustomHttpResponseErrorHandler\\"}"
            },
            {
              "type": "function_call",
              "call_id": "call_2",
              "name": "search_log",
              "arguments": "{\\"searchTerm\\":\\"httpStatus=400 BAD_REQUEST\\"}"
            },
            {
              "type": "function_call",
              "call_id": "call_3",
              "name": "search_log",
              "arguments": "{\\"searchTerm\\":\\"Cognito\\"}"
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
                        List.of(toolResult),
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
        AiFunctionCall aiFunctionCall = new AiFunctionCall();
        aiFunctionCall.setCallId(null);
        aiFunctionCall.setFunctionName("search_log");
        aiFunctionCall.setArguments("{\"searchTerm\":\"test\"}");
        functionCall.setFunctionCalls(List.of(aiFunctionCall));

        ToolExecutionResult toolResult =
                new ToolExecutionResult("call_123", "result");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> openAiClient.continueAnalysis(
                        functionCall,
                        List.of(toolResult),
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
        AiFunctionCall aiFunctionCall = new AiFunctionCall();
        aiFunctionCall.setCallId("call_123");
        aiFunctionCall.setFunctionName("search_log");
        aiFunctionCall.setArguments("{\"searchTerm\":\"test\"}");

        functionCall.setFunctionCalls(List.of(aiFunctionCall));

        ToolExecutionResult toolResult =
                new ToolExecutionResult(
                        "call_456",
                        "result"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> openAiClient.continueAnalysis(
                        functionCall,
                        List.of(toolResult),
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

    @Test
    void shouldExecuteMultipleFunctionCallFlow() throws Exception {

        /*
         * Premier appel :
         * application → OpenAI
         * OpenAI → FUNCTION_CALL call_1
         */
        AiResponse firstFunctionCall =
                openAiClient.generateAnalysis(
                        "Recherche plusieurs erreurs dans le log",
                        "test-model"
                );

        assertEquals(
                AiResponse.Type.FUNCTION_CALL,
                firstFunctionCall.getType()
        );

        assertEquals(
                "resp_1",
                firstFunctionCall.getResponseId()
        );

        AiFunctionCall aiFunctionCall = firstFunctionCall.getFunctionCalls().getFirst();
        assertEquals(
                "call_1",
                aiFunctionCall.getCallId()
        );

        assertEquals(
                "search_log",
                aiFunctionCall.getFunctionName()
        );

        /*
         * Résultat du premier tool.
         */
        ToolExecutionResult firstToolResult =
                new ToolExecutionResult(
                        aiFunctionCall.getCallId(),
                        "Résultat recherche erreur 1"
                );

        /*
         * Deuxième appel :
         * application → OpenAI
         * OpenAI → FUNCTION_CALL call_2
         */
        AiResponse secondFunctionCall =
                openAiClient.continueAnalysis(
                        firstFunctionCall,
                        List.of(firstToolResult),
                        "test-model"
                );

        assertEquals(
                AiResponse.Type.FUNCTION_CALL,
                secondFunctionCall.getType()
        );

        assertEquals(
                "resp_2",
                secondFunctionCall.getResponseId()
        );

        AiFunctionCall aiFunctionCall_2 = secondFunctionCall.getFunctionCalls().getFirst();
        assertEquals(
                "call_2",
                aiFunctionCall_2.getCallId()
        );

        assertEquals(
                "search_log",
                aiFunctionCall_2.getFunctionName()
        );

        /*
         * Résultat du deuxième tool.
         */
        ToolExecutionResult secondToolResult =
                new ToolExecutionResult(
                        aiFunctionCall_2.getCallId(),
                        "Résultat recherche erreur 2"
                );

        /*
         * Troisième appel :
         * application → OpenAI
         * OpenAI → TEXT
         */
        AiResponse finalResponse =
                openAiClient.continueAnalysis(
                        secondFunctionCall,
                        List.of(secondToolResult),
                        "test-model"
                );

        assertEquals(
                AiResponse.Type.TEXT,
                finalResponse.getType()
        );

        assertEquals(
                "Analyse terminée après deux tool calls",
                finalResponse.getText()
        );

        /*
         * Trois appels HTTP au total :
         *
         * 1. generateAnalysis()
         * 2. continueAnalysis(call_1)
         * 3. continueAnalysis(call_2)
         */
        assertEquals(
                3,
                requestCount
        );

        /*
         * Vérification du deuxième appel HTTP :
         * function_call_output du call_1
         */
        JsonNode secondRequest =
                objectMapper.readTree(
                        receivedBodies.get(1)
                );

        JsonNode secondTools = secondRequest.path("tools");

        assertEquals(
                1,
                secondTools.size()
        );

        assertEquals(
                "function",
                secondTools.get(0).path("type").asText()
        );

        assertEquals(
                "search_log",
                secondTools.get(0).path("name").asText()
        );

        assertEquals(
                "resp_1",
                secondRequest
                        .path("previous_response_id")
                        .asText()
        );

        JsonNode secondInput =
                secondRequest.path("input");

        assertEquals(
                "function_call_output",
                secondInput
                        .get(0)
                        .path("type")
                        .asText()
        );

        assertEquals(
                "call_1",
                secondInput
                        .get(0)
                        .path("call_id")
                        .asText()
        );

        /*
         * Vérification du troisième appel HTTP :
         * function_call_output du call_2
         */
        JsonNode thirdRequest =
                objectMapper.readTree(
                        receivedBodies.get(2)
                );

        JsonNode thirdTools = thirdRequest.path("tools");

        assertEquals(
                1,
                thirdTools.size()
        );

        assertEquals(
                "function",
                thirdTools.get(0).path("type").asText()
        );

        assertEquals(
                "search_log",
                thirdTools.get(0).path("name").asText()
        );

        assertEquals(
                "resp_2",
                thirdRequest
                        .path("previous_response_id")
                        .asText()
        );

        JsonNode thirdInput =
                thirdRequest.path("input");

        assertEquals(
                "function_call_output",
                thirdInput
                        .get(0)
                        .path("type")
                        .asText()
        );

        assertEquals(
                "call_2",
                thirdInput
                        .get(0)
                        .path("call_id")
                        .asText()
        );
    }

    @Test
    void shouldExtractFunctionCallsWhenMessageAppearsBeforeFunctionCalls() {

        /*
         * OpenAI peut retourner un message avant les function_call.
         *
         * Le parser doit parcourir l'ensemble du tableau "output"
         * avant de déterminer le type de réponse.
         */
        AiResponse response =
                openAiClient.generateAnalysis(
                        "Réponse avec message puis function calls",
                        "test-model"
                );

        assertEquals(
                AiResponse.Type.FUNCTION_CALL,
                response.getType()
        );

        assertEquals(
                "resp_message_then_calls",
                response.getResponseId()
        );

        assertEquals(
                3,
                response.getFunctionCalls().size()
        );

        AiFunctionCall firstCall =
                response.getFunctionCalls().getFirst();

        assertEquals(
                "call_1",
                firstCall.getCallId()
        );

        assertEquals(
                "search_log",
                firstCall.getFunctionName()
        );

        assertEquals(
                "{\"searchTerm\":\"CustomHttpResponseErrorHandler\"}",
                firstCall.getArguments()
        );

        AiFunctionCall secondCall =
                response.getFunctionCalls().get(1);

        assertEquals(
                "call_2",
                secondCall.getCallId()
        );

        assertEquals(
                "{\"searchTerm\":\"httpStatus=400 BAD_REQUEST\"}",
                secondCall.getArguments()
        );

        AiFunctionCall thirdCall =
                response.getFunctionCalls().get(2);

        assertEquals(
                "call_3",
                thirdCall.getCallId()
        );

        assertEquals(
                "{\"searchTerm\":\"Cognito\"}",
                thirdCall.getArguments()
        );
    }

    @Test
    void shouldExecuteMultipleFunctionCallsInSameResponse() throws Exception {

        /*
         * Premier appel :
         * application → OpenAI
         * OpenAI → deux FUNCTION_CALL
         */
        AiResponse functionCall =
                openAiClient.generateAnalysis(
                        "Recherche deux erreurs dans le log",
                        "test-model"
                );

        assertEquals(
                AiResponse.Type.FUNCTION_CALL,
                functionCall.getType()
        );

        assertEquals(
                "resp_multi",
                functionCall.getResponseId()
        );

        assertEquals(
                2,
                functionCall.getFunctionCalls().size()
        );

        AiFunctionCall firstFunctionCall =
                functionCall.getFunctionCalls().get(0);

        AiFunctionCall secondFunctionCall =
                functionCall.getFunctionCalls().get(1);

        assertEquals(
                "call_123",
                firstFunctionCall.getCallId()
        );

        assertEquals(
                "call_456",
                secondFunctionCall.getCallId()
        );

        /*
         * Résultats produits par les deux tools.
         */
        ToolExecutionResult firstToolResult =
                new ToolExecutionResult(
                        "call_123",
                        "Résultat recherche erreur 1"
                );

        ToolExecutionResult secondToolResult =
                new ToolExecutionResult(
                        "call_456",
                        "Résultat recherche erreur 2"
                );

        /*
         * Deuxième appel :
         * application → OpenAI
         *
         * avec DEUX function_call_output.
         */
        AiResponse finalResponse =
                openAiClient.continueAnalysis(
                        functionCall,
                        List.of(
                                firstToolResult,
                                secondToolResult
                        ),
                        "test-model"
                );

        assertEquals(
                AiResponse.Type.TEXT,
                finalResponse.getType()
        );

        assertEquals(
                "Analyse terminée après deux function calls",
                finalResponse.getText()
        );

        /*
         * Deux appels HTTP au total.
         */
        assertEquals(
                2,
                requestCount
        );

        /*
         * Vérification du deuxième appel HTTP.
         */
        JsonNode secondRequest =
                objectMapper.readTree(
                        receivedBodies.get(1)
                );

        JsonNode tools =
                secondRequest.path("tools");

        assertEquals(
                1,
                tools.size()
        );

        assertEquals(
                "function",
                tools.get(0).path("type").asText()
        );

        assertEquals(
                "search_log",
                tools.get(0).path("name").asText()
        );

        assertEquals(
                "resp_multi",
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
         * Premier function_call_output.
         */
        JsonNode firstOutput = input.get(0);

        assertEquals(
                "function_call_output",
                firstOutput.path("type").asText()
        );

        assertEquals(
                "call_123",
                firstOutput.path("call_id").asText()
        );

        assertEquals(
                "Résultat recherche erreur 1",
                firstOutput.path("output").asText()
        );

        /*
         * Deuxième function_call_output.
         */
        JsonNode secondOutput = input.get(1);

        assertEquals(
                "function_call_output",
                secondOutput.path("type").asText()
        );

        assertEquals(
                "call_456",
                secondOutput.path("call_id").asText()
        );

        assertEquals(
                "Résultat recherche erreur 2",
                secondOutput.path("output").asText()
        );
    }

    @Test
    void shouldDisableParallelToolCallsWhenGenerateAnalysis() throws Exception {

        openAiClient.generateAnalysis(
                "Analyse ce log",
                "test-model"
        );

        String requestBody = receivedBodies.getFirst();

        JsonNode request = objectMapper.readTree(requestBody);

        assertTrue(request.has("parallel_tool_calls"));
        assertFalse(request.get("parallel_tool_calls").asBoolean());
    }

    @Test
    void shouldDisableParallelToolCallsWhenContinueAnalysis() throws Exception {

        AiResponse functionCall = openAiClient.generateAnalysis(
                "Recherche l'erreur dans le log",
                "test-model"
        );

        ToolExecutionResult toolResult =
                new ToolExecutionResult(
                        functionCall.getFunctionCalls()
                                .getFirst()
                                .getCallId(),
                        "Résultat de recherche"
                );

        openAiClient.continueAnalysis(
                functionCall,
                List.of(toolResult),
                "test-model"
        );

        String requestBody = receivedBodies.getLast();

        JsonNode request = objectMapper.readTree(requestBody);

        assertTrue(request.has("parallel_tool_calls"));
        assertFalse(
                request.get("parallel_tool_calls").asBoolean()
        );
    }
}

