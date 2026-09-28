package com.bnguimgo.ailoganalyzer.application;

import com.bnguimgo.ailoganalyzer.domain.ai.analyzer.AiAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DefaultLogAnalysisServiceSpringTest {

    @Autowired
    private LogAnalysisService logAnalysisService;

    @Autowired
    private AiAnalyzer aiAnalyzer;

    @Test
    void shouldCreateLogAnalysisServiceWithAiAnalyzer() {

        assertNotNull(logAnalysisService);
        assertNotNull(aiAnalyzer);
    }
}