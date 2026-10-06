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

        prompt.append("Chaque recherche doit avoir un objectif de diagnostic précis : ")
                .append("avant d'appeler search_log, identifie explicitement ce que ")
                .append("tu cherches à démontrer ou à réfuter. Choisis ensuite un terme ")
                .append("de recherche aussi discriminant que possible par rapport à ")
                .append("cette hypothèse.\n");

        prompt.append("Évite les recherches génériques ou trop larges lorsqu'un terme ")
                .append("plus précis est disponible. Par exemple, privilégie le nom ")
                .append("d'une exception, d'une classe, d'une méthode, d'un message ")
                .append("d'erreur ou d'un élément technique directement lié à ")
                .append("l'hypothèse plutôt qu'un terme générique comme le nom d'un ")
                .append("framework, d'une bibliothèque, d'un composant ou d'une classe ")
                .append("très fréquente dans le log.\n");

        prompt.append("Après chaque recherche, évalue explicitement le résultat ")
                .append("par rapport à l'hypothèse : ")
                .append("CONFIRMÉE, INFIRMÉE ou INSUFFISANTE. ")
                .append("Si une preuve suffisante est obtenue, arrête les recherches ")
                .append("liées à cette hypothèse. ")
                .append("Si elle est infirmée, réévalue le diagnostic avant de lancer ")
                .append("une nouvelle recherche.\n");

        prompt.append("Une recherche doit toujours avoir un objectif de diagnostic précis. ")
                .append("Après avoir obtenu son résultat, détermine explicitement si ")
                .append("ce résultat apporte une preuve suffisante pour répondre à cet objectif.\n");

        prompt.append("Considère qu'une hypothèse est suffisamment étayée lorsqu'une ")
                .append("preuve directe présente dans le contexte ou dans les résultats ")
                .append("de search_log permet de la confirmer avec un niveau de confiance ")
                .append("raisonnable et qu'aucune information supplémentaire identifiable ")
                .append("dans le log n'est nécessaire pour établir la conclusion.\n");

        prompt.append("Si la preuve est suffisante, arrête les recherches liées à cette ")
                .append("hypothèse et passe à la conclusion.\n");

        prompt.append("Avant toute nouvelle recherche, compare le résultat obtenu ")
                .append("à ta conclusion actuelle. N'effectue une nouvelle recherche ")
                .append("que si tu peux identifier une information précise dont ")
                .append("l'absence empêche encore de confirmer ou d'infirmer la conclusion ")
                .append("et si cette information pourrait réellement modifier le diagnostic.\n");

        prompt.append("Si le résultat d'une recherche est insuffisant ou vide et qu'aucune ")
                .append("information précise supplémentaire susceptible de modifier le ")
                .append("diagnostic ne peut être identifiée, arrête les recherches.\n");

        prompt.append("Ne lance pas une nouvelle recherche uniquement parce qu'un terme ")
                .append("technique semble pertinent ou parce qu'il pourrait apporter ")
                .append("davantage de contexte. Une recherche supplémentaire doit avoir ")
                .append("un impact potentiel explicite sur la conclusion.\n");

        prompt.append("Si les preuves disponibles permettent déjà d'établir la conclusion ")
                .append("principale, arrête les recherches même si certaines informations ")
                .append("secondaires restent inconnues. Indique alors explicitement ces ")
                .append("informations comme des limites du diagnostic.\n");

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