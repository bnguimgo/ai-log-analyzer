package com.bnguimgo.ailoganalyzer.parser;

import com.bnguimgo.ailoganalyzer.domain.log.LogEvent;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogParserTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldParseTomcatLog() throws Exception {

        LogReader reader = new LogReader();
        LogParser parser = new LogParser();

        List<String> lines = reader.read(LOG_FILE_PATH);

        List<LogEvent> events = parser.parse(lines);

        assertFalse(events.isEmpty());

        LogEvent first = events.getFirst();

        assertEquals("main", first.getThread());
        assertEquals("org.apache.catalina.core.ApplicationContext.log", first.getLogger());

        assertEquals("INFO", first.getLevel().name());

        assertTrue(first.getMessage().contains("Spring WebApplicationInitializers detected"));
    }

    @Test
    void shouldKeepMultilineStackTrace() throws Exception {

        LogReader reader = new LogReader();
        LogParser parser = new LogParser();

        List<String> lines = reader.read(LOG_FILE_PATH);

        List<LogEvent> events = parser.parse(lines);

        LogEvent errorEvent = events.stream()
                .filter(event -> event.getMessage().contains("Exception lors de l'envoi"))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals("ERROR", errorEvent.getLevel().name());

        assertTrue(errorEvent.getStackTrace().contains("UnsatisfiedDependencyException"));

        assertTrue(errorEvent.getStackTrace().contains("Host name may not be null"));

        assertTrue(errorEvent.getStackTrace().contains("Caused by:"));
    }
}