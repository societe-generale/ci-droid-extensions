package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextField;
import lombok.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class SimpleReplaceAction implements ActionToReplicate {

    private String initialValue;

    private String newValue;

    @Override
    public String provideContent(String initialContent, ResourceToUpdate resourceToUpdate) {

        return initialContent.replaceAll(initialValue, newValue);
    }

    @Override
    public String getDescriptionForUI() {
        return "simple replace in the file";
    }

    @Override
    public void init(Map<String, String> updateActionInfos) {
        this.initialValue = updateActionInfos.get("initialValue");
        this.newValue = updateActionInfos.get("newValue");
    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextField("initialValue", "old value, to replace"),
                new TextField("newValue", "new value"));
    }

}
