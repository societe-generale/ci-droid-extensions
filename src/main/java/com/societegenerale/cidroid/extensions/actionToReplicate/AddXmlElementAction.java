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
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An action that will look for an xpath element, and if found, will add an element in it, in last position (if there are existing children)
 */
@Data
@NoArgsConstructor
@Slf4j
@ToString
public class AddXmlElementAction extends AbstractXmlProcessingAction implements ActionToReplicate {

    protected static final String ELEMENT_TO_ADD = "elementToAdd";

    protected static final String XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED = "xpathUnderWhichElementNeedsToBeAdded";

    private String elementToAdd;

    private String xpathUnderWhichElementNeedsToBeAdded;

    @Override
    public String provideContent(String documentToProcess, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {

        Document originalDocument = parseStringIntoDocument(documentToProcess, "original document");

        List<Node> elementUnderXpathWeLookFor = originalDocument.selectNodes(xpathUnderWhichElementNeedsToBeAdded);

        if (elementUnderXpathWeLookFor.isEmpty()) {
            return documentToProcess;
        }

        Node lastNodeMatching = elementUnderXpathWeLookFor.get(elementUnderXpathWeLookFor.size() - 1);

        Document documentToAdd = parseStringIntoDocument(elementToAdd, ELEMENT_TO_ADD);

        Element lastElementInOriginalDocument = (Element) lastNodeMatching;

        putDocumentToAddUnderSameNamespaceAsParent(documentToAdd, lastElementInOriginalDocument);

        lastElementInOriginalDocument.add(documentToAdd.getRootElement());

        return prettyPrint(originalDocument);

    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextField(ELEMENT_TO_ADD, "the XML element to add"),
                new TextField(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED, "The Xpath under which the element needs to be added"));
    }

    @Override
    public String getDescriptionForUI() {
        return "Add an XML element under a given XPath";
    }

    @Override
    public void init(Map<String, String> updateActionInfos) {

        this.elementToAdd = updateActionInfos.get(ELEMENT_TO_ADD);
        this.xpathUnderWhichElementNeedsToBeAdded = updateActionInfos.get(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED);
    }

}
