package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import com.bnguimgo.ailoganalyzer.domain.ai.SearchRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface LogSearchTool {

    List<String> search(Path logFile, SearchRequest searchRequest) throws IOException;
}