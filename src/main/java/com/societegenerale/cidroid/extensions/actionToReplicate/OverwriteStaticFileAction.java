package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextArea;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OverwriteStaticFileAction implements ActionToReplicate {

    private String staticContent;

    @Override
    public String provideContent(String initialContent, ResourceToUpdate resourceToUpdate) {

        return staticContent;
    }

    @Override
    public void init(Map<String, String> updateActionInfos) {

        this.staticContent = updateActionInfos.get("staticContent");

    }

    @Override
    public boolean canContinueIfResourceDoesntExist() {
        return true;
    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextArea("staticContent", "content to overwrite/create"));
    }

    @Override
    public String getDescriptionForUI() {
        return "overwrite/create a file with given content";
    }
}
