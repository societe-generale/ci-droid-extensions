package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OverwriteStaticFileActionTest {

    @Test
    public void shouldFinalizeBuild() {

        OverwriteStaticFileAction action = new OverwriteStaticFileAction();

        Map<String, String> additionalInfosForInstantiation = new HashMap<>();

        additionalInfosForInstantiation.put("staticContent", "theNewContent");

        action.init(additionalInfosForInstantiation);

        assertThat(action.getStaticContent()).isEqualTo("theNewContent");

    }


    @Test
    public void shouldReplaceWholeContent() throws IssueProvidingContentException {

        String newContent = "this is the static replacement content";

        OverwriteStaticFileAction overwriteStaticFileAction = new OverwriteStaticFileAction(newContent);

        String transformedContent = overwriteStaticFileAction.provideContent("this was previous content");

        assertThat(transformedContent).isEqualTo(newContent);

    }

}