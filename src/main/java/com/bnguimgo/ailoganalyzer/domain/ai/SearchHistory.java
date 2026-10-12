package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.infrastructure.ai.tools.ToolExecutionResult;

import java.util.HashMap;
import java.util.Map;

public class SearchHistory {

    private final Map<String, ToolExecutionResult> searches = new HashMap<>();

    public boolean contains(SearchRequest request) {
        return searches.containsKey(request.searchTerm());
    }

    public void add(SearchRequest request, ToolExecutionResult result) {

        searches.put(request.searchTerm(), result);
    }

    public ToolExecutionResult findResult(SearchRequest request) {
        return searches.get(request.searchTerm());
    }
}