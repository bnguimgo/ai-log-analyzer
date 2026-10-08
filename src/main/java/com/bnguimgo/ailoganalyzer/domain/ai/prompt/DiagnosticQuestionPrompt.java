package com.bnguimgo.ailoganalyzer.domain.ai.prompt;

public class DiagnosticQuestionPrompt implements PromptSection {

    @Override
    public String build() {

        StringBuilder prompt = new StringBuilder();

        prompt.append("\n=== QUESTION SPÉCIFIQUE ===\n");
        prompt.append("Analyse les erreurs HTTP 400 et détermine, à partir des logs bruts, ")
                .append("si elles sont liées à OAuth2/Cognito et quels éléments précis ")
                .append("permettent de l'établir.\n\n");

        return prompt.toString();
    }
}