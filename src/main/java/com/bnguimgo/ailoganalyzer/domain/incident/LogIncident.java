package com.bnguimgo.ailoganalyzer.domain.incident;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LogIncident {

    private IncidentType type;
    private IncidentSeverity severity;

    private LocalDateTime firstOccurrence;
    private LocalDateTime lastOccurrence;
    private String signature;

    private int occurrenceCount;

    private String exceptionType;
    private String rootCauseType;
    private String rootCauseMessage;

    private String logger;
    private String thread;

    private Integer httpStatus;

    private String mainClass;
    private String mainMethod;
    private Integer mainLine;

    private String exceptionMessage;

    private final List<String> involvedClasses = new ArrayList<String>();

    public IncidentType getType() {
        return type;
    }

    public void setType(IncidentType type) {
        this.type = type;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public LocalDateTime getFirstOccurrence() {
        return firstOccurrence;
    }

    public void setFirstOccurrence(LocalDateTime firstOccurrence) {
        this.firstOccurrence = firstOccurrence;
    }

    public LocalDateTime getLastOccurrence() {
        return lastOccurrence;
    }

    public void setLastOccurrence(LocalDateTime lastOccurrence) {
        this.lastOccurrence = lastOccurrence;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getRootCauseType() {
        return rootCauseType;
    }

    public void setRootCauseType(String rootCauseType) {
        this.rootCauseType = rootCauseType;
    }

    public String getRootCauseMessage() {
        return rootCauseMessage;
    }

    public void setRootCauseMessage(String rootCauseMessage) {
        this.rootCauseMessage = rootCauseMessage;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(String mainMethod) {
        this.mainMethod = mainMethod;
    }

    public Integer getMainLine() {
        return mainLine;
    }

    public void setMainLine(Integer mainLine) {
        this.mainLine = mainLine;
    }

    public List<String> getInvolvedClasses() {
        return involvedClasses;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }
}