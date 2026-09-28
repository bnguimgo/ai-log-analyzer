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

        assertNotNull(
                definition.get("parameters")
        );
    }
}