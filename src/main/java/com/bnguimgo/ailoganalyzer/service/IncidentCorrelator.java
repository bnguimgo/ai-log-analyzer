package com.bnguimgo.ailoganalyzer.service;

import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelationType;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class IncidentCorrelator {

    private static final long MAX_TEMPORAL_GAP_MINUTES = 60;

    public List<IncidentRelation> correlate(List<LogIncident> incidents) {

        List<IncidentRelation> relations = new ArrayList<>();

        for (int i = 0; i < incidents.size(); i++) {

            LogIncident source = incidents.get(i);

            for (int j = i + 1; j < incidents.size(); j++) {

                LogIncident target = incidents.get(j);

                correlatePair(source, target, relations);
            }
        }

        return relations;
    }

    private void correlatePair(
            LogIncident source,
            LogIncident target,
            List<IncidentRelation> relations) {

        if (hasSameSignature(source, target)) {
            relations.add(createRelation(
                    source,
                    target,
                    IncidentRelationType.SAME_SIGNATURE,
                    "Les deux incidents possèdent la même signature."
            ));
        }

        if (hasSameComponent(source, target)) {
            relations.add(createRelation(
                    source,
                    target,
                    IncidentRelationType.SAME_COMPONENT,
                    "Les deux incidents impliquent le même composant applicatif."
            ));
        }

        if (isTemporallyRelated(source, target)) {
            relations.add(createRelation(
                    source,
                    target,
                    IncidentRelationType.TEMPORALLY_RELATED,
                    "Les deux incidents sont proches dans le temps."
            ));
        }

        if (isPossibleCausalLink(source, target)) {
            relations.add(createRelation(
                    source,
                    target,
                    IncidentRelationType.POSSIBLE_CAUSAL_LINK,
                    "L'incident antérieur peut être lié au même domaine technique que l'incident suivant."
            ));
        }
    }

    private boolean hasSameSignature(
            LogIncident source,
            LogIncident target) {

        if (source.getSignature() == null
                || target.getSignature() == null) {
            return false;
        }

        return source.getSignature().equals(target.getSignature());
    }

    private boolean hasSameComponent(
            LogIncident source,
            LogIncident target) {

        if (source.getMainClass() == null
                || target.getMainClass() == null) {
            return false;
        }

        String sourcePackage = extractPackage(source.getMainClass());
        String targetPackage = extractPackage(target.getMainClass());

        return sourcePackage.equals(targetPackage);
    }

    private boolean isTemporallyRelated(
            LogIncident source,
            LogIncident target) {

        if (source.getLastOccurrence() == null
                || target.getFirstOccurrence() == null) {
            return false;
        }

        if (target.getFirstOccurrence().isBefore(source.getLastOccurrence())) {
            return false;
        }

        long minutes = Duration.between(
                source.getLastOccurrence(),
                target.getFirstOccurrence()
        ).toMinutes();

        return minutes <= MAX_TEMPORAL_GAP_MINUTES;
    }

    private boolean isPossibleCausalLink(
            LogIncident source,
            LogIncident target) {

        if (source.getType() == null
                || target.getType() == null) {
            return false;
        }

        if (source.getLastOccurrence() == null
                || target.getFirstOccurrence() == null) {
            return false;
        }

        if (!target.getFirstOccurrence()
                .isAfter(source.getLastOccurrence())) {
            return false;
        }

        String sourceText = buildTechnicalText(source);
        String targetText = buildTechnicalText(target);

        boolean sameTechnicalDomain =
                containsCommonKeyword(sourceText, targetText);

        return sameTechnicalDomain
                && isTemporallyRelated(source, target);
    }

    private boolean containsCommonKeyword(
            String source,
            String target) {

        String[] keywords = {
                "oauth",
                "cognito",
                "proxy",
                "http",
                "spring",
                "security"
        };

        for (String keyword : keywords) {
            if (source.contains(keyword)
                    && target.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String buildTechnicalText(LogIncident incident) {

        StringBuilder text = new StringBuilder();

        append(text, incident.getType());
        append(text, incident.getLogger());
        append(text, incident.getMainClass());
        append(text, incident.getExceptionType());
        append(text, incident.getRootCauseType());

        return text.toString().toLowerCase();
    }

    private void append(StringBuilder text, Object value) {

        if (value != null) {
            text.append(value).append(' ');
        }
    }

    private String extractPackage(String className) {

        int lastDot = className.lastIndexOf('.');

        if (lastDot <= 0) {
            return className;
        }

        return className.substring(0, lastDot);
    }

    private IncidentRelation createRelation(
            LogIncident source,
            LogIncident target,
            IncidentRelationType type,
            String reason) {

        return new IncidentRelation(
                source,
                target,
                type,
                reason);
    }

}