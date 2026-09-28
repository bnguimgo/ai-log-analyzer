package com.bnguimgo.ailoganalyzer.config;

import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.openai.OpenAiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "ai.provider.type=openai",
        "ai.provider.api-key=test-key"
})
class AiConfigurationOpenAiTest {

    @Autowired
    private AiClient aiClient;

    @Autowired
    private AiProviderProperties properties;

    @Test
    void shouldCreateOpenAiClient() {
        assertNotNull(aiClient);
        assertNotNull(properties);

        assertInstanceOf(
                OpenAiClient.class,
                aiClient);
    }
}