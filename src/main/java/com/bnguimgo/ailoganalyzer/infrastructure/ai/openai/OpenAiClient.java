package com.bnguimgo.ailoganalyzer.infrastructure.ai.openai;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiFunctionCall;
import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchToolDefinition;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

            logger.info(
                    "OpenAI response body: {}",
                    response.body()
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
            List<ToolExecutionResult> toolResults,
            String model) {

        if (functionCall == null) {
            throw new IllegalArgumentException(
                    "functionCall must not be null"
            );
        }

        if (functionCall.getType()
                != AiResponse.Type.FUNCTION_CALL) {

            throw new IllegalArgumentException(
                    "functionCall must be of type FUNCTION_CALL"
            );
        }

        if (toolResults == null
                || toolResults.isEmpty()) {

            throw new IllegalArgumentException(
                    "toolResults must not be null or empty"
            );
        }

        if (functionCall.getResponseId() == null
                || functionCall.getResponseId().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "functionCall.responseId must not be null or empty"
            );
        }

        List<AiFunctionCall> functionCalls =
                functionCall.getFunctionCalls();

        if (functionCalls == null || functionCalls.isEmpty()) {
            throw new IllegalArgumentException(
                    "functionCall must contain at least one function call"
            );
        }

        for (AiFunctionCall aiFunctionCall : functionCalls) {

            if (aiFunctionCall == null) {
                throw new IllegalArgumentException(
                        "functionCall must not contain null elements"
                );
            }

            if (aiFunctionCall.getCallId() == null
                    || aiFunctionCall.getCallId().trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "functionCall.callId must not be null or empty"
                );
            }
        }

        for (ToolExecutionResult toolResult : toolResults) {

            if (toolResult == null) {
                throw new IllegalArgumentException(
                        "toolResults must not contain null elements"
                );
            }

            if (toolResult.callId() == null
                    || toolResult.callId().trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "toolResult callId must not be null or empty"
                );
            }

            boolean callIdMatches = functionCalls.stream()
                    .anyMatch(aiFunctionCall ->
                            aiFunctionCall.getCallId()
                                    .equals(toolResult.callId())
                    );

            if (!callIdMatches) {

                throw new IllegalArgumentException(
                        "functionCall.callId and toolResult.callId must match"
                );
            }
        }

        if (model == null || model.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "model must not be null or empty"
            );
        }

        ArrayNode input =
                objectMapper.createArrayNode();

        for (ToolExecutionResult toolResult : toolResults) {

            input.add(
                    objectMapper.createObjectNode()
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
            );
        }

        ObjectNode requestNode =
                objectMapper.createObjectNode();

        requestNode.put(
                "model",
                model
        );

        requestNode.put(
                "previous_response_id",
                functionCall.getResponseId()
        );

        requestNode.set(
                "input",
                input
        );

        requestNode.set(
                "tools",
                objectMapper.valueToTree(
                        List.of(
                                LogSearchToolDefinition.asMap()
                        )
                )
        );

        String requestBody = requestNode.toString();

        logger.info(
                "Calling OpenAI Responses API with tool outputs - " +
                        "toolCount={}, previousResponseId={}",
                toolResults.size(),
                functionCall.getResponseId()
        );

        try {

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
                    "OpenAI Responses API continuation returned HTTP {}",
                    response.statusCode()
            );

            logger.info(
                    "OpenAI continuation response body: {}",
                    response.body()
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

    private AiResponse extractResponse(String responseBody) throws IOException {

        logger.debug(
                "Parsing OpenAI Responses API response"
        );

        JsonNode root =
                objectMapper.readTree(responseBody);

        String responseId =
                root.path("id").asText(null);

        JsonNode output =
                root.path("output");

        List<AiFunctionCall> functionCalls = new ArrayList<>();

        for (JsonNode outputItem : output) {

            String type = outputItem.path("type").asText();

            if ("function_call".equals(type)) {

                AiFunctionCall functionCall = new AiFunctionCall();

                String callId =
                        outputItem
                                .path("call_id")
                                .asText();

                if (callId == null || callId.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "functionCall.callId must not be null or empty"
                    );
                }

                String functionName =
                        outputItem
                                .path("name")
                                .asText();

                String arguments =
                        outputItem
                                .path("arguments")
                                .asText();

                functionCall.setCallId(callId);

                functionCall.setFunctionName(
                        functionName
                );

                functionCall.setArguments(arguments);

                logger.info(
                        "OpenAI requested function '{}' - callId={}",
                        functionName,
                        callId
                );

                functionCalls.add(functionCall);
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

        if (!functionCalls.isEmpty()) {

            AiResponse response = new AiResponse();

            response.setType(AiResponse.Type.FUNCTION_CALL);

            response.setResponseId(responseId);

            response.setFunctionCalls(functionCalls);

            return response;
        }

        throw new IllegalStateException(
                "OpenAI response does not contain text or function call"
        );
    }
}