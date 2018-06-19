package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import org.junit.Before;
import org.junit.Test;

import static com.societegenerale.cidroid.extensions.actionToReplicate.TemplateBasedContentAction.PLACEHOLDER_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TemplateBasedContentActionTest {

    TemplateBasedContentAction templateBasedContentAction = new TemplateBasedContentAction();

    ResourceToUpdate resourceToUpdate = new ResourceToUpdate();

    @Before
    public void setup() {
        resourceToUpdate.setPlaceHolderValue("specificValue");
    }

    @Test
    public void shouldProvideContentWithReplacedPlaceHolder() throws IssueProvidingContentException {

        templateBasedContentAction.setTemplatedContent("some content with a ${placeHolderValue} in the middle");

        String actualContent = templateBasedContentAction.provideContent(null, resourceToUpdate);

        assertThat(actualContent).isEqualTo("some content with a specificValue in the middle");
    }

    @Test
    public void shouldThrowExceptionIfPlaceHolderNotFoundInTemplatedContent() {

        templateBasedContentAction.setTemplatedContent("some content with a ${someincorrectPlaceholderValue} in the middle");

        assertThatThrownBy(() -> {
            templateBasedContentAction.provideContent(null, resourceToUpdate);
        })
                .isInstanceOf(IssueProvidingContentException.class)
                .hasMessageContaining("templated content didn't contain the expected placeholder value, ie " + PLACEHOLDER_VALUE);

    }

}