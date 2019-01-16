package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeleteResourceActionTest {

    DeleteResourceAction action = new DeleteResourceAction();

    @Before
    public void setup(){
        action.init(emptyMap());
    }

    @Test
    public void shouldHaveNoConfigParam() {
        assertThat(action.getExpectedUIFields()).isEmpty();
    }


    @Test
    public void shouldThrowExceptionWhenAskedToProvideContent() throws IssueProvidingContentException {
        assertThatThrownBy(() -> { action.provideContent("some content"); }).isInstanceOf(NotImplementedException.class);
    }

}