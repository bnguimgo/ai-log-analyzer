package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record StructuredContext(List<LogIncident> incidents,
                                List<IncidentRelation> relations,
                                Path logFile) {

    public StructuredContext(
            List<LogIncident> incidents,
            List<IncidentRelation> relations,
            Path logFile) {

        if (incidents == null) {
            throw new IllegalArgumentException(
                    "incidents must not be null"
            );
        }

        if (relations == null) {
            throw new IllegalArgumentException(
                    "relations must not be null"
            );
        }

        if (logFile == null) {
            throw new IllegalArgumentException(
                    "logFile must not be null"
            );
        }

        this.incidents = new ArrayList<>(incidents);
        this.relations = new ArrayList<>(relations);
        this.logFile = logFile;
    }
}