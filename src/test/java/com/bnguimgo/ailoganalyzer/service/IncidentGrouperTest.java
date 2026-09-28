package com.bnguimgo.ailoganalyzer.service;

import com.bnguimgo.ailoganalyzer.domain.incident.IncidentType;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncidentGrouperTest {

    @Test
    void shouldGroupSimilarIncidents() {
        LogIncident first = createIncident(
                LocalDateTime.of(2026, 9, 16, 19, 23, 11),
                400
        );

        LogIncident second = createIncident(
                LocalDateTime.of(2026, 9, 16, 19, 49, 15),
                400
        );

        LogIncident third = createIncident(
                LocalDateTime.of(2026, 9, 16, 20, 6, 33),
                400
        );

        IncidentGrouper grouper = new IncidentGrouper();

        List<LogIncident> grouped = grouper.group(
                Arrays.asList(first, second, third)
        );

        assertEquals(1, grouped.size());

        LogIncident groupedIncident = grouped.getFirst();

        assertEquals(3, groupedIncident.getOccurrenceCount());

        assertEquals(
                LocalDateTime.of(2026, 9, 16, 19, 23, 11),
                groupedIncident.getFirstOccurrence()
        );

        assertEquals(
                LocalDateTime.of(2026, 9, 16, 20, 6, 33),
                groupedIncident.getLastOccurrence()
        );

        assertEquals(
                "OAUTH2_ERROR|UNKNOWN|UNKNOWN|400",
                groupedIncident.getSignature()
        );
    }

    private LogIncident createIncident(
            LocalDateTime timestamp,
            int httpStatus) {

        LogIncident incident = new LogIncident();

        incident.setType(IncidentType.OAUTH2_ERROR);
        incident.setFirstOccurrence(timestamp);
        incident.setLastOccurrence(timestamp);
        incident.setOccurrenceCount(1);
        incident.setHttpStatus(httpStatus);

        return incident;
    }
}