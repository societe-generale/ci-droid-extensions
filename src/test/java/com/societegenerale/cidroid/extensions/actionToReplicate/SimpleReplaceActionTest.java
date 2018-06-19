package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleReplaceActionTest {

    @Test
    public void shouldFinalizeBuild() {

        SimpleReplaceAction action = new SimpleReplaceAction();

        Map<String, String> additionalInfosForInstantiation = new HashMap<>();

        additionalInfosForInstantiation.put("initialValue", "before");
        additionalInfosForInstantiation.put("newValue", "after");

        action.init(additionalInfosForInstantiation);

        assertThat(action.getInitialValue()).isEqualTo("before");
        assertThat(action.getNewValue()).isEqualTo("after");
    }



    @Test
    public void shouldReplaceAllOccurrencesOfMatchedContent() throws IssueProvidingContentException {

        SimpleReplaceAction simpleReplaceAction = new SimpleReplaceAction("to", "ta");

        String transformedContent = simpleReplaceAction.provideContent("Where is this Tomato going to ?");

        assertThat(transformedContent).isEqualTo("Where is this Tomata going ta ?");

    }

    @Test
    public void specialCharactersHaveToBeEscaped() throws IssueProvidingContentException {

        SimpleReplaceAction simpleReplaceAction = new SimpleReplaceAction("fpl-\\$\\{ENVNAME}.myDomain.com/authentication",
                "fpl-\\$\\{ENVNAME}-authentication.myDomain.com");

        String transformedContent = simpleReplaceAction.provideContent("# APPLICATION PROPERTIES\n" +
                "# ----------------------------------------\n" +
                "ogn:\n" +
                "  security:\n" +
                "    mode: OAUTH2\n" +
                "    oauth2.user-info-uri: ${AUTH_SERVICE_URL:https://fpl-${ENVNAME}.myDomain.com/authentication/user}\n" +
                "  service.access.scheme: https\n" +
                "  environment.name: ${ENVNAME}");

        assertThat(transformedContent).contains("fpl-${ENVNAME}-authentication.myDomain.com");

    }
}