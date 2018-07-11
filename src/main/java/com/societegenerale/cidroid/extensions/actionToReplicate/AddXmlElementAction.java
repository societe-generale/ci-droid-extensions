package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextField;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.societegenerale.cidroid.extensions.actionToReplicate.XMLUtils.prettyPrint;

/**
 * An action that will look for an xpath element, and if found, will add an element in it, in last position (if there are existing children)
 */
public class AddXmlElementAction implements ActionToReplicate {

    protected static final String ELEMENT_TO_ADD = "elementToAdd";

    protected static final String XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED = "xpathUnderWhichElementNeedsToBeAdded";

    private String elementToAdd;

    private String xpathUnderWhichElementNeedsToBeAdded;

    @Override
    public String provideContent(String documentToProcess, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {

        Document originalDocument=parseStringIntoDocument(documentToProcess,"original document");

        List<Node> elementUnderXpathWeLookFor = originalDocument.selectNodes(xpathUnderWhichElementNeedsToBeAdded);

        if (elementUnderXpathWeLookFor.isEmpty()) {
            return documentToProcess;
        }

        Node lastNodeMatching = elementUnderXpathWeLookFor.get(elementUnderXpathWeLookFor.size() - 1);

        Document documentToAdd=parseStringIntoDocument(elementToAdd,ELEMENT_TO_ADD);

        Element lastElementInOriginalDocument = (Element) lastNodeMatching;
        lastElementInOriginalDocument.add(documentToAdd.getRootElement());

        try {
            return prettyPrint(originalDocument);
        }
        catch(IOException e){
            throw new IssueProvidingContentException("problem while writing the new content during processing", e);
        }

    }

    private Document parseStringIntoDocument(String documentToProcess, String elementInError) throws IssueProvidingContentException {

        SAXReader reader = new SAXReader();

        try {
            return reader.read(new InputSource(new StringReader(documentToProcess)));
        } catch (DocumentException e) {
            throw new IssueProvidingContentException("issue while parsing "+elementInError+" - is it a valid XML doc ?", e);
        }

    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextField(ELEMENT_TO_ADD, "the XML element to add"),
                new TextField(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED, "The Xpath under which the element needs to be added"));
    }

    @Override
    public void init(Map<String, String> updateActionInfos) {

        this.elementToAdd = updateActionInfos.get(ELEMENT_TO_ADD);
        this.xpathUnderWhichElementNeedsToBeAdded = updateActionInfos.get(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED);
    }
}
