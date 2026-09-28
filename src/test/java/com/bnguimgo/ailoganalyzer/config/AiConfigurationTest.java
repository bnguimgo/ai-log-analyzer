package com.bnguimgo.ailoganalyzer.config;

import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.AiAnalyzer;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.DefaultAiAnalyzer;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.MockAiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {"ai.provider.type=mock"})
class AiConfigurationTest {

    @Autowired
    private AiAnalyzer aiAnalyzer;

    @Autowired
    private AiClient aiClient;

    @Autowired
    private AiProviderProperties properties;

    @Test
    void shouldCreateAiBeans() {
        assertNotNull(aiAnalyzer);
        assertNotNull(aiClient);
        assertNotNull(properties);

        assertInstanceOf(
                DefaultAiAnalyzer.class,
                aiAnalyzer);

        assertInstanceOf(
                MockAiClient.class,
                aiClient);
    }
}