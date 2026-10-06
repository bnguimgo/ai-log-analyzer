package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;

/**
 * AiPromptBuilder a un rôle très précis : transformer notre contexte technique structuré en une consigne compréhensible par un LLM.
 */
public class AiPromptBuilder {

    public String build(StructuredContext context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "context must not be null"
            );
        }

        StringBuilder prompt = new StringBuilder();

        prompt.append("Tu es un expert en analyse de logs Java/Spring.\n");
        prompt.append("Analyse les incidents suivants et identifie les ")
                .append("causes probables, les éléments de preuve et les ")
                .append("recommandations.\n\n");

        prompt.append("=== RÈGLE D'UTILISATION DE SEARCH_LOG ===\n");

        prompt.append("Si une conclusion ou une affirmation nécessite de vérifier ")
                .append("un élément qui n'est pas suffisamment démontré par le ")
                .append("contexte structuré fourni, utilise search_log pour rechercher ")
                .append("les preuves correspondantes dans le contenu brut du log ")
                .append("avant de conclure.\n");

        prompt.append("Lorsque search_log est nécessaire, appelle directement l'outil ")
                .append("sans demander confirmation à l'utilisateur et sans simplement ")
                .append("annoncer que tu vas effectuer la recherche.\n");

        prompt.append("N'affirme pas comme un fait une information qui n'est pas ")
                .append("démontrée par le contexte structuré ou par les résultats ")
                .append("de search_log.\n");

        prompt.append("Utilise search_log notamment lorsque tu dois vérifier ")
                .append("le message exact d'une erreur, rechercher une exception ")
                .append("ou sa cause, vérifier les détails d'une requête ou d'une ")
                .append("réponse HTTP, confirmer un enchaînement d'événements, ")
                .append("ou rechercher un élément nécessaire pour étayer une hypothèse.\n");

        prompt.append("Si les informations disponibles sont suffisantes pour établir ")
                .append("une conclusion, n'utilise pas search_log inutilement.\n\n");

        prompt.append("Une fois que tu disposes de suffisamment d'éléments de preuve ")
                .append("pour répondre à la question, arrête les recherches et produis ")
                .append("directement l'analyse finale. Ne multiplie pas les recherches ")
                .append("pour explorer le log sans objectif précis.\n\n");

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

        prompt.append("\n=== QUESTION SPÉCIFIQUE ===\n");
        prompt.append("Analyse les erreurs HTTP 400 et détermine, à partir des logs bruts, ")
                .append("si elles sont liées à OAuth2/Cognito et quels éléments précis ")
                .append("permettent de l'établir.\n\n");

        prompt.append("\n=== ATTENDU ===\n");
        prompt.append("1. Résume le problème principal.\n");
        prompt.append("2. Identifie les causes probables.\n");
        prompt.append("3. Donne les éléments de preuve associés.\n");
        prompt.append("4. Propose des recommandations concrètes.\n");
        prompt.append("5. Signale explicitement les incertitudes.\n");

        /*prompt.append("\n=== TEST TOOL CALLING ===\n");
        prompt.append("Pour cette analyse, tu dois obligatoirement utiliser l'outil search_log au moins une fois. ");
        prompt.append("Utilise-le pour rechercher exactement le texte suivant dans le fichier de log : ");
        prompt.append("\"Host name may not be null\".\n");*/

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