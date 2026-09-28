package com.bnguimgo.ailoganalyzer.domain.analysis;

import com.bnguimgo.ailoganalyzer.domain.ai.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class LogAnalysisReportTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldCreateCompleteLogAnalysisReport() {

        LogAnalysisResult analysisResult =
                new LogAnalysisResult(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        LOG_FILE_PATH
                );

        StructuredContext context =
                new StructuredContext(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        LOG_FILE_PATH
                );

        AiAnalysisResponse aiResponse =
                new AiAnalysisResponse();

        LogAnalysisReport report =
                new LogAnalysisReport(
                        analysisResult,
                        context,
                        aiResponse);

        assertSame(
                analysisResult,
                report.analysisResult());

        assertSame(
                context,
                report.structuredContext());

        assertSame(
                aiResponse,
                report.aiAnalysisResponse());
    }
}