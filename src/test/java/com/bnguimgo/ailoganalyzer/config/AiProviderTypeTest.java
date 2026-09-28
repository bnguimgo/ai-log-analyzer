package com.bnguimgo.ailoganalyzer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderTypeTest {

    @Test
    void shouldConvertMockProvider() {
        assertEquals(
                AiProviderType.MOCK,
                AiProviderType.fromValue("mock"));
    }

    @Test
    void shouldConvertOpenAiProvider() {
        assertEquals(
                AiProviderType.OPENAI,
                AiProviderType.fromValue("openai"));
    }

    @Test
    void shouldIgnoreCase() {
        assertEquals(
                AiProviderType.OPENAI,
                AiProviderType.fromValue("OPENAI"));
    }

    @Test
    void shouldIgnoreSurroundingSpaces() {
        assertEquals(
                AiProviderType.MOCK,
                AiProviderType.fromValue("  mock  "));
    }

    @Test
    void shouldRejectUnknownProvider() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiProviderType.fromValue("opneai"));

        assertEquals(
                "Unsupported AI provider: opneai. " +
                        "Supported providers: mock, openai",
                exception.getMessage());
    }

    @Test
    void shouldRejectEmptyProvider() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AiProviderType.fromValue(""));

        assertEquals(
                "AI provider must not be null or empty. " +
                        "Supported providers: mock, openai",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullProvider() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AiProviderType.fromValue(null));
    }
}