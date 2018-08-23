package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An action that will look for an xpath element, and if found, will remove it from the document.
 *
 * <b>Caveat</b> : if you use this action for an XML document with a namespace (typically, a pom.xml),
 * you need to provide an {@link RemoveXmlElementAction#xpathElementToRemove} that takes this into account.
 * <br>
 * For example, See unit test class, or https://stackoverflow.com/questions/10813653/xpath-select-node-based-in-a-condition-with-local-name
 */
@Data
@NoArgsConstructor
@ToString
@Slf4j
public class RemoveXmlElementAction extends AbstractXmlProcessingAction implements ActionToReplicate {

    protected static final String XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED = "xpathElementToRemove";

    private String xpathElementToRemove;

    @Override
    public String provideContent(String documentToProcess, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {

        Document originalDocument = parseStringIntoDocument(documentToProcess);

        List<Node> elementUnderXpathWeLookFor = originalDocument.selectNodes(xpathElementToRemove);

        if (elementUnderXpathWeLookFor.isEmpty()) {
            log.info(xpathElementToRemove + " didn't match any element - not removing any element");
            return documentToProcess;
        }

        elementUnderXpathWeLookFor.stream().forEach(e -> e.detach());

        return prettyPrint(originalDocument);
    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextField(XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED, "The Xpath to be removed"));
    }

    @Override
    public String getDescriptionForUI() {
        return "remove an XML element identified by its XPath";
    }

    @Override
    public void init(Map<String, String> updateActionInfos) {

        this.xpathElementToRemove = updateActionInfos.get(XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED);
    }
}
