package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import java.util.List;
import java.util.Map;

public class LogSearchToolDefinition {

    private LogSearchToolDefinition() {
    }

    public static Map<String, Object> asMap() {

        // NB : Voir la documentation de l'API OpenAI pour valider le format de propriétés ci-dessous
        return Map.of(
                "type", "function",
                "name", "search_log",
                "description",
                "Recherche un texte dans le fichier de log. "
                        + "Utilise cet outil lorsque tu dois vérifier la présence "
                        + "d'un message, d'une exception ou d'un autre élément "
                        + "dans le log afin d'obtenir des éléments de preuve.",
                "parameters",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "searchTerm", Map.of(
                                        "type", "string",
                                        "description",
                                        "Texte à rechercher dans le log."
                                ),
                                "objective", Map.of(
                                        "type", "string",
                                        "description",
                                        "Objectif de la recherche."
                                )
                        ),
                        "required", List.of(
                                "searchTerm",
                                "objective"
                        ),
                        "additionalProperties", false
                ),
                "strict", true
        );
    }
}