package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchHistoryTest {

    @Test
    void shouldReturnFalseWhenSearchHasNotBeenPerformed() {

        SearchHistory history = new SearchHistory();

        SearchRequest request = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        assertFalse(history.contains(request));
    }

    @Test
    void shouldReturnTrueWhenSearchHasBeenPerformed() {

        SearchHistory history = new SearchHistory();

        SearchRequest request = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        ToolExecutionResult result = createToolResult(request);

        history.add(request, result);

        assertTrue(history.contains(request));
    }

    @Test
    void shouldRecognizeEquivalentSearchRequest() {

        SearchHistory history = new SearchHistory();

        SearchRequest first = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        SearchRequest equivalent = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        ToolExecutionResult result = createToolResult(first);

        history.add(first, result);

        assertTrue(history.contains(equivalent));
    }

    @Test
    void shouldNotConsiderSameTermWithDifferentObjectiveAsSameSearch() {

        SearchHistory history = new SearchHistory();

        SearchRequest first = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        SearchRequest differentObjective = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver la stacktrace complète"
        );

        ToolExecutionResult result = createToolResult(first);

        history.add(first, result);

        assertTrue(history.contains(differentObjective));
        assertEquals(result, history.findResult(differentObjective));
    }

    @Test
    void shouldNotReuseResultForDifferentSearchTerms() {

        SearchHistory history = new SearchHistory();

        SearchRequest first = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        SearchRequest differentTerm = new SearchRequest(
                "Host name may not be null",
                "Trouver les erreurs de configuration du proxy"
        );

        ToolExecutionResult result = createToolResult(first);

        history.add(first, result);

        assertFalse(history.contains(differentTerm));
        assertNull(history.findResult(differentTerm));
    }

    private ToolExecutionResult createToolResult(
            SearchRequest request) {

        return new ToolExecutionResult(
                "call_1",
                "Résultat du tool",
                request
        );
    }
}