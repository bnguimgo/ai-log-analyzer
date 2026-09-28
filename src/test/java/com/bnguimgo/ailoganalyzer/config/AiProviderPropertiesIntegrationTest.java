package com.bnguimgo.ailoganalyzer.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiProviderPropertiesIntegrationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues(
                            "ai.provider.model=test-model",
                            "ai.provider.api-key=test-api-key"
                    );

    @Test
    void shouldBindProviderConfiguration() {

        contextRunner.run(context -> {

            AiProviderProperties properties =
                    context.getBean(AiProviderProperties.class);

            assertEquals(
                    "test-model",
                    properties.getModel()
            );

            assertEquals(
                    "test-api-key",
                    properties.getApiKey()
            );
        });
    }

    @EnableConfigurationProperties(AiProviderProperties.class)
    static class TestConfiguration {
    }
}