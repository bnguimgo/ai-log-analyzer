package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultLogSearchToolTest {

    private final DefaultLogSearchTool tool = new DefaultLogSearchTool();
    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldFindMatchingLines() throws Exception {

        List<String> results =
                tool.search(
                        LOG_FILE_PATH,
                        "Host name may not be null"
                );

        assertEquals(7, results.size());
    }

    @Test
    void shouldReturnEmptyListWhenNothingMatches() throws Exception {

        List<String> results = tool.search(LOG_FILE_PATH, "THIS_TEXT_DOES_NOT_EXIST");

        assertEquals(0, results.size());
    }

    @Test
    void shouldRejectNullFile() {

        assertThrows(
                IllegalArgumentException.class,
                () -> tool.search(
                        null,
                        "ERROR"
                )
        );
    }

    @Test
    void shouldRejectEmptySearchTerm() {

        assertThrows(
                IllegalArgumentException.class,
                () -> tool.search(
                        LOG_FILE_PATH,
                        ""
                )
        );
    }
}