package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisResult;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class StructuredContextBuilderTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldBuildStructuredContextFromAnalysisResult() {

        LogIncident incident = new LogIncident();

        LogAnalysisResult result =
                new LogAnalysisResult(
                        Collections.singletonList(incident),
                        Collections.emptyList(),
                        LOG_FILE_PATH
                );

        StructuredContextBuilder builder =
                new StructuredContextBuilder();

        StructuredContext context =
                builder.build(result);

        assertNotNull(context);
        assertEquals(1, context.incidents().size());
        assertTrue(context.relations().isEmpty());
    }

    @Test
    void shouldRejectNullAnalysisResult() {

        StructuredContextBuilder builder =
                new StructuredContextBuilder();

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.build(null)
        );
    }
}