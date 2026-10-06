package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import com.bnguimgo.ailoganalyzer.domain.ai.AiFunctionCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ToolExecutor {

    private static final Logger logger =
            LoggerFactory.getLogger(ToolExecutor.class);

    private final LogSearchTool logSearchTool;
    private final ObjectMapper objectMapper;

    public ToolExecutor(
            LogSearchTool logSearchTool,
            ObjectMapper objectMapper) {

        if (logSearchTool == null) {
            throw new IllegalArgumentException(
                    "logSearchTool must not be null"
            );
        }

        if (objectMapper == null) {
            throw new IllegalArgumentException(
                    "objectMapper must not be null"
            );
        }

        this.logSearchTool = logSearchTool;
        this.objectMapper = objectMapper;
    }

    public ToolExecutionResult execute(
            AiFunctionCall functionCall,
            Path logFile) throws IOException {

        if (logFile == null) {
            throw new IllegalArgumentException(
                    "logFile must not be null"
            );
        }

        if (functionCall == null) {
            throw new IllegalArgumentException(
                    "functionCall must not be null"
            );
        }

        if (!"search_log".equals(
                functionCall.getFunctionName())) {

            throw new IllegalArgumentException(
                    "Unsupported function: "
                            + functionCall.getFunctionName()
            );
        }

        logger.info(
                "Executing tool '{}' - callId={}",
                functionCall.getFunctionName(),
                functionCall.getCallId()
        );

        JsonNode arguments =
                objectMapper.readTree(
                        functionCall.getArguments()
                );

        String searchTerm =
                arguments
                        .path("searchTerm")
                        .asText(null);

        if (searchTerm == null
                || searchTerm.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "searchTerm must not be null or empty"
            );
        }

        logger.debug(
                "Executing search_log - searchTerm='{}', logFile='{}'",
                searchTerm,
                logFile
        );

        List<String> results =
                logSearchTool.search(
                        logFile,
                        searchTerm
                );

        String output =
                String.join(
                        System.lineSeparator(),
                        results
                );

        logger.info(
                "Tool '{}' completed - callId={}, resultCount={}",
                functionCall.getFunctionName(),
                functionCall.getCallId(),
                results.size()
        );

        return new ToolExecutionResult(
                functionCall.getCallId(),
                output
        );
    }
}