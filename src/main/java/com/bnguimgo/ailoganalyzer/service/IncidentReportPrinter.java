package com.bnguimgo.ailoganalyzer.service;

import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisResult;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class IncidentReportPrinter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public void print(LogAnalysisResult result) {

        List<LogIncident> incidents = result.incidents();
        List<IncidentRelation> relations = result.relations();

        System.out.println();
        System.out.println("==================================================");
        System.out.println("           RAPPORT D'ANALYSE DES LOGS");
        System.out.println("==================================================");
        System.out.println();
        System.out.println("Nombre de familles d'incidents : "
                + incidents.size());
        System.out.println();

        int index = 1;

        for (LogIncident incident : incidents) {
            printIncident(index, incident);
            index++;
        }

        System.out.println();
        System.out.println("--------------------------------------------------");
        System.out.println("RELATIONS ENTRE INCIDENTS");
        System.out.println("--------------------------------------------------");

        System.out.println(
                "Nombre de relations : " + relations.size()
        );

        for (int i = 0; i < relations.size(); i++) {

            IncidentRelation relation = relations.get(i);

            System.out.println();
            System.out.println("[" + (i + 1) + "] "
                    + relation.getType());

            System.out.println(
                    "    Source : "
                            + describeIncident(relation.getSource())
            );

            System.out.println(
                    "    Cible  : "
                            + describeIncident(relation.getTarget())
            );

            System.out.println(
                    "    Raison : "
                            + relation.getReason()
            );
        }
    }

    private String describeIncident(LogIncident incident) {

        return incident.getType()
                + " | "
                + incident.getSignature();
    }

    private void printIncident(int index, LogIncident incident) {

        String signature = incident.getSignature() != null
                ? incident.getSignature()
                : "NON_DEFINIE";

        System.out.println("[" + index + "] " + incident.getType());
        System.out.println("    Signature   : " + signature);
        System.out.println("    Occurrences : " + incident.getOccurrenceCount());
        System.out.println("    Sévérité    : " + incident.getSeverity());

        if (incident.getHttpStatus() != null) {
            System.out.println("    HTTP status : " + incident.getHttpStatus());
        }

        if (incident.getExceptionType() != null) {
            System.out.println("    Exception   : " + incident.getExceptionType());
        }

        if (incident.getRootCauseType() != null) {
            System.out.println("    Cause racine: " + incident.getRootCauseType());
        }

        if (incident.getRootCauseMessage() != null) {
            System.out.println("    Message     : " + incident.getRootCauseMessage());
        }

        if (incident.getFirstOccurrence() != null) {
            System.out.println("    Première occurrence : "
                    + incident.getFirstOccurrence().format(DATE_FORMATTER));
        }

        if (incident.getLastOccurrence() != null) {
            System.out.println("    Dernière occurrence  : "
                    + incident.getLastOccurrence().format(DATE_FORMATTER));
        }

        if (incident.getMainClass() != null) {
            System.out.println("    Classe principale : "
                    + incident.getMainClass());
        }

        if (incident.getMainMethod() != null) {
            System.out.println("    Méthode principale : "
                    + incident.getMainMethod());
        }

        if (incident.getMainLine() != null) {
            System.out.println("    Ligne principale : "
                    + incident.getMainLine());
        }

        System.out.println();
    }
}