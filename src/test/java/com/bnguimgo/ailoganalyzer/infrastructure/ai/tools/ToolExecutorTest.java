package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import com.bnguimgo.ailoganalyzer.domain.ai.AiFunctionCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolExecutorTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");
    private static final ToolExecutor toolExecutor = new ToolExecutor(
            new DefaultLogSearchTool(),
            new ObjectMapper() );

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

        AiFunctionCall functionCall =
                new AiFunctionCall();

        functionCall.setCallId(
                "call_123"
        );

        functionCall.setFunctionName(
                "search_log"
        );

        functionCall.setArguments(
                "{\"searchTerm\":\"Host name may not be null\"}"
        );

        ToolExecutionResult result =
                executor.execute(
                        functionCall,
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
    void shouldRejectUnsupportedFunction() {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiFunctionCall functionCall =
                new AiFunctionCall();

        functionCall.setFunctionName(
                "unknown_tool"
        );

        functionCall.setArguments(
                "{}"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(functionCall, LOG_FILE_PATH)
        );
    }

    @Test
    void shouldRejectEmptySearchTerm() {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiFunctionCall functionCall =
                new AiFunctionCall();

        functionCall.setFunctionName(
                "search_log"
        );

        functionCall.setArguments(
                "{\"searchTerm\":\"\"}"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(functionCall, LOG_FILE_PATH)
        );
    }

    @Test
    void shouldRejectInvalidArgumentsJson() {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiFunctionCall functionCall =
                new AiFunctionCall();

        functionCall.setFunctionName(
                "search_log"
        );

        functionCall.setArguments(
                "INVALID_JSON"
        );

        assertThrows(
                Exception.class,
                () -> executor.execute(functionCall, LOG_FILE_PATH)
        );
    }

    @Test
    void shouldPreserveFunctionCallId() throws Exception {

        ToolExecutor executor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiFunctionCall functionCall =
                new AiFunctionCall();

        functionCall.setCallId(
                "call_456"
        );

        functionCall.setFunctionName(
                "search_log"
        );

        functionCall.setArguments(
                "{\"searchTerm\":\"Host name may not be null\"}"
        );

        ToolExecutionResult result =
                executor.execute(functionCall, LOG_FILE_PATH);

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

        AiFunctionCall functionCall = new AiFunctionCall();
        functionCall.setFunctionName("search_log");
        functionCall.setArguments("{}");

        assertThrows( IllegalArgumentException.class,
                () -> toolExecutor.execute( functionCall, LOG_FILE_PATH ) );

    }

    @Test void shouldRejectBlankSearchTerm() {

        AiFunctionCall functionCall = new AiFunctionCall();
        functionCall.setFunctionName( "search_log" );
        functionCall.setArguments( "{\"searchTerm\":\" \"}" );
        assertThrows( IllegalArgumentException.class,
                () -> toolExecutor.execute( functionCall, LOG_FILE_PATH ));
    }

    @Test
    void shouldRejectNullLogFile() {
        AiFunctionCall functionCall = new AiFunctionCall();
        functionCall.setCallId("call_123");
        functionCall.setFunctionName("search_log");
        functionCall.setArguments("{\"searchTerm\":\"Host name may not be null\"}");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolExecutor.execute(functionCall, null)
        );

        assertEquals("logFile must not be null", exception.getMessage());
    }
}