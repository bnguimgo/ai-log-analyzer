package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class MockAiAnalyzerTest {

    @Test
    void shouldAnalyzeStructuredContext() {

        LogIncident incident = new LogIncident();

        StructuredContext context =
                new StructuredContext(
                        Collections.singletonList(incident),
                        Collections.emptyList(),
                        Path.of("test.log")
                );

        MockAiAnalyzer analyzer =
                new MockAiAnalyzer();

        AiAnalysisResponse response =
                analyzer.analyze(context);

        assertNotNull(response);

        assertEquals(
                "Analyse simulée de 1 famille(s) d'incidents.",
                response.getSummary()
        );

        assertNotNull(response.getProbableCauses());
        assertNotNull(response.getRecommendations());
        assertNotNull(response.getUncertainties());
    }

    @Test
    void shouldRejectNullContext() {

        MockAiAnalyzer analyzer =
                new MockAiAnalyzer();

        assertThrows(
                IllegalArgumentException.class,
                () -> analyzer.analyze(null)
        );
    }

    /**
     * Ce test permet de s'assurer que le mock MockAiAnalyzer ne modifie pas le contexte reçu
     */
    @Test
    void shouldNotModifyContext() {

        LogIncident incident = new LogIncident();

        StructuredContext context =
                new StructuredContext(
                        Collections.singletonList(incident),
                        Collections.emptyList(),
                        Path.of("test.log")
                );

        int incidentCount =
                context.incidents().size();

        MockAiAnalyzer analyzer =
                new MockAiAnalyzer();

        analyzer.analyze(context);

        assertEquals(
                incidentCount,
                context.incidents().size()
        );
    }
}
