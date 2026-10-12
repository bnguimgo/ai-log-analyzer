package com.bnguimgo.ailoganalyzer.domain.ai.prompt;

/**
 * Cette classe décrit juste le prompt
 */
public class PromptDescription {

    public String build(StringBuilder prompt) {

        prompt.append("Tu es un expert en analyse de logs Java/Spring.\n");
        prompt.append("Analyse les incidents suivants et identifie les ")
                .append("causes probables, les éléments de preuve et les ")
                .append("recommandations.\n\n");

        return prompt.toString();
    }
}