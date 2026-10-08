package com.bnguimgo.ailoganalyzer.domain.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchRequestTest {

    @Test
    void shouldCreateSearchRequest() {

        SearchRequest request = new SearchRequest(
                "CustomHttpResponseException",
                "Identifier les erreurs HTTP 400 associées"
        );

        assertEquals(
                "CustomHttpResponseException",
                request.searchTerm()
        );

        assertEquals(
                "Identifier les erreurs HTTP 400 associées",
                request.objective()
        );
    }

    @Test
    void shouldRejectBlankSearchTerm() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchRequest(
                        "",
                        "Identifier les erreurs HTTP 400 associées"
                )
        );
    }

    @Test
    void shouldRejectBlankObjective() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchRequest(
                        "CustomHttpResponseException",
                        ""
                )
        );
    }

    @Test
    void shouldRejectNullSearchTerm() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchRequest(
                        null,
                        "Identifier les erreurs HTTP 400 associées"
                )
        );
    }

    @Test
    void shouldRejectNullObjective() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchRequest(
                        "CustomHttpResponseException",
                        null
                )
        );
    }

    @Test
    void shouldConsiderRequestsWithSameTermAndObjectiveAsEqual() {

        SearchRequest first = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        SearchRequest second = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldConsiderRequestsWithDifferentObjectivesAsDifferent() {

        SearchRequest first = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        SearchRequest second = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver la stacktrace complète"
        );

        assertNotEquals(first, second);
    }

    @Test
    void shouldConsiderRequestsWithDifferentTermsAsDifferent() {

        SearchRequest first = new SearchRequest(
                "CustomHttpResponseException",
                "Trouver les erreurs HTTP 400"
        );

        SearchRequest second = new SearchRequest(
                "Host name may not be null",
                "Trouver les erreurs HTTP 400"
        );

        assertNotEquals(first, second);
    }
}