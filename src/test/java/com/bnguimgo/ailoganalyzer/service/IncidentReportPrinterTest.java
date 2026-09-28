package com.bnguimgo.ailoganalyzer.service;

import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisResult;
import com.bnguimgo.ailoganalyzer.domain.incident.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class IncidentReportPrinterTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldPrintReportWithoutError() {
        LogIncident incident = new LogIncident();

        incident.setType(IncidentType.OAUTH2_ERROR);
        incident.setSeverity(IncidentSeverity.ERROR);
        incident.setOccurrenceCount(2);
        incident.setFirstOccurrence(
                LocalDateTime.of(2026, 9, 16, 19, 23, 11)
        );
        incident.setLastOccurrence(
                LocalDateTime.of(2026, 9, 16, 19, 49, 15)
        );

        LogAnalysisResult result = new LogAnalysisResult(
                Collections.singletonList(incident),
                Collections.emptyList(),
                LOG_FILE_PATH
        );

        IncidentReportPrinter printer = new IncidentReportPrinter();

        assertDoesNotThrow(() ->
                printer.print(result)
        );
    }

    @Test
    void shouldPrintReportWithRelationsWithoutError() {
        LogIncident source = new LogIncident();
        source.setType(IncidentType.OAUTH2_ERROR);
        source.setSeverity(IncidentSeverity.ERROR);
        source.setOccurrenceCount(1);
        source.setFirstOccurrence(
                LocalDateTime.of(2026, 9, 16, 19, 23, 11)
        );
        source.setLastOccurrence(
                LocalDateTime.of(2026, 9, 16, 19, 23, 11)
        );

        LogIncident target = new LogIncident();
        target.setType(IncidentType.OAUTH2_ERROR);
        target.setSeverity(IncidentSeverity.ERROR);
        target.setOccurrenceCount(1);
        target.setFirstOccurrence(
                LocalDateTime.of(2026, 9, 16, 19, 30, 0)
        );
        target.setLastOccurrence(
                LocalDateTime.of(2026, 9, 16, 19, 30, 0)
        );

        IncidentRelation relation = new IncidentRelation(
                source,
                target,
                IncidentRelationType.TEMPORALLY_RELATED,
                "Les deux incidents sont proches dans le temps.");

        LogAnalysisResult result = new LogAnalysisResult(
                Arrays.asList(source, target),
                Collections.singletonList(relation),
                LOG_FILE_PATH
        );

        IncidentReportPrinter printer = new IncidentReportPrinter();

        assertDoesNotThrow(() ->
                printer.print(result)
        );
    }
}