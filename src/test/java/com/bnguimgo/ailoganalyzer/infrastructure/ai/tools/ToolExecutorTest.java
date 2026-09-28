package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import com.bnguimgo.ailoganalyzer.domain.ai.AiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolExecutorTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldExecuteSearchLogFunctionCall()
            throws Exception {

        LogSearchTool logSearchTool =
                new DefaultLogSearchTool();

        ToolExecutor executor =
                new ToolExecutor(
                        logSearchTool,
                        new ObjectMapper()
                );

        AiResponse aiResponse =
                new AiResponse();

        aiResponse.setType(
                AiResponse.Type.FUNCTION_CALL
        );

        aiResponse.setCallId(
                "call_123"
        );

        aiResponse.setFunctionName(
                "search_log"
        );

        aiResponse.setArguments(
                "{\"searchTerm\":\"Host name may not be null\"}"
        );

        ToolExecutionResult result =
                executor.execute(
                        aiResponse,
                        LOG_FILE_PATH
                );

        assertEquals(
                "call_123",
                result.callId()
        );

        assertEquals(
                7,
                result.output()
                        .lines()
                        .count()
        );
    }

    @Test
    void shouldRejectNullAiResponse() {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(null, LOG_FILE_PATH)
        );
    }

    @Test
    void shouldRejectTextResponse() {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiResponse aiResponse =
                new AiResponse();

        aiResponse.setType(
                AiResponse.Type.TEXT
        );

        aiResponse.setText(
                "Analyse terminée"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(aiResponse, LOG_FILE_PATH)
        );
    }

    @Test
    void shouldRejectUnsupportedFunction() {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiResponse aiResponse =
                new AiResponse();

        aiResponse.setType(
                AiResponse.Type.FUNCTION_CALL
        );

        aiResponse.setFunctionName(
                "unknown_tool"
        );

        aiResponse.setArguments(
                "{}"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(aiResponse, LOG_FILE_PATH)
        );
    }

    @Test
    void shouldRejectEmptySearchTerm() {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiResponse aiResponse =
                new AiResponse();

        aiResponse.setType(
                AiResponse.Type.FUNCTION_CALL
        );

        aiResponse.setFunctionName(
                "search_log"
        );

        aiResponse.setArguments(
                "{\"searchTerm\":\"\"}"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(aiResponse, LOG_FILE_PATH)
        );
    }

    @Test
    void shouldRejectInvalidArgumentsJson() {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiResponse aiResponse =
                new AiResponse();

        aiResponse.setType(
                AiResponse.Type.FUNCTION_CALL
        );

        aiResponse.setFunctionName(
                "search_log"
        );

        aiResponse.setArguments(
                "INVALID_JSON"
        );

        assertThrows(
                Exception.class,
                () -> executor.execute(aiResponse, LOG_FILE_PATH)
        );
    }

    @Test
    void shouldPreserveFunctionCallId() throws Exception {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiResponse aiResponse =
                new AiResponse();

        aiResponse.setType(
                AiResponse.Type.FUNCTION_CALL
        );

        aiResponse.setCallId(
                "call_456"
        );

        aiResponse.setFunctionName(
                "search_log"
        );

        aiResponse.setArguments(
                "{\"searchTerm\":\"Host name may not be null\"}"
        );

        ToolExecutionResult result =
                executor.execute(aiResponse, LOG_FILE_PATH);

        assertEquals(
                "call_456",
                result.callId()
        );
    }

    @Test
    void shouldRejectMissingCallId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolExecutionResult(
                        null,
                        "résultat"
                )
        );
    }

    @Test
    void shouldRejectMissingSearchTerm() {
        ToolExecutor executor = new ToolExecutor(
                new DefaultLogSearchTool(),
                new ObjectMapper() );

        AiResponse aiResponse = new AiResponse();
        aiResponse.setType( AiResponse.Type.FUNCTION_CALL );
        aiResponse.setFunctionName( "search_log" );
        aiResponse.setArguments( "{}" );

        assertThrows( IllegalArgumentException.class,
                () -> executor.execute( aiResponse, LOG_FILE_PATH ) );

    }

    @Test void shouldRejectBlankSearchTerm() {
        ToolExecutor executor = new ToolExecutor(
                new DefaultLogSearchTool(),
                new ObjectMapper() );

        AiResponse aiResponse = new AiResponse();
        aiResponse.setType( AiResponse.Type.FUNCTION_CALL );
        aiResponse.setFunctionName( "search_log" );
        aiResponse.setArguments( "{\"searchTerm\":\" \"}" );
        assertThrows( IllegalArgumentException.class,
                () -> executor.execute( aiResponse, LOG_FILE_PATH ));
    }
}