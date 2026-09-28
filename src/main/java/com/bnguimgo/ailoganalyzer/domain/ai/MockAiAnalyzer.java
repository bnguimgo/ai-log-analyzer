package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.AiAnalyzer;

import java.util.Collections;

/**
 * Cette classe sera remplacée par l'IA future.
 * Il s'agit d'un mock des résultats de l'analyse faite l'IA
 */
public class MockAiAnalyzer implements AiAnalyzer {

    @Override
    public AiAnalysisResponse analyze(StructuredContext context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "context must not be null"
            );
        }

        AiAnalysisResponse response = new AiAnalysisResponse();

        response.setSummary(
                "Analyse simulée de "
                        + context.incidents().size()
                        + " famille(s) d'incidents."
        );

        response.setProbableCauses(Collections.emptyList());
        response.setRecommendations(Collections.emptyList());
        response.setUncertainties(Collections.emptyList());

        return response;
    }
}