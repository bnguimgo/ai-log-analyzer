package com.bnguimgo.ailoganalyzer.runner;

import com.bnguimgo.ailoganalyzer.AiLogAnalyzerApplication;
import com.bnguimgo.ailoganalyzer.application.LogAnalysisService;
import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisReport;
import com.bnguimgo.ailoganalyzer.service.IncidentReportPrinter;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SpringLogAnalysisRunner {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    public static void main(String[] args) {

        Path logFile = resolveLogFile(args);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AiLogAnalyzerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            LogAnalysisService analysisService =
                    context.getBean(LogAnalysisService.class);

            IncidentReportPrinter reportPrinter =
                    context.getBean(IncidentReportPrinter.class);

            System.out.println(
                    "Analyse du fichier : " + logFile.toAbsolutePath()
            );

            LogAnalysisReport report = analysisService.analyze(logFile);

            reportPrinter.print(report.getAnalysisResult());

            System.out.println();
            System.out.println(
                    "Analyse IA : " + report.getAiAnalysisResponse().getSummary());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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