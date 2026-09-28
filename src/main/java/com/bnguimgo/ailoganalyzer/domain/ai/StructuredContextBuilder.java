package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class StructuredContextBuilder {

    public StructuredContext build(LogAnalysisResult analysisResult) {

        if (analysisResult == null) {
            throw new IllegalArgumentException(
                    "analysisResult must not be null"
            );
        }

        return new StructuredContext(
                analysisResult.incidents(),
                analysisResult.relations(),
                analysisResult.logFile()
        );
    }
}