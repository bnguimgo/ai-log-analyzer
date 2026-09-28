package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface LogSearchTool {

    List<String> search(Path logFile, String searchTerm) throws IOException;
}