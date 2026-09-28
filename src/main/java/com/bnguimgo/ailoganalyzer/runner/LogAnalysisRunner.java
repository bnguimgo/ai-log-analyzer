package com.bnguimgo.ailoganalyzer.runner;

import com.bnguimgo.ailoganalyzer.application.DefaultLogAnalysisService;
import com.bnguimgo.ailoganalyzer.application.LogAnalysisService;
import com.bnguimgo.ailoganalyzer.config.AiProviderProperties;
import com.bnguimgo.ailoganalyzer.domain.ai.AiPromptBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContextBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.AiAnalyzer;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.DefaultAiAnalyzer;
import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisReport;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.MockAiClient;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.DefaultLogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.LogSearchTool;
import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutor;
import com.bnguimgo.ailoganalyzer.parser.LogParser;
import com.bnguimgo.ailoganalyzer.parser.LogReader;
import com.bnguimgo.ailoganalyzer.service.IncidentCorrelator;
import com.bnguimgo.ailoganalyzer.service.IncidentDetector;
import com.bnguimgo.ailoganalyzer.service.IncidentGrouper;
import com.bnguimgo.ailoganalyzer.service.IncidentReportPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LogAnalysisRunner {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    public static void main(String[] args) throws Exception {

        Path logFile = resolveLogFile(args);

        AiProviderProperties properties =
                new AiProviderProperties();

        properties.setModel("test-model");

        MockAiClient mockAiClient =
                new MockAiClient();

        AiPromptBuilder prompt =
                new AiPromptBuilder();

        LogAnalysisService analysisService =
                getLogAnalysisService(
                        properties,
                        mockAiClient,
                        prompt
                );

        IncidentReportPrinter reportPrinter =
                new IncidentReportPrinter();

        System.out.println(
                "Analyse du fichier : "
                        + logFile.toAbsolutePath()
        );

        LogAnalysisReport report =
                analysisService.analyze(logFile);

        reportPrinter.print(
                report.analysisResult()
        );
    }

    private static LogAnalysisService getLogAnalysisService(
            AiProviderProperties properties,
            MockAiClient mockAiClient,
            AiPromptBuilder prompt) {

        LogSearchTool logSearchTool =
                new DefaultLogSearchTool();

        ObjectMapper objectMapper =
                new ObjectMapper();

        ToolExecutor toolExecutor =
                new ToolExecutor(
                        logSearchTool,
                        objectMapper
                );

        AiAnalyzer aiAnalyzer =
                new DefaultAiAnalyzer(
                        mockAiClient,
                        prompt,
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

    private static Path resolveLogFile(String[] args) {

        if (args.length > 0
                && args[0] != null
                && !args[0].trim().isEmpty()) {

            return Paths.get(args[0]);
        }

        return LOG_FILE_PATH;
    }
}