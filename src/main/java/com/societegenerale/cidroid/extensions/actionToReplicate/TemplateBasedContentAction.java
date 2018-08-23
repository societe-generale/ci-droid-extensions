package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextArea;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString
public class TemplateBasedContentAction implements ActionToReplicate {

    protected final static String PLACEHOLDER_VALUE = "\\$\\{placeHolderValue}";

    private String templatedContent;

    @Override
    public String provideContent(String initialContent, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {

        String result = templatedContent.replaceAll(PLACEHOLDER_VALUE, resourceToUpdate.getPlaceHolderValue());

        if (result.equals(templatedContent)) {
            throw new IssueProvidingContentException("templated content didn't contain the expected placeholder value, ie " + PLACEHOLDER_VALUE);
        }

        return result;
    }

    @Override
    public void init(Map<String, String> actionDetails) {
        this.templatedContent = actionDetails.get("newProfileContent");
    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextArea("templatedContent", "template to use"));
    }

    @Override
    public String getDescriptionForUI() {
        return "writes a file, based on a template and placeholder value";
    }
}
