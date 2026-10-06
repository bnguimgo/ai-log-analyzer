package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogSearchToolDefinitionTest {

    @Test
    void shouldCreateValidToolDefinition() {

        Map<String, Object> definition =
                LogSearchToolDefinition.asMap();

        assertNotNull(definition);

        assertEquals(
                "function",
                definition.get("type")
        );

        assertEquals(
                "search_log",
                definition.get("name")
        );

        assertNotNull(
                definition.get("description")
        );

        String expectedDescription = "Recherche un texte dans le fichier de log. "
                + "Utilise cet outil lorsque tu dois vérifier la présence "
                + "d'un message, d'une exception ou d'un autre élément "
                + "dans le log afin d'obtenir des éléments de preuve.";

        assertEquals(
                expectedDescription,
                definition.get("description")
        );

        assertNotNull(
                definition.get("parameters")
        );
    }
}