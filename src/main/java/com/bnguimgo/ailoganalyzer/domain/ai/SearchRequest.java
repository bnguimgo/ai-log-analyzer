package com.bnguimgo.ailoganalyzer.domain.ai;

import java.util.Objects;

/**
 * Cette classe décrit une recherche
 */
public record SearchRequest(String searchTerm, String objective) {

    public SearchRequest {

        if (searchTerm == null || searchTerm.isBlank()) {
            throw new IllegalArgumentException(
                    "searchTerm must not be blank"
            );
        }

        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException(
                    "objective must not be blank"
            );
        }

    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof SearchRequest that)) {
            return false;
        }

        return Objects.equals(searchTerm, that.searchTerm)
                && Objects.equals(objective, that.objective);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                searchTerm,
                objective
        );
    }
}