package com.bnguimgo.ailoganalyzer.application;

import com.bnguimgo.ailoganalyzer.domain.ai.AiAnalysisResponse;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;
import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContextBuilder;
import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.AiAnalyzer;
import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisReport;
import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisResult;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import com.bnguimgo.ailoganalyzer.domain.log.LogEvent;
import com.bnguimgo.ailoganalyzer.parser.LogParser;
import com.bnguimgo.ailoganalyzer.parser.LogReader;
import com.bnguimgo.ailoganalyzer.service.IncidentCorrelator;
import com.bnguimgo.ailoganalyzer.service.IncidentDetector;
import com.bnguimgo.ailoganalyzer.service.IncidentGrouper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class DefaultLogAnalysisService implements LogAnalysisService {

    private final LogReader logReader;
    private final LogParser logParser;
    private final IncidentDetector incidentDetector;
    private final IncidentGrouper incidentGrouper;
    private final IncidentCorrelator incidentCorrelator;
    private final StructuredContextBuilder structuredContextBuilder;
    private final AiAnalyzer aiAnalyzer;

    public DefaultLogAnalysisService(
            LogReader logReader,
            LogParser logParser,
            IncidentDetector incidentDetector,
            IncidentGrouper incidentGrouper,
            IncidentCorrelator incidentCorrelator,
            StructuredContextBuilder structuredContextBuilder,
            AiAnalyzer aiAnalyzer) {

        this.logReader = logReader;
        this.logParser = logParser;
        this.incidentDetector = incidentDetector;
        this.incidentGrouper = incidentGrouper;
        this.incidentCorrelator = incidentCorrelator;
        this.structuredContextBuilder = structuredContextBuilder;
        this.aiAnalyzer = aiAnalyzer;
    }

    @Override
    public LogAnalysisReport analyze(Path logFile) throws IOException {

        if (logFile == null) {
            throw new IllegalArgumentException("logFile must not be null");
        }

        List<String> lines = logReader.read(logFile);
        List<LogEvent> events = logParser.parse(lines);
        List<LogIncident> incidents = incidentDetector.detect(events);
        List<LogIncident> groupedIncidents = incidentGrouper.group(incidents);
        List<IncidentRelation> relations = incidentCorrelator.correlate(groupedIncidents);

        LogAnalysisResult analysisResult = new LogAnalysisResult(groupedIncidents, relations, logFile);

        StructuredContext context = structuredContextBuilder.build(analysisResult);

        AiAnalysisResponse aiResponse = aiAnalyzer.analyze(context);

        return new LogAnalysisReport(
                analysisResult,
                context,
                aiResponse);
    }
}
