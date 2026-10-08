package com.bnguimgo.ailoganalyzer.application;

import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.prompt.AiPromptBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContextBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.AiAnalyzer;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.DefaultAiAnalyzer;
import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisReport;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.MockAiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.DefaultLogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import com.bnguimgo.ailoganalyzer.parser.LogParser;
import com.bnguimgo.ailoganalyzer.parser.LogReader;
import com.bnguimgo.ailoganalyzer.service.IncidentCorrelator;
import com.bnguimgo.ailoganalyzer.service.IncidentDetector;
import com.bnguimgo.ailoganalyzer.service.IncidentGrouper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLogAnalysisServiceWithAiTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldExecuteAnalysisWithAiAnalyzer() throws Exception {

        DefaultLogAnalysisService service = getDefaultLogAnalysisService();

        LogAnalysisReport report = service.analyze(LOG_FILE_PATH);

        assertNotNull(report);
        assertNotNull(report.analysisResult());
        assertNotNull(report.structuredContext());

        AiAnalysisResponse response =
                report.aiAnalysisResponse();

        assertNotNull(response);
        assertEquals(
                "Analyse IA simulée avec le modèle : test-model",
                response.getSummary()
        );
    }

    private static DefaultLogAnalysisService getDefaultLogAnalysisService() {

        AiProviderProperties properties =
                new AiProviderProperties();

        properties.setModel("test-model");

        ToolExecutor toolExecutor =
                new ToolExecutor(
                        new DefaultLogSearchTool(),
                        new ObjectMapper()
                );

        AiAnalyzer aiAnalyzer =
                new DefaultAiAnalyzer(
                        new MockAiClient(),
                        new AiPromptBuilder(),
                        properties,
                        toolExecutor
                );

        return new DefaultLogAnalysisService(
                new LogReader(),
                new LogParser(),
                new IncidentDetector(),
                new IncidentGrouper(),
                new IncidentCorrelator(),
                new StructuredContextBuilder(),
                aiAnalyzer
        );
    }
}