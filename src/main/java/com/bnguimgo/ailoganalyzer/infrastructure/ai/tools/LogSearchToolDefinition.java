package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import java.util.Map;

public class LogSearchToolDefinition {

    private LogSearchToolDefinition() {
    }

    public static Map<String, Object> asMap() {

        //NB : Voir la documentation de l'API OpenAI pour valider le format de sortir ci-dessous
        return Map.of(
                "type", "function",
                "name", "search_log",
                "description",
                "Recherche un texte dans le fichier de log.",
                "parameters",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "searchTerm", Map.of(
                                        "type", "string",
                                        "description",
                                        "Texte à rechercher dans le log."
                                )
                        ),
                        "required", java.util.List.of("searchTerm"),
                        "additionalProperties", false
                ),
                "strict", true
        );
    }
}