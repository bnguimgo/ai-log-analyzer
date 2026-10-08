package com.bnguimgo.ailoganalyzer.domain.ai.prompt;

import com.bnguimgo.ailoganalyzer.domain.ai.StructuredContext;

/**
 * AiPromptBuilder a un rôle très précis : transformer notre contexte technique structuré en une consigne compréhensible par un LLM.
 */
public class AiPromptBuilder {

    private final SearchPolicyPrompt searchPolicyPrompt;
    private final IncidentContextPrompt incidentContextPrompt;
    private final DiagnosticQuestionPrompt diagnosticQuestionPrompt;
    private final AnalysisOutputPrompt analysisOutputPrompt;

    public AiPromptBuilder() {
        this.searchPolicyPrompt = new SearchPolicyPrompt();
        this.incidentContextPrompt = new IncidentContextPrompt();
        this.diagnosticQuestionPrompt = new DiagnosticQuestionPrompt();
        this.analysisOutputPrompt = new AnalysisOutputPrompt();
    }

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

        //=== RÈGLE D'UTILISATION DE SEARCH_LOG ===
        prompt.append(searchPolicyPrompt.build());

        //=== INCIDENTS ET RELATIONS ENTRE INCIDENTS ===
        prompt.append(incidentContextPrompt.build(context));

        //=== QUESTION SPÉCIFIQUE ===
        prompt.append(diagnosticQuestionPrompt.build());

        //=== ATTENDU ===
        prompt.append(analysisOutputPrompt.build());

        return prompt.toString();
    }

}