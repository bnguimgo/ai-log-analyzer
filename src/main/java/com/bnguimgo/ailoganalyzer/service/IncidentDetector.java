package com.bnguimgo.ailoganalyzer.service;

import com.bnguimgo.ailoganalyzer.domain.incident.IncidentSeverity;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentType;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import com.bnguimgo.ailoganalyzer.domain.log.LogEvent;
import com.bnguimgo.ailoganalyzer.domain.log.LogLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IncidentDetector {

    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile(
                    "^([a-zA-Z0-9_.$]+(?:Exception|Error))"
                            + "(?::\\s*(.*))?"
                            + "(?:\\{.*)?$"
            );

    private static final Pattern STRUCTURED_EXCEPTION_PATTERN =
            Pattern.compile(
                    "^([a-zA-Z0-9_.$]+(?:Exception|Error))\\{(.*)}$"
            );

    private static final Pattern HTTP_STATUS_PROPERTY_PATTERN =
            Pattern.compile("httpStatus\\s*=\\s*(4\\d{2}|5\\d{2})");

    private static final Pattern HTTP_STATUS_MESSAGE_PATTERN =
            Pattern.compile(
                    "\\b(4\\d{2}|5\\d{2})\\s+"
                            + "(BAD_REQUEST|NOT_FOUND|UNAUTHORIZED|FORBIDDEN|"
                            + "INTERNAL_SERVER_ERROR|BAD_GATEWAY|SERVICE_UNAVAILABLE|"
                            + "GATEWAY_TIMEOUT)\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern STACK_FRAME_PATTERN =
            Pattern.compile("^\\s*at\\s+([a-zA-Z0-9_.$]+)\\.([a-zA-Z0-9_$]+)\\(([^)]*)\\)");

    public List<LogIncident> detect(List<LogEvent> events) {

        List<LogIncident> incidents = new ArrayList<>();

        for (LogEvent event : events) {

            if (!isIncident(event)) {
                continue;
            }

            LogIncident incident = createIncident(event);

            incidents.add(incident);
        }

        return incidents;
    }

    private boolean isIncident(LogEvent event) {
        return event.getLevel() == LogLevel.ERROR;
    }

    private LogIncident createIncident(LogEvent event) {

        LogIncident incident = new LogIncident();

        incident.setSeverity(IncidentSeverity.ERROR);
        incident.setType(detectType(event));

        incident.setFirstOccurrence(event.getTimestamp());
        incident.setLastOccurrence(event.getTimestamp());
        incident.setOccurrenceCount(1);

        incident.setLogger(event.getLogger());
        incident.setThread(event.getThread());

        extractException(event, incident);
        extractHttpStatus(event, incident);
        extractStackFrames(event, incident);

        return incident;
    }

    private IncidentType detectType(LogEvent event) {

        String text = buildSearchText(event).toLowerCase();

        if (text.contains("oauth2")
                || text.contains("oauth")
                || text.contains("cognito")) {
            return IncidentType.OAUTH2_ERROR;
        }

        if (text.contains("http")
                || text.contains("bad_request")) {
            return IncidentType.HTTP_ERROR;
        }

        if (text.contains("springframework")) {
            return IncidentType.SPRING_ERROR;
        }

        if (text.contains("startup")
                || text.contains("initializ")) {
            return IncidentType.APPLICATION_STARTUP;
        }

        return IncidentType.UNKNOWN;
    }

    private void extractException(LogEvent event, LogIncident incident) {
        String stackTrace = event.getStackTrace();

        if (stackTrace == null || stackTrace.trim().isEmpty()) {
            return;
        }

        String[] lines = stackTrace.split("\\R");

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("Caused by:")) {
                String cause = trimmed.substring("Caused by:".length()).trim();

                extractRootCause(cause, incident);
                continue;
            }

            if (incident.getExceptionType() == null) {
                extractMainException(trimmed, incident);
            }
        }
    }

    private void extractRootCause(String cause, LogIncident incident) {
        Matcher structuredMatcher =
                STRUCTURED_EXCEPTION_PATTERN.matcher(cause);

        if (structuredMatcher.matches()) {
            incident.setRootCauseType(
                    extractSimpleClassName(structuredMatcher.group(1))
            );
            incident.setRootCauseMessage(
                    structuredMatcher.group(2)
            );
            return;
        }

        Matcher matcher = EXCEPTION_PATTERN.matcher(cause);

        if (matcher.matches()) {
            incident.setRootCauseType(matcher.group(1));
            incident.setRootCauseMessage(matcher.group(2));
        }
    }

    private void extractMainException(String line, LogIncident incident) {
        Matcher structuredMatcher =
                STRUCTURED_EXCEPTION_PATTERN.matcher(line);

        if (structuredMatcher.matches()) {
            incident.setExceptionType(
                    extractSimpleClassName(structuredMatcher.group(1))
            );

            incident.setExceptionMessage(
                    structuredMatcher.group(2)
            );
            return;
        }

        Matcher matcher = EXCEPTION_PATTERN.matcher(line);

        if (matcher.matches()) {
            incident.setExceptionType(matcher.group(1));
            incident.setExceptionMessage(matcher.group(2));
        }
    }

    private String extractSimpleClassName(String className) {
        int lastDot = className.lastIndexOf('.');

        if (lastDot < 0) {
            return className;
        }

        return className.substring(lastDot + 1);
    }

    private void extractHttpStatus(LogEvent event, LogIncident incident) {
        String text = buildSearchText(event);

        Matcher propertyMatcher =
                HTTP_STATUS_PROPERTY_PATTERN.matcher(text);

        if (propertyMatcher.find()) {
            incident.setHttpStatus(
                    Integer.valueOf(propertyMatcher.group(1))
            );
            return;
        }

        Matcher messageMatcher =
                HTTP_STATUS_MESSAGE_PATTERN.matcher(text);

        if (messageMatcher.find()) {
            incident.setHttpStatus(
                    Integer.valueOf(messageMatcher.group(1))
            );
        }
    }

    private void extractStackFrames(LogEvent event, LogIncident incident) {
        String stackTrace = event.getStackTrace();

        if (stackTrace == null) {
            return;
        }

        String[] lines = stackTrace.split("\\R");

        for (String line : lines) {
            Matcher matcher = STACK_FRAME_PATTERN.matcher(line);

            if (!matcher.matches()) {
                continue;
            }

            String className = matcher.group(1);

            if (!incident.getInvolvedClasses().contains(className)) {
                incident.getInvolvedClasses().add(className);
            }

            if (incident.getMainClass() == null
                    && isApplicationClass(className)) {

                incident.setMainClass(className);
                incident.setMainMethod(matcher.group(2));

                String location = matcher.group(3);
                extractLineNumber(location, incident);
            }
        }
    }

    private boolean isApplicationClass(String className) {
        return className.startsWith("fr.pomona.");
    }

    private void extractLineNumber(
            String location,
            LogIncident incident) {

        Matcher matcher = Pattern.compile(":(\\d+)$")
                .matcher(location);

        if (matcher.find()) {
            incident.setMainLine(
                    Integer.valueOf(matcher.group(1))
            );
        }
    }

    private String buildSearchText(LogEvent event) {

        StringBuilder text = new StringBuilder();

        if (event.getMessage() != null) {
            text.append(event.getMessage()).append('\n');
        }

        if (event.getStackTrace() != null) {
            text.append(event.getStackTrace());
        }

        return text.toString();
    }
}