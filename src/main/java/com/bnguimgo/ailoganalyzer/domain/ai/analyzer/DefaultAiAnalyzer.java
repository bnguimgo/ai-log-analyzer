package com.bnguimgo.ailoganalyzer.domain.ai.analyzer;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.AiPromptBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

public class DefaultAiAnalyzer implements AiAnalyzer {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultAiAnalyzer.class);

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
            StructuredContext context) {

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

        //Premier appel de l'IA
        AiResponse aiResponse =
                aiClient.generateAnalysis(
                        prompt,
                        properties.getModel()
                );

        if (aiResponse.getType() == AiResponse.Type.TEXT) {

            logger.info(
                    "AI returned final response without tool execution"
            );

            return createResponse(
                    aiResponse.getText()
            );
        }

        if (aiResponse.getType() != AiResponse.Type.FUNCTION_CALL) {
            throw new IllegalStateException(
                    "Unsupported AI response type: "
                            + aiResponse.getType()
            );
        }

        validateFunctionCall(aiResponse);

        logger.info(
                "AI requested tool execution - function={}, callId={}",
                aiResponse.getFunctionName(),
                aiResponse.getCallId()
        );

        ToolExecutionResult toolResult;

        try {

            logger.info(
                    "Executing tool '{}' - callId={}",
                    aiResponse.getFunctionName(),
                    aiResponse.getCallId()
            );

            toolResult =
                    toolExecutor.execute(
                            aiResponse,
                            context.logFile()
                    );

        } catch (IOException e) {

            logger.error(
                    "Tool execution failed - function={}, callId={}",
                    aiResponse.getFunctionName(),
                    aiResponse.getCallId(),
                    e
            );

            throw new IllegalStateException(
                    "Unable to execute AI tool",
                    e
            );
        }

        logger.info(
                "Tool execution completed - function={}, callId={}",
                aiResponse.getFunctionName(),
                aiResponse.getCallId()
        );

        /*
         * Deuxième appel à l'IA :
         * on transmet le résultat du tool pour permettre
         * à l'IA de poursuivre son analyse.
         */
        logger.info(
                "Calling AI for final analysis after tool execution - " +
                        "callId={}, model={}",
                toolResult.callId(),
                properties.getModel()
        );

        AiResponse finalResponse =
                aiClient.continueAnalysis(
                        aiResponse,
                        toolResult,
                        properties.getModel()
                );

        logger.info(
                "AI final response received after tool execution - " +
                        "callId={}, responseType={}",
                toolResult.callId(),
                finalResponse.getType()
        );

        if (finalResponse.getType()
                != AiResponse.Type.TEXT) {

            throw new IllegalStateException(
                    "AI did not return a final text response"
            );
        }

        return createResponse(
                finalResponse.getText()
        );
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

    private void validateFunctionCall(AiResponse aiResponse) {

        if (aiResponse.getResponseId() == null
                || aiResponse.getResponseId().trim().isEmpty()) {

            throw new IllegalStateException(
                    "AI function call is missing responseId"
            );
        }

        if (aiResponse.getCallId() == null
                || aiResponse.getCallId().trim().isEmpty()) {

            throw new IllegalStateException(
                    "AI function call is missing callId"
            );
        }

        if (aiResponse.getFunctionName() == null
                || aiResponse.getFunctionName().trim().isEmpty()) {

            throw new IllegalStateException(
                    "AI function call is missing functionName"
            );
        }

        if (aiResponse.getArguments() == null
                || aiResponse.getArguments().trim().isEmpty()) {

            throw new IllegalStateException(
                    "AI function call is missing arguments"
            );
        }
    }
}