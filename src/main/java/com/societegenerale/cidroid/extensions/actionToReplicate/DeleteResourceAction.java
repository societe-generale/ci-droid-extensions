package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public class DeleteResourceAction implements ActionToReplicate {

    @Override
    public String provideContent(String s, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {
        throw new NotImplementedException("we are not supposed to call this method on "+this.getClass().getCanonicalName());
    }

    @Override
    public void init(Map<String, String> map) {
        //no param to init
    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {

        return emptyList();
    }

    @Override
    public String getDescriptionForUI() {
        return "delete the mentioned resource";
    }
}
