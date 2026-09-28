package com.bnguimgo.ailoganalyzer.service;

import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelationType;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentType;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncidentCorrelatorTest {

    @Test
    void shouldDetectTemporalRelation() {

        LogIncident first = createIncident(
                LocalDateTime.of(2026, 9, 16, 19, 0),
                "OAUTH2_ERROR|400"
        );

        LogIncident second = createIncident(
                LocalDateTime.of(2026, 9, 16, 19, 30),
                "OAUTH2_ERROR|401"
        );

        IncidentCorrelator correlator = new IncidentCorrelator();

        List<IncidentRelation> relations =
                correlator.correlate(Arrays.asList(first, second));

        assertTrue(relations.stream()
                .anyMatch(relation ->
                        relation.getType()
                                == IncidentRelationType.TEMPORALLY_RELATED));
    }

    @Test
    void shouldNotDetectTemporalRelationWhenIncidentsAreTooFarApart() {

        LogIncident first = createIncident(
                LocalDateTime.of(2026, 9, 16, 14, 0),
                "OAUTH2_ERROR|400"
        );

        LogIncident second = createIncident(
                LocalDateTime.of(2026, 9, 16, 19, 0),
                "OAUTH2_ERROR|401"
        );

        IncidentCorrelator correlator = new IncidentCorrelator();

        List<IncidentRelation> relations =
                correlator.correlate(Arrays.asList(first, second));

        assertFalse(relations.stream()
                .anyMatch(relation ->
                        relation.getType()
                                == IncidentRelationType.TEMPORALLY_RELATED));
    }

    @Test
    void shouldDetectSameSignature() {

        LogIncident first = createIncident(
                LocalDateTime.of(2026, 9, 16, 19, 0),
                "OAUTH2_ERROR|400"
        );

        LogIncident second = createIncident(
                LocalDateTime.of(2026, 9, 16, 19, 5),
                "OAUTH2_ERROR|400"
        );

        IncidentCorrelator correlator = new IncidentCorrelator();

        List<IncidentRelation> relations =
                correlator.correlate(Arrays.asList(first, second));

        assertTrue(relations.stream()
                .anyMatch(relation ->
                        relation.getType()
                                == IncidentRelationType.SAME_SIGNATURE));
    }

    private LogIncident createIncident(
            LocalDateTime timestamp,
            String signature) {

        LogIncident incident = new LogIncident();

        incident.setType(IncidentType.OAUTH2_ERROR);
        incident.setFirstOccurrence(timestamp);
        incident.setLastOccurrence(timestamp);
        incident.setSignature(signature);
        incident.setMainClass(
                "fr.pomona.manageo.web.security.cognito.TestClass"
        );

        return incident;
    }
}