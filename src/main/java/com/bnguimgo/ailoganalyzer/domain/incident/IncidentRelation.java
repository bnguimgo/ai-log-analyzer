package com.bnguimgo.ailoganalyzer.domain.incident;

public class IncidentRelation {

    private LogIncident source;
    private LogIncident target;
    private IncidentRelationType type;
    private String reason;

    public IncidentRelation(LogIncident source, LogIncident target, IncidentRelationType incidentRelationType, String reason) {
        this.source = source;
        this.target = target;
        this.type = incidentRelationType;
        this.reason = reason;
    }

    public LogIncident getSource() {
        return source;
    }

    public void setSource(LogIncident source) {
        this.source = source;
    }

    public LogIncident getTarget() {
        return target;
    }

    public void setTarget(LogIncident target) {
        this.target = target;
    }

    public IncidentRelationType getType() {
        return type;
    }

    public void setType(IncidentRelationType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}