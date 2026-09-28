package com.bnguimgo.ailoganalyzer.service;

import com.bnguimgo.ailoganalyzer.domain.incident.IncidentType;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import com.bnguimgo.ailoganalyzer.domain.log.LogEvent;
import com.bnguimgo.ailoganalyzer.domain.log.LogLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncidentDetectorTest {

    @Test
    void shouldDetectRootCause() {

        LogEvent event = getLogEvent();

        IncidentDetector detector = new IncidentDetector();

        List<LogIncident> incidents =
                detector.detect(Collections.singletonList(event));

        assertEquals(1, incidents.size());

        LogIncident incident = incidents.getFirst();

        assertEquals(
                IncidentType.OAUTH2_ERROR,
                incident.getType()
        );

        assertEquals(
                "org.springframework.beans.factory.UnsatisfiedDependencyException",
                incident.getExceptionType()
        );

        assertEquals(
                "java.lang.IllegalArgumentException",
                incident.getRootCauseType()
        );

        assertEquals(
                "Host name may not be null",
                incident.getRootCauseMessage()
        );

        assertEquals(
                "fr.pomona.manageo.web.security.cognito.proxy.AuthHeaderGenerator",
                incident.getMainClass()
        );

        assertEquals(
                "createProxyConfiguration",
                incident.getMainMethod()
        );

        assertEquals(
                Integer.valueOf(62),
                incident.getMainLine()
        );
    }

    private static LogEvent getLogEvent() {
        LogEvent event = new LogEvent();

        event.setTimestamp(
                LocalDateTime.of(
                        2026, 9, 16,
                        14, 45, 36, 982000000
                )
        );

        event.setLevel(LogLevel.ERROR);

        event.setThread("main");

        event.setLogger(
                "org.apache.catalina.core.StandardContext.listenerStart"
        );

        event.setMessage(
                "Exception lors de l'envoi de l'évènement contexte initialisé"
        );

        event.setStackTrace(
                """
                        org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'absenceRepository'
                        \tat fr.pomona.manageo.web.security.cognito.proxy.AuthHeaderGenerator.createProxyConfiguration(AuthHeaderGenerator.java:62)
                        Caused by: java.lang.IllegalArgumentException: Host name may not be null
                        \tat org.apache.http.HttpHost.<init>(HttpHost.java:80)
                        \tat fr.pomona.manageo.web.security.cognito.proxy.AuthHeaderGenerator.createProxyConfiguration(AuthHeaderGenerator.java:62)"""
        );
        return event;
    }

    @Test
    void shouldDetectHttpStatus() {

        LogEvent event = new LogEvent();

        event.setTimestamp(LocalDateTime.now());
        event.setLevel(LogLevel.ERROR);
        event.setThread("http-nio-8080-exec-7");
        event.setLogger(
                "org.apache.catalina.core.StandardWrapperValve.invoke"
        );

        event.setMessage(
                "\"Servlet.service()\" threw exception"
        );

        event.setStackTrace(
                """
                        fr.pomona.manageo.web.security.CustomHttpResponseException{httpStatus=400 BAD_REQUEST}
                        \tat fr.pomona.manageo.web.security.CustomHttpResponseErrorHandler.handleError(CustomHttpResponseErrorHandler.java:27)
                        \tat fr.pomona.manageo.web.security.cognito.proxy.oauth2.CustomOAuth2AccessTokenResponseClient.getTokenResponse(CustomOAuth2AccessTokenResponseClient.java:35)"""
        );

        IncidentDetector detector = new IncidentDetector();

        List<LogIncident> incidents =
                detector.detect(Collections.singletonList(event));

        assertEquals(1, incidents.size());

        LogIncident incident = incidents.getFirst();

        assertEquals(
                Integer.valueOf(400),
                incident.getHttpStatus()
        );

        assertEquals(
                IncidentType.OAUTH2_ERROR,
                incident.getType()
        );
    }

    @Test
    void shouldIgnoreInfoEvents() {

        LogEvent event = new LogEvent();

        event.setTimestamp(LocalDateTime.now());
        event.setLevel(LogLevel.INFO);
        event.setMessage("Application started");

        IncidentDetector detector = new IncidentDetector();

        List<LogIncident> incidents =
                detector.detect(Collections.singletonList(event));

        assertTrue(incidents.isEmpty());
    }

    @Test
    void shouldNotDetectHttpStatusFromStackTraceLineNumber() {
        LogEvent event = new LogEvent();

        event.setTimestamp(
                LocalDateTime.of(2026, 9, 16, 14, 45, 36)
        );
        event.setLevel(LogLevel.ERROR);
        event.setThread("main");
        event.setLogger("test");
        event.setMessage("Une erreur est survenue");
        event.setStackTrace(
                "org.springframework.beans.factory.UnsatisfiedDependencyException\n"
                        + "\tat org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor"
                        + "$AutowiredFieldElement.resolveFieldValue(AutowiredAnnotationBeanPostProcessor.java:408)"
        );

        IncidentDetector detector = new IncidentDetector();

        List<LogIncident> incidents =
                detector.detect(Collections.singletonList(event));

        assertEquals(1, incidents.size());

        assertNull(incidents.getFirst().getHttpStatus());
    }

    @Test
    void shouldSelectFirstApplicationStackFrameAsMainClass() {
        LogEvent event = getEvent();

        IncidentDetector detector = new IncidentDetector();

        List<LogIncident> incidents =
                detector.detect(Collections.singletonList(event));

        assertEquals(1, incidents.size());

        LogIncident incident = incidents.getFirst();

        assertEquals(
                "fr.pomona.manageo.web.security.cognito.proxy.AuthHeaderGenerator",
                incident.getMainClass()
        );

        assertEquals(
                "createProxyConfiguration",
                incident.getMainMethod()
        );

        assertEquals(
                62,
                incident.getMainLine()
        );
    }

    @Test
    void shouldExtractStructuredHttpException() {
        LogEvent event = extractLogEvent();

        IncidentDetector detector = new IncidentDetector();

        List<LogIncident> incidents =
                detector.detect(Collections.singletonList(event));

        assertEquals(1, incidents.size());

        LogIncident incident = incidents.getFirst();

        assertEquals(
                "CustomHttpResponseException",
                incident.getExceptionType()
        );

        assertEquals(
                Integer.valueOf(400),
                incident.getHttpStatus()
        );
    }

    private static LogEvent extractLogEvent() {
        LogEvent event = new LogEvent();

        event.setTimestamp(
                LocalDateTime.of(2026, 9, 16, 19, 23, 11)
        );
        event.setLevel(LogLevel.ERROR);
        event.setThread("http-nio-8080-exec-1");
        event.setLogger("test");
        event.setMessage("\"Servlet.service()\" pour la servlet [application] "
                + "a généré une exception");

        event.setStackTrace(
                "fr.pomona.manageo.web.security.CustomHttpResponseException"
                        + "{httpStatus=400 BAD_REQUEST}"
        );
        return event;
    }

    private static LogEvent getEvent() {
        LogEvent event = new LogEvent();

        event.setTimestamp(
                LocalDateTime.of(2026, 9, 16, 14, 45, 36)
        );
        event.setLevel(LogLevel.ERROR);
        event.setThread("main");
        event.setLogger("test");
        event.setMessage("Erreur de démarrage");

        event.setStackTrace(
                """
                        org.springframework.beans.factory.UnsatisfiedDependencyException
                        \tat org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement.resolveFieldValue(AutowiredAnnotationBeanPostProcessor.java:713)
                        \tat fr.pomona.manageo.web.security.cognito.proxy.AuthHeaderGenerator.createProxyConfiguration(AuthHeaderGenerator.java:62)
                        \tat fr.pomona.manageo.web.security.cognito.config.CognitoSecurityConfiguration.oauth2ClientRestOperations(CognitoSecurityConfiguration.java:120)"""
        );
        return event;
    }
}