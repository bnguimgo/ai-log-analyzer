package com.bnguimgo.ailoganalyzer.application;

import com.bnguimgo.ailoganalyzer.domain.analysis.LogAnalysisReport;

import java.io.IOException;
import java.nio.file.Path;

public interface LogAnalysisService {

    LogAnalysisReport analyze(Path logFile) throws IOException;
}