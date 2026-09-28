package com.bnguimgo.ailoganalyzer.parser;

import com.bnguimgo.ailoganalyzer.domain.log.LogEvent;
import com.bnguimgo.ailoganalyzer.domain.log.LogLevel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogParser {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss.SSS", java.util.Locale.ENGLISH);

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{2}-[A-Za-z]{3}-\\d{4} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})"
                    + "\\s+(\\S+)"
                    + "\\s+\\[([^]]+)]"
                    + "\\s+(\\S+)"
                    + "\\s+(.*)$"
    );

    public List<LogEvent> parse(List<String> lines) {

        List<LogEvent> events = new ArrayList<>();

        LogEvent currentEvent = null;
        StringBuilder stackTrace = new StringBuilder();

        for (String line : lines) {

            Matcher matcher = LOG_PATTERN.matcher(line);

            if (matcher.matches()) {

                if (currentEvent != null) {
                    currentEvent.setStackTrace(stackTrace.toString());
                    events.add(currentEvent);
                }

                currentEvent = new LogEvent();

                currentEvent.setTimestamp(LocalDateTime.parse(matcher.group(1), DATE_FORMATTER));
                currentEvent.setLevel(parseLevel(matcher.group(2)));
                currentEvent.setThread(matcher.group(3));
                currentEvent.setLogger(matcher.group(4));
                currentEvent.setMessage(matcher.group(5));

                stackTrace = new StringBuilder();

            } else if (currentEvent != null) {

                if (!stackTrace.isEmpty()) {
                    stackTrace.append(System.lineSeparator());
                }

                stackTrace.append(line);
            }
        }

        if (currentEvent != null) {
            currentEvent.setStackTrace(stackTrace.toString());
            events.add(currentEvent);
        }

        return events;
    }

    private LogLevel parseLevel(String value) {

        return switch (value.toUpperCase()) {
            case "TRACE" -> LogLevel.TRACE;
            case "DEBUG" -> LogLevel.DEBUG;
            case "INFO", "INFOS" -> LogLevel.INFO;
            case "WARN", "WARNING" -> LogLevel.WARN;
            case "ERROR", "GRAVE" -> LogLevel.ERROR;
            default -> LogLevel.UNKNOWN;
        };
    }
}