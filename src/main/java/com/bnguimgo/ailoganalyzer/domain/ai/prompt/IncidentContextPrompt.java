package com.bnguimgo.ailoganalyzer.domain.ai.prompt;

import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;

public class IncidentContextPrompt {

    public String build(StructuredContext context) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("=== INCIDENTS ===\n");

        for (int i = 0; i < context.incidents().size(); i++) {

            LogIncident incident =
                    context.incidents().get(i);

            prompt.append("\nIncident ")
                    .append(i + 1)
                    .append(":\n");

            appendField(
                    prompt,
                    "Type",
                    incident.getType()
            );

            appendField(
                    prompt,
                    "Sévérité",
                    incident.getSeverity()
            );

            appendField(
                    prompt,
                    "Exception",
                    incident.getExceptionType()
            );

            appendField(
                    prompt,
                    "Message exception",
                    incident.getExceptionMessage()
            );

            appendField(
                    prompt,
                    "Cause racine",
                    incident.getRootCauseType()
            );

            appendField(
                    prompt,
                    "Message cause racine",
                    incident.getRootCauseMessage()
            );

            appendField(
                    prompt,
                    "Logger",
                    incident.getLogger()
            );

            appendField(
                    prompt,
                    "Thread",
                    incident.getThread()
            );

            appendField(
                    prompt,
                    "HTTP status",
                    incident.getHttpStatus()
            );

            appendField(
                    prompt,
                    "Classe principale",
                    incident.getMainClass()
            );

            appendField(
                    prompt,
                    "Méthode principale",
                    incident.getMainMethod()
            );

            appendField(
                    prompt,
                    "Ligne principale",
                    incident.getMainLine()
            );

            appendField(
                    prompt,
                    "Occurrences",
                    incident.getOccurrenceCount()
            );

            appendField(
                    prompt,
                    "Signature",
                    incident.getSignature()
            );
        }

        prompt.append("\n=== RELATIONS ENTRE INCIDENTS ===\n");

        for (IncidentRelation relation : context.relations()) {

            prompt.append("\nRelation : ")
                    .append(relation.getType())
                    .append("\n");

            appendField(
                    prompt,
                    "Raison",
                    relation.getReason()
            );
        }

        return prompt.toString();
    }

    private void appendField(
            StringBuilder prompt,
            String name,
            Object value) {

        prompt.append(name)
                .append(" : ")
                .append(value == null ? "N/A" : value)
                .append("\n");
    }
}