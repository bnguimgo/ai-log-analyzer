package com.bnguimgo.ailoganalyzer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiProviderPropertiesTest {

    @Test
    void shouldStoreProviderConfiguration() {

        AiProviderProperties properties = new AiProviderProperties();

        properties.setApiKey("test-api-key");
        properties.setModel("test-model");

        assertEquals("test-api-key", properties.getApiKey());

        assertEquals("test-model", properties.getModel());
    }

    @Test
    void shouldBindType() {

        AiProviderProperties properties = new AiProviderProperties();

        properties.setType("openai");

        assertEquals("openai", properties.getType());
    }
}