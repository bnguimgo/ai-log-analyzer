package com.bnguimgo.ailoganalyzer.domain.ai;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class AiAnalysisResponseTest {

    @Test
    void shouldCreateCompleteAiAnalysisResponse() {

        AiRootCause rootCause = new AiRootCause();
        rootCause.setDescription("Configuration du proxy incorrecte");
        rootCause.setEvidence("Host name may not be null");
        rootCause.setConfidence("HIGH");

        AiAnalysisResponse response = getAiAnalysisResponse(rootCause);

        assertEquals(
                "L'application rencontre une erreur lors de la configuration du proxy.",
                response.getSummary()
        );

        assertEquals(1, response.getProbableCauses().size());
        assertEquals(1, response.getRecommendations().size());
        assertEquals(1, response.getUncertainties().size());
    }

    private static AiAnalysisResponse getAiAnalysisResponse(AiRootCause rootCause) {
        AiRecommendation recommendation = new AiRecommendation();
        recommendation.setDescription(
                "Vérifier la configuration du proxy HTTP"
        );
        recommendation.setRationale(
                "Le host du proxy semble absent de la configuration."
        );

        AiAnalysisResponse response = new AiAnalysisResponse();

        response.setSummary(
                "L'application rencontre une erreur lors de la configuration du proxy."
        );

        response.setProbableCauses(
                Collections.singletonList(rootCause)
        );

        response.setRecommendations(
                Collections.singletonList(recommendation)
        );

        response.setUncertainties(
                Collections.singletonList(
                        "La configuration effective du proxy n'a pas été vérifiée."
                )
        );
        return response;
    }
}
