package com.bnguimgo.ailoganalyzer.service;

import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IncidentGrouper {

    public List<LogIncident> group(List<LogIncident> incidents) {
        Map<String, LogIncident> groupedIncidents =
                new LinkedHashMap<>();

        for (LogIncident incident : incidents) {
            String signature = buildSignature(incident);
            incident.setSignature(signature);

            LogIncident existingIncident = groupedIncidents.get(signature);

            if (existingIncident == null) {
                groupedIncidents.put(signature, incident);
            } else {
                merge(existingIncident, incident);
            }
        }

        return new ArrayList<>(groupedIncidents.values());
    }

    private String buildSignature(LogIncident incident) {

        return valueOrUnknown(incident.getType()) +
                "|" +
                valueOrUnknown(incident.getExceptionType()) +
                "|" +
                valueOrUnknown(incident.getRootCauseType()) +
                "|" +
                valueOrUnknown(incident.getHttpStatus());
    }

    private void merge(LogIncident target, LogIncident source) {
        target.setOccurrenceCount(
                target.getOccurrenceCount() + source.getOccurrenceCount()
        );

        if (source.getFirstOccurrence() != null
                && (target.getFirstOccurrence() == null
                || source.getFirstOccurrence().isBefore(
                target.getFirstOccurrence()))) {

            target.setFirstOccurrence(source.getFirstOccurrence());
        }

        if (source.getLastOccurrence() != null
                && (target.getLastOccurrence() == null
                || source.getLastOccurrence().isAfter(
                target.getLastOccurrence()))) {

            target.setLastOccurrence(source.getLastOccurrence());
        }

        mergeInvolvedClasses(target, source);
    }

    private void mergeInvolvedClasses(
            LogIncident target,
            LogIncident source) {

        for (String className : source.getInvolvedClasses()) {
            if (!target.getInvolvedClasses().contains(className)) {
                target.getInvolvedClasses().add(className);
            }
        }
    }

    private String valueOrUnknown(Object value) {
        return value == null ? "UNKNOWN" : value.toString();
    }
}