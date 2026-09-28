package com.bnguimgo.ailoganalyzer.infrastructure.ai.openai;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchToolDefinition;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OpenAiClient implements AiClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;

    public OpenAiClient(
            AiProviderProperties properties,
            ObjectMapper objectMapper) {

        this(
                properties.getApiKey(),
                properties.getApiUrl(),
                HttpClient.newHttpClient(),
                objectMapper
        );
    }

    public OpenAiClient(
            String apiKey,
            String apiUrl,
            HttpClient httpClient,
            ObjectMapper objectMapper) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "apiKey must not be null or empty"
            );
        }

        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "apiUrl must not be null or empty"
            );
        }

        if (httpClient == null) {
            throw new IllegalArgumentException(
                    "httpClient must not be null"
            );
        }

        if (objectMapper == null) {
            throw new IllegalArgumentException(
                    "objectMapper must not be null"
            );
        }

        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiResponse generateAnalysis(
            String prompt,
            String model) {

        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "prompt must not be null or empty"
            );
        }

        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "model must not be null or empty"
            );
        }

        logger.info(
                "Calling OpenAI Responses API - model={}",
                model
        );

        try {
            String requestBody =
                    objectMapper.createObjectNode()
                            .put("model", model)
                            .put("input", prompt)
                            .set(
                                    "tools",
                                    objectMapper.valueToTree(
                                            List.of(
                                                    LogSearchToolDefinition
                                                            .asMap()
                                            )
                                    )
                            )
                            .toString();

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(apiUrl))
                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            requestBody,
                                            StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            logger.info(
                    "OpenAI Responses API returned HTTP {}",
                    response.statusCode()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "OpenAI API returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            return extractResponse(response.body());

        } catch (IOException e) {

            logger.error(
                    "Unable to call OpenAI Responses API",
                    e
            );

            throw new IllegalStateException(
                    "Unable to call OpenAI API",
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            logger.error(
                    "OpenAI API call was interrupted",
                    e
            );

            throw new IllegalStateException(
                    "OpenAI API call was interrupted",
                    e
            );
        }
    }

    @Override
    public AiResponse continueAnalysis(
            AiResponse functionCall,
            ToolExecutionResult toolResult,
            String model) {

        if (functionCall == null) {
            throw new IllegalArgumentException(
                    "functionCall must not be null"
            );
        }

        if (functionCall.getType()
                != AiResponse.Type.FUNCTION_CALL) {

            throw new IllegalArgumentException(
                    "functionCall must be a FUNCTION_CALL"
            );
        }

        if (toolResult == null) {
            throw new IllegalArgumentException(
                    "toolResult must not be null"
            );
        }

        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "model must not be null or empty"
            );
        }

        logger.info(
                "Calling OpenAI Responses API with function_call_output - " +
                        "callId={}, model={}",
                toolResult.callId(),
                model
        );

        try {
            String requestBody =
                    objectMapper.createObjectNode()
                            .put("model", model)//model est un attribut propre de OpenAI, voir le endPoint v1/responses
                            .put("previous_response_id", functionCall.getResponseId())//previous_response_id est un attribut propre à l'API https://api.openai.com/v1/responses
                            .set(
                                    "input",
                                    objectMapper.createArrayNode()
                                            .add(
                                                    objectMapper
                                                            .createObjectNode()
                                                            .put(
                                                                    "type",
                                                                    "function_call_output"
                                                            )
                                                            .put(
                                                                    "call_id",
                                                                    toolResult.callId()
                                                            )
                                                            .put(
                                                                    "output",
                                                                    toolResult.output()
                                                            )
                                            )
                            )
                            .toString();

            logger.debug(
                    "Sending function_call_output for callId={}",
                    toolResult.callId()
            );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(apiUrl))
                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            requestBody,
                                            StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            logger.info(
                    "OpenAI Responses API second call returned HTTP {}",
                    response.statusCode()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                logger.error(
                        "OpenAI API returned HTTP {} during " +
                                "function_call_output",
                        response.statusCode()
                );

                throw new IllegalStateException(
                        "OpenAI API returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            return extractResponse(response.body());

        } catch (IOException e) {

            logger.error(
                    "Unable to call OpenAI Responses API with " +
                            "function_call_output",
                    e
            );

            throw new IllegalStateException(
                    "Unable to call OpenAI API",
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            logger.error(
                    "OpenAI second API call was interrupted",
                    e
            );

            throw new IllegalStateException(
                    "OpenAI API call was interrupted",
                    e
            );
        }
    }

    private AiResponse extractResponse(
            String responseBody) throws IOException {

        logger.debug(
                "Parsing OpenAI Responses API response"
        );

        JsonNode root =
                objectMapper.readTree(responseBody);

        String responseId =
                root.path("id").asText(null);

        JsonNode output =
                root.path("output");

        for (JsonNode outputItem : output) {

            String type =
                    outputItem.path("type").asText();

            if ("function_call".equals(type)) {

                String callId =
                        outputItem
                                .path("call_id")
                                .asText();

                String functionName =
                        outputItem
                                .path("name")
                                .asText();

                logger.info(
                        "OpenAI requested function '{}' - callId={}",
                        functionName,
                        callId
                );

                AiResponse response =
                        new AiResponse();

                response.setType(
                        AiResponse.Type.FUNCTION_CALL
                );

                response.setResponseId(responseId);

                response.setCallId(callId);

                response.setFunctionName(
                        functionName
                );

                response.setArguments(
                        outputItem
                                .path("arguments")
                                .asText()
                );

                return response;
            }

            if ("message".equals(type)) {

                JsonNode content =
                        outputItem.path("content");

                for (JsonNode contentItem : content) {

                    if ("output_text".equals(
                            contentItem
                                    .path("type")
                                    .asText())) {

                        logger.info(
                                "OpenAI returned final text response"
                        );

                        AiResponse response =
                                new AiResponse();

                        response.setType(
                                AiResponse.Type.TEXT
                        );

                        response.setText(
                                contentItem
                                        .path("text")
                                        .asText()
                        );

                        return response;
                    }
                }
            }
        }

        throw new IllegalStateException(
                "OpenAI response does not contain text or function call"
        );
    }
}