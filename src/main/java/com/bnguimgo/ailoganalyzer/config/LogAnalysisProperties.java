package com.bnguimgo.ailoganalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ai.log-analysis")
public class LogAnalysisProperties {

    private List<String> applicationPackages = new ArrayList<>();

    public List<String> getApplicationPackages() {
        return applicationPackages;
    }

    public void setApplicationPackages(List<String> applicationPackages) {
        this.applicationPackages = applicationPackages;
    }
}