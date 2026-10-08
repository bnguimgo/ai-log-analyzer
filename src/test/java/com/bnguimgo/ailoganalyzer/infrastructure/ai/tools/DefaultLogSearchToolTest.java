package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import com.bnguimgo.ailoganalyzer.domain.ai.SearchRequest;
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

        SearchRequest searchRequest = new SearchRequest(
                "Host name may not be null",
                "Vérifier la présence de cette erreur");
        List<String> results =
                tool.search(
                        LOG_FILE_PATH,
                        searchRequest
                );

        assertEquals(7, results.size());
    }

    @Test
    void shouldReturnEmptyListWhenNothingMatches() throws Exception {

        SearchRequest searchRequest = new SearchRequest(
                "THIS_TEXT_DOES_NOT_EXIST",
                "Vérifier la présence de cette erreur");
        List<String> results = tool.search(LOG_FILE_PATH, searchRequest);

        assertEquals(0, results.size());
    }

    @Test
    void shouldRejectNullFile() {

        SearchRequest searchRequest = new SearchRequest(
                "ERROR",
                "Vérifier la présence de cette erreur");
        assertThrows(
                IllegalArgumentException.class,
                () -> tool.search(null, searchRequest)
        );
    }

}