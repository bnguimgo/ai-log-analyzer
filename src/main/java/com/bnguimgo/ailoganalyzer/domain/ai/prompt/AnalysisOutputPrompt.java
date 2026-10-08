package com.bnguimgo.ailoganalyzer.domain.ai.prompt;

public class AnalysisOutputPrompt implements PromptSection {


    @Override
    public String build() {

        StringBuilder prompt = new StringBuilder();

        prompt.append("\n=== ATTENDU ===\n");
        prompt.append("1. Résume le problème principal.\n");
        prompt.append("2. Identifie les causes probables.\n");
        prompt.append("3. Donne les éléments de preuve associés.\n");
        prompt.append("4. Propose des recommandations concrètes.\n");
        prompt.append("5. Signale explicitement les incertitudes.\n");

        return prompt.toString();
    }

}
