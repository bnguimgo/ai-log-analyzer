package com.bnguimgo.ailoganalyzer.application;

import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContextBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.AiAnalyzer;
import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisReport;
import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisResult;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelationType;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import com.bnguimgo.ailoganalyzer.domain.log.LogEvent;
import com.bnguimgo.ailoganalyzer.parser.LogParser;
import com.bnguimgo.ailoganalyzer.parser.LogReader;
import com.bnguimgo.ailoganalyzer.service.IncidentCorrelator;
import com.bnguimgo.ailoganalyzer.service.IncidentDetector;
import com.bnguimgo.ailoganalyzer.service.IncidentGrouper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultLogAnalysisServiceTest {

    @Mock
    private LogReader logReader;

    @Mock
    private LogParser logParser;

    @Mock
    private IncidentDetector incidentDetector;

    @Mock
    private IncidentGrouper incidentGrouper;

    @Mock
    private IncidentCorrelator incidentCorrelator;

    @Mock
    private StructuredContextBuilder structuredContextBuilder;

    @Mock
    private AiAnalyzer aiAnalyzer;

    @InjectMocks
    private DefaultLogAnalysisService service;

    @Test
    void shouldExecuteCompleteAnalysisPipeline() throws IOException {

        Path logFile = Paths.get("test.log");

        List<String> lines = Arrays.asList(
                "line1",
                "line2"
        );

        List<LogEvent> events = List.of(
                new LogEvent()
        );

        List<LogIncident> incidents = List.of(
                new LogIncident()
        );

        List<LogIncident> groupedIncidents = List.of(
                new LogIncident()
        );

        List<IncidentRelation> relations = List.of(
                new IncidentRelation(
                        groupedIncidents.getFirst(),
                        groupedIncidents.getFirst(),
                        IncidentRelationType.SAME_SIGNATURE,
                        "same signature"
                )
        );

        LogAnalysisResult analysisResult =
                new LogAnalysisResult(
                        groupedIncidents,
                        relations,
                        logFile
                );

        StructuredContext context =
                new StructuredContext(
                        groupedIncidents,
                        relations,
                        logFile
                );

        AiAnalysisResponse aiResponse =
                new AiAnalysisResponse();

        when(logReader.read(logFile))
                .thenReturn(lines);

        when(logParser.parse(lines))
                .thenReturn(events);

        when(incidentDetector.detect(events))
                .thenReturn(incidents);

        when(incidentGrouper.group(incidents))
                .thenReturn(groupedIncidents);

        when(incidentCorrelator.correlate(groupedIncidents))
                .thenReturn(relations);

        when(structuredContextBuilder.build(analysisResult))
                .thenReturn(context);

        when(aiAnalyzer.analyze(context))
                .thenReturn(aiResponse);

        LogAnalysisReport result = service.analyze(logFile);

        assertNotNull(result);

        assertNotNull(result.getAnalysisResult());

        assertEquals(
                groupedIncidents,
                result.getAnalysisResult().incidents()
        );

        assertEquals(
                relations,
                result.getAnalysisResult().relations()
        );

        assertSame(context, result.getStructuredContext());
        assertSame(aiResponse, result.getAiAnalysisResponse());

        InOrder inOrder = inOrder(
                logReader,
                logParser,
                incidentDetector,
                incidentGrouper,
                incidentCorrelator,
                structuredContextBuilder,
                aiAnalyzer
        );

        inOrder.verify(logReader).read(logFile);
        inOrder.verify(logParser).parse(lines);
        inOrder.verify(incidentDetector).detect(events);
        inOrder.verify(incidentGrouper).group(incidents);
        inOrder.verify(incidentCorrelator).correlate(groupedIncidents);
        inOrder.verify(structuredContextBuilder).build(analysisResult);
        inOrder.verify(aiAnalyzer).analyze(context);

        verifyNoMoreInteractions(
                logReader,
                logParser,
                incidentDetector,
                incidentGrouper,
                incidentCorrelator,
                structuredContextBuilder,
                aiAnalyzer
        );
    }

    @Test
    void shouldRejectNullLogFile() {

        DefaultLogAnalysisService service =
                new DefaultLogAnalysisService(
                        logReader,
                        logParser,
                        incidentDetector,
                        incidentGrouper,
                        incidentCorrelator,
                        structuredContextBuilder,
                        aiAnalyzer
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.analyze(null)
        );
    }

}