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

import static org.junit.jupiter.api.Assertions.*;

class OpenAiClientTest {

    private HttpServer server;
    private String receivedAuthorization;
    private String receivedBody;
    private int requestCount;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {

        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/v1/responses", this::handleRequest);

        server.start();

        requestCount = 0;
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldCallResponsesApiAndExtractText() throws Exception {

        OpenAiClient client =
                new OpenAiClient(
                        "test-api-key",
                        "http://localhost:"
                                + server.getAddress().getPort()
                                + "/v1/responses",
                        HttpClient.newHttpClient(),
                        objectMapper
                );

        AiResponse result =
                client.generateAnalysis(
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
        OpenAiClient client = new OpenAiClient( properties, new ObjectMapper() );
        AiResponse result = client.generateAnalysis( "Analyse cet incident", properties.getModel() );
        assertEquals( AiResponse.Type.TEXT, result.getType() );
        assertEquals( "Analyse terminée", result.getText() );
        assertEquals( "Bearer test-api-key", receivedAuthorization );
        assertRequestContainsToolDefinition();
    }

    @Test void shouldExecuteTwoStepFunctionCallFlow() throws Exception {
        OpenAiClient client = new OpenAiClient(
                "test-api-key",
                "http://localhost:" + server.getAddress().getPort() + "/v1/responses",
                HttpClient.newHttpClient(),
                objectMapper );

        /* * Premier appel :
        * utilisateur → OpenAI
        * OpenAI répond avec un function_call.
        */
        AiResponse functionCall = client.generateAnalysis( "Recherche l'erreur dans le log", "test-model" );
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
                client.continueAnalysis( functionCall, toolResult, "test-model" );
        assertEquals( AiResponse.Type.TEXT, finalResponse.getType() );
        assertEquals( "Analyse terminée après exécution du tool", finalResponse.getText() );

        /* * Nous devons bien avoir effectué exactement deux appels HTTP. */
        assertEquals( 2, requestCount );

        /* * Vérifie que le deuxième appel contenait
        bien le function_call_output attendu. */

        assertSecondRequestContainsFunctionCallOutput(); }

    @Test
    void shouldRejectEmptyPrompt() {

        OpenAiClient client =
                new OpenAiClient(
                        "test-api-key",
                        "http://localhost",
                        HttpClient.newHttpClient(),
                        objectMapper
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.generateAnalysis(
                        "",
                        "test-model"
                )
        );
    }

    @Test
    void shouldRejectEmptyModel() {

        OpenAiClient client =
                new OpenAiClient(
                        "test-api-key",
                        "http://localhost",
                        HttpClient.newHttpClient(),
                        objectMapper
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.generateAnalysis(
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

        receivedBody =
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

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
        JsonNode root = objectMapper.readTree( receivedBody );
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
        JsonNode root = objectMapper.readTree( receivedBody );
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

        JsonNode request =
                objectMapper.readTree(receivedBody);

        JsonNode input =
                request.path("input");

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

}