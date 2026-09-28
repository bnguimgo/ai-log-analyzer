package com.bnguimgo.ailoganalyzer.domain.ai;

import java.util.List;

public class AiAnalysisResponse {

    private String summary;
    private List<AiRootCause> probableCauses;
    private List<AiRecommendation> recommendations;
    private List<String> uncertainties;

    // getters / setters

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<AiRootCause> getProbableCauses() {
        return probableCauses;
    }

    public void setProbableCauses(List<AiRootCause> probableCauses) {
        this.probableCauses = probableCauses;
    }

    public List<AiRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<AiRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public List<String> getUncertainties() {
        return uncertainties;
    }

    public void setUncertainties(List<String> uncertainties) {
        this.uncertainties = uncertainties;
    }
}