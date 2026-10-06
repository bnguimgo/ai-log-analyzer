package com.bnguimgo.ailoganalyzer.domain.ai;

import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelation;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentRelationType;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentSeverity;
import com.bnguimgo.ailoganalyzer.domain.incident.IncidentType;
import com.bnguimgo.ailoganalyzer.domain.incident.LogIncident;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AiPromptBuilderTest {

    private static final Path LOG_FILE_PATH = Paths.get("src/test/resources/logs/manageo-cognito.log");

    @Test
    void shouldBuildPromptContainingIncidentInformation() {

        LogIncident incident = getLogIncident();

        StructuredContext context =
                new StructuredContext(
                        Collections.singletonList(incident),
                        Collections.emptyList(),
                        LOG_FILE_PATH
                );

        AiPromptBuilder builder =
                new AiPromptBuilder();

        String prompt = builder.build(context);

        assertTrue(prompt.contains("OAUTH2_ERROR"));
        assertTrue(prompt.contains("ERROR"));
        assertTrue(prompt.contains(
                "org.springframework.beans.factory.UnsatisfiedDependencyException"
        ));
        assertTrue(prompt.contains(
                "java.lang.IllegalArgumentException"
        ));
        assertTrue(prompt.contains(
                "Host name may not be null"
        ));
        assertTrue(prompt.contains("400"));
        assertTrue(prompt.contains(
                "AuthHeaderGenerator"
        ));
        assertTrue(prompt.contains(
                "createProxyConfiguration"
        ));
        assertTrue(prompt.contains("62"));
        assertTrue(prompt.contains("5"));
        assertTrue(prompt.contains(
                "OAUTH2_ERROR|CustomHttpResponseException|UNKNOWN|400"
        ));
    }

    private static LogIncident getLogIncident() {
        LogIncident incident = new LogIncident();

        incident.setType(IncidentType.OAUTH2_ERROR);
        incident.setSeverity(IncidentSeverity.ERROR);
        incident.setExceptionType(
                "org.springframework.beans.factory.UnsatisfiedDependencyException"
        );
        incident.setExceptionMessage(
                "Error creating bean"
        );
        incident.setRootCauseType(
                "java.lang.IllegalArgumentException"
        );
        incident.setRootCauseMessage(
                "Host name may not be null"
        );
        incident.setLogger(
                "fr.pomona.manageo.web.security.cognito.proxy.AuthHeaderGenerator"
        );
        incident.setThread("main");
        incident.setHttpStatus(400);
        incident.setMainClass(
                "fr.pomona.manageo.web.security.cognito.proxy.AuthHeaderGenerator"
        );
        incident.setMainMethod("createProxyConfiguration");
        incident.setMainLine(62);
        incident.setOccurrenceCount(5);
        incident.setSignature(
                "OAUTH2_ERROR|CustomHttpResponseException|UNKNOWN|400"
        );
        return incident;
    }

    @Test
    void shouldBuildPromptContainingRelations() {

        LogIncident source = new LogIncident();
        source.setType(IncidentType.OAUTH2_ERROR);

        LogIncident target = new LogIncident();
        target.setType(IncidentType.HTTP_ERROR);

        StructuredContext context = getStructuredContext(source, target);

        AiPromptBuilder builder =
                new AiPromptBuilder();

        String prompt = builder.build(context);

        assertTrue(prompt.contains("TEMPORALLY_RELATED"));
        assertTrue(prompt.contains(
                "Les deux incidents sont proches dans le temps."
        ));
    }

    private static StructuredContext getStructuredContext(LogIncident source, LogIncident target) {
        IncidentRelation relation =
                new IncidentRelation(
                        source,
                        target,
                        IncidentRelationType.TEMPORALLY_RELATED,
                        "Les deux incidents sont proches dans le temps."
                );

        return new StructuredContext(
                        Arrays.asList(source, target),
                        Collections.singletonList(relation),
                        LOG_FILE_PATH
                );
    }

    @Test
    void shouldRejectNullContext() {

        AiPromptBuilder builder =
                new AiPromptBuilder();

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.build(null)
        );
    }

    @Test
    void shouldInstructAiToEvaluateSearchResultBeforeContinuing() {

        StructuredContext context = new StructuredContext(
                Collections.emptyList(),
                Collections.emptyList(),
                LOG_FILE_PATH
        );

        AiPromptBuilder builder = new AiPromptBuilder();

        String prompt = builder.build(context);

        assertTrue(prompt.contains("CONFIRMÉE"));
        assertTrue(prompt.contains("INFIRMÉE"));
        assertTrue(prompt.contains("INSUFFISANTE"));

        assertTrue(prompt.contains("Après chaque recherche"));
        assertTrue(prompt.contains("évalue"));
        assertTrue(prompt.contains("arrête les recherches"));
        assertTrue(prompt.contains("réévalue le diagnostic"));
    }

/*    @Test
    void shouldIncludeEvidenceStoppingRule() {
        StructuredContext context = new StructuredContext(
                Collections.emptyList(),
                Collections.emptyList(),
                LOG_FILE_PATH
        );

        AiPromptBuilder builder = new AiPromptBuilder();

        String prompt = builder.build(context);

        assertTrue(prompt.contains(
                "Si la preuve est suffisante, arrête les recherches"
        ));

        assertTrue(prompt.contains(
                "Si aucune recherche supplémentaire précise et utile ne peut être identifiée"
        ));

        assertTrue(prompt.contains(
                "la preuve disponible dans le log est insuffisante"
        ));
    }*/

/*    @Test
    void shouldRequireNewSearchToBePotentiallyDiagnostic() {

        StructuredContext context = new StructuredContext(
                Collections.emptyList(),
                Collections.emptyList(),
                LOG_FILE_PATH
        );

        AiPromptBuilder aiPromptBuilder = new AiPromptBuilder();

        String prompt = aiPromptBuilder.build(context);

        assertTrue(
                prompt.contains(
                        "Avant toute nouvelle recherche, compare le résultat obtenu à ta conclusion actuelle"
                )
        );

        assertTrue(
                prompt.contains(
                        "cette information pourrait réellement modifier le diagnostic"
                )
        );

        assertTrue(
                prompt.contains(
                        "considère cette piste comme épuisée"
                )
        );
    }*/
}