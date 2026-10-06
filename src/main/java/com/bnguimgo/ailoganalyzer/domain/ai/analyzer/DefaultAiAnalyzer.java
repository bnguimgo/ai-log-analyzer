package com.bnguimgo.ailoganalyzer.domain.ai.analyzer;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.*;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultAiAnalyzer implements AiAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAiAnalyzer.class);
    private static final int MAX_TOOL_CALLS = 10;
    private final AiClient aiClient;
    private final AiPromptBuilder aiPromptBuilder;
    private final AiProviderProperties properties;
    private final ToolExecutor toolExecutor;

    public DefaultAiAnalyzer(
            AiClient aiClient,
            AiPromptBuilder aiPromptBuilder,
            AiProviderProperties properties,
            ToolExecutor toolExecutor) {

        this.aiClient = aiClient;
        this.aiPromptBuilder = aiPromptBuilder;
        this.properties = properties;
        this.toolExecutor = toolExecutor;
    }

    @Override
    public AiAnalysisResponse analyze(
            StructuredContext context) throws IOException {

        if (context == null) {
            throw new IllegalArgumentException(
                    "context must not be null"
            );
        }

        logger.info(
                "Starting AI log analysis - logFile={}",
                context.logFile()
        );

        String prompt =
                aiPromptBuilder.build(context);

        logger.debug("Calling AI for initial analysis");

        AiResponse aiResponse =
                aiClient.generateAnalysis(
                        prompt,
                        properties.getModel()
                );

        int executedToolCalls = 0;

        while (aiResponse.getType() == AiResponse.Type.FUNCTION_CALL) {

            List<AiFunctionCall> functionCalls =
                    aiResponse.getFunctionCalls();

            if (functionCalls == null || functionCalls.isEmpty()) {
                throw new IllegalStateException(
                        "AI function call response contains no function calls"
                );
            }

            if (executedToolCalls + functionCalls.size() > MAX_TOOL_CALLS) {
                throw new IllegalStateException(
                        "Maximum number of AI tool calls exceeded"
                );
            }

            List<ToolExecutionResult> toolResults =
                    executeToolCalls(
                            aiResponse,
                            context.logFile()
                    );

            executedToolCalls += toolResults.size();

            logger.info(
                    "Calling AI for continuation after tool executions - " +
                            "toolCount={}, totalToolCalls={}, model={}",
                    toolResults.size(),
                    executedToolCalls,
                    properties.getModel()
            );

            aiResponse =
                    aiClient.continueAnalysis(
                            aiResponse,
                            toolResults,
                            properties.getModel()
                    );

            logger.info(
                    "AI continuation response received - " +
                            "toolCount={}, totalToolCalls={}, responseType={}",
                    toolResults.size(),
                    executedToolCalls,
                    aiResponse.getType()
            );
        }

        return createResponse(aiResponse.getText());
    }

    private List<ToolExecutionResult> executeToolCalls(
            AiResponse aiResponse,
            Path logFile) {

        if (aiResponse.getResponseId() == null
                || aiResponse.getResponseId().trim().isEmpty()) {

            throw new IllegalStateException(
                    "AI function call response is missing responseId"
            );
        }

        List<AiFunctionCall> functionCalls =
                aiResponse.getFunctionCalls();

        if (functionCalls == null
                || functionCalls.isEmpty()) {

            throw new IllegalStateException(
                    "AI function call response contains no function calls"
            );
        }

        List<ToolExecutionResult> toolResults = new ArrayList<>();

        for (AiFunctionCall functionCall : functionCalls) {

            validateFunctionCall(functionCall);

            logger.info(
                    "AI requested tool execution - function={}, callId={}",
                    functionCall.getFunctionName(),
                    functionCall.getCallId()
            );

            try {

                logger.info(
                        "Executing tool '{}' - callId={}",
                        functionCall.getFunctionName(),
                        functionCall.getCallId()
                );

                ToolExecutionResult toolResult =
                        toolExecutor.execute(
                                functionCall,
                                logFile
                        );

                toolResults.add(toolResult);

                logger.info(
                        "Tool execution completed - function={}, callId={}",
                        functionCall.getFunctionName(),
                        functionCall.getCallId()
                );

            } catch (IOException e) {

                logger.error(
                        "Tool execution failed - function={}, callId={}",
                        functionCall.getFunctionName(),
                        functionCall.getCallId(),
                        e
                );

                throw new IllegalStateException(
                        "Unable to execute AI tool",
                        e
                );
            }
        }

        return toolResults;
    }

    private AiAnalysisResponse createResponse(
            String summary) {

        AiAnalysisResponse response =
                new AiAnalysisResponse();

        response.setSummary(summary);

        response.setProbableCauses(
                Collections.emptyList()
        );

        response.setRecommendations(
                Collections.emptyList()
        );

        response.setUncertainties(
                Collections.emptyList()
        );

        return response;
    }

    private void validateFunctionCall(AiFunctionCall functionCall) {

        if (functionCall == null) {
            throw new IllegalStateException(
                    "AI function call must not be null"
            );
        }

        if (functionCall.getCallId() == null
                || functionCall.getCallId().trim().isEmpty()) {

            throw new IllegalStateException(
                    "AI function call is missing callId"
            );
        }

        if (functionCall.getFunctionName() == null
                || functionCall.getFunctionName().trim().isEmpty()) {

            throw new IllegalStateException(
                    "AI function call is missing functionName"
            );
        }

        if (functionCall.getArguments() == null
                || functionCall.getArguments().trim().isEmpty()) {

            throw new IllegalStateException(
                    "AI function call is missing arguments"
            );
        }
    }
}