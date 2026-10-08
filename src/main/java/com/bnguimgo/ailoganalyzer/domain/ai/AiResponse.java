package com.bnguimgo.ailoganalyzer.domain.ai;

import java.util.List;

public class AiResponse {

    public enum Type {
        TEXT,
        FUNCTION_CALL
    }

    private Type type;
    private String text;
    private String responseId;

    private List<AiFunctionCall> functionCalls;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getResponseId() { return responseId; }

    public void setResponseId(String responseId) { this.responseId = responseId; }

    public List<AiFunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    public void setFunctionCalls(List<AiFunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
    }
}