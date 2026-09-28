package com.bnguimgo.ailoganalyzer.domain.ai;

public class AiResponse {

    public enum Type {
        TEXT,
        FUNCTION_CALL
    }

    private Type type;
    private String text;
    private String responseId;
    private String callId;
    private String functionName;
    private String arguments;

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

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
}