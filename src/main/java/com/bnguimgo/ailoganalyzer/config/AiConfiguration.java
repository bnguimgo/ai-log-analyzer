package com.bnguimgo.ailoganalyzer.config;

import com.bnguimgo.ailoganalyzer.domain.ai.prompt.AiPromptBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.AiAnalyzer;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.DefaultAiAnalyzer;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.AiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.MockAiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.openai.OpenAiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.DefaultLogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    @Bean
    public AiClient aiClient(AiProviderProperties properties,
                             ObjectMapper objectMapper) {

        AiProviderType provider = AiProviderType.fromValue(properties.getType());

        return switch (provider) {
            case MOCK -> new MockAiClient();
            case OPENAI -> new OpenAiClient(properties, objectMapper);
            default -> throw new IllegalStateException(
                    "Unsupported AI provider: " + provider);
        };
    }

    @Bean
    public AiPromptBuilder aiPromptBuilder() {
        return new AiPromptBuilder();
    }

    @Bean
    public AiAnalyzer aiAnalyzer(
            AiClient aiClient,
            AiPromptBuilder aiPromptBuilder,
            AiProviderProperties properties,
            ToolExecutor toolExecutor) {

        return new DefaultAiAnalyzer(
                aiClient,
                aiPromptBuilder,
                properties,
                toolExecutor
        );
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public LogSearchTool logSearchTool() {
        return new DefaultLogSearchTool();
    }

    @Bean
    public ToolExecutor toolExecutor(
            LogSearchTool logSearchTool,
            ObjectMapper objectMapper) {

        return new ToolExecutor(
                logSearchTool,
                objectMapper
        );
    }
}