package com.bnguimgo.ailoganalyzer.infrastructure.ai.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DefaultLogSearchTool implements LogSearchTool {

    @Override
    public List<String> search(Path logFile, String searchTerm) throws IOException {

        if (logFile == null) {
            throw new IllegalArgumentException(
                    "Log file must not be null");
        }

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Search term must not be null or empty");
        }

        List<String> lines = Files.readAllLines(logFile);
        List<String> results = new ArrayList<String>();

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);

            if (line.contains(searchTerm)) {
                results.add(
                        "Ligne " + (i + 1) + " : " + line
                );
            }
        }

        return results;
    }
}