package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextArea;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An action that will look for an xpath element, and if found, will add the provided element(s) in it, in last position (if there are existing children).
 */
@Data
@NoArgsConstructor
@Slf4j
@ToString
public class AddXmlContentAction extends AbstractXmlProcessingAction implements ActionToReplicate {

    protected static final String ELEMENT_TO_ADD = "elementToAdd";

    protected static final String XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED = "xpathUnderWhichElementNeedsToBeAdded";

    private String elementToAdd;

    private String xpathUnderWhichElementNeedsToBeAdded;

    @Override
    public String provideContent(String documentToProcess, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {

        Document originalDocument = parseStringIntoDocument(documentToProcess);

        List<Node> elementUnderXpathWeLookFor = originalDocument.selectNodes(xpathUnderWhichElementNeedsToBeAdded);

        if (elementUnderXpathWeLookFor.isEmpty()) {
            return documentToProcess;
        }

        Node lastNodeMatching = elementUnderXpathWeLookFor.get(elementUnderXpathWeLookFor.size() - 1);

        Element lastElementInOriginalDocument = (Element) lastNodeMatching;

        List<Document> documentsToAdd = parseStringIntoDocuments(elementToAdd.trim());

        addDocumentsAfter(documentsToAdd, lastElementInOriginalDocument);

        return prettyPrint(originalDocument);

    }

    private void addDocumentsAfter(List<Document> documentsToAdd, Element lastElementInOriginalDocument) {

        for (Document documentToAdd : documentsToAdd) {
            putDocumentToAddUnderSameNamespaceAsParent(documentToAdd, lastElementInOriginalDocument);
            lastElementInOriginalDocument.add(documentToAdd.getRootElement());
        }

    }

    protected List<Document> parseStringIntoDocuments(String documentToProcess) throws IssueProvidingContentException {

        List<String> endBlocksForRootElements = findEndBlocksForRootElements(documentToProcess);

        List<Document> parsedDocumentsFromProvidedGlobalDoc = new ArrayList<>();

        SAXReader reader = new SAXReader();

        try {

            for (int i = 0; i < endBlocksForRootElements.size(); i++) {

                String endBlock = endBlocksForRootElements.get(i);

                String standaloneXmlBlock = findFirstXmlBlockEndingWithElement(documentToProcess, endBlock);

                Document documentForTheBlock = reader.read(new InputSource(new StringReader(standaloneXmlBlock)));

                parsedDocumentsFromProvidedGlobalDoc.add(documentForTheBlock);

                if (notTheLastBlockBeingProcessed(endBlocksForRootElements, i)) {
                    documentToProcess = removeParsedBlockFromGlobalDocument(documentToProcess, endBlock);
                }
            }

            return parsedDocumentsFromProvidedGlobalDoc;

        } catch (DocumentException e) {
            throw new IssueProvidingContentException("issue while parsing " + ELEMENT_TO_ADD + " - is it a valid XML doc ?", e);
        }

    }

    private boolean notTheLastBlockBeingProcessed(List<String> endBlocksForRootElements, int indexOfElementBeingProcessed) {
        return (indexOfElementBeingProcessed != endBlocksForRootElements.size() - 1);
    }

    private String findFirstXmlBlockEndingWithElement(String documentToParse, String endElement) {
        return documentToParse.substring(0, documentToParse.indexOf(endElement) + endElement.length());
    }

    private String removeParsedBlockFromGlobalDocument(String globalDocument, String endElement) {
        return globalDocument.substring(globalDocument.indexOf(endElement) + endElement.length());
    }

    private List<String> findEndBlocksForRootElements(String documentToProcess) throws IssueProvidingContentException {

        List<String> endBlocksForEachElementAtRootLevel = new ArrayList<>();

        while (thereIsStillSomethingToParse(documentToProcess)) {
            String closingElementOfNextXmlBlockEvents = findClosingElementOfNextXmlBlockEvents(documentToProcess);

            endBlocksForEachElementAtRootLevel.add(closingElementOfNextXmlBlockEvents);

            documentToProcess = removeParsedBlockFromGlobalDocument(documentToProcess, closingElementOfNextXmlBlockEvents);
        }

        return endBlocksForEachElementAtRootLevel;
    }

    private boolean thereIsStillSomethingToParse(String documentToProcess) {
        return !documentToProcess.isEmpty();
    }

    private String findClosingElementOfNextXmlBlockEvents(String documentToProcess) throws IssueProvidingContentException {

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;

        try {
            reader = factory.createXMLEventReader(IOUtils.toInputStream(documentToProcess, "UTF-8"));
        } catch (XMLStreamException | IOException e) {
            log.warn("problem while parsing this document : " + documentToProcess, e);
        }

        String currentElement = null;

        while (reader.hasNext()) {
            final XMLEvent event;

            try {
                event = reader.nextEvent();
            } catch (XMLStreamException e) {

                //not great.. but didn't find a better way for now. In case we try to add several elements at once, an XMLStreamException will be thrown, with below message
                //Apart from exception message content, there's no way to differentiate between a really malformed xml String (missing closing element for ex) and a doc with 2 elements at the root (which in our case should be considered valid)
                if (e.getMessage().contains("The markup in the document following the root element must be well-formed")) {
                    log.info("problem while parsing document  - but it's very likely because there are several root elements, it will be handled", e);
                    break;
                } else {
                    throw new IssueProvidingContentException(
                            "issue while parsing " + ELEMENT_TO_ADD + " " + documentToProcess + " - is it a valid XML doc ?", e);
                }
            }

            if (event.isEndDocument()) {
                break;
            }

            if (eventIsFirstElement(event, currentElement)) {
                currentElement = event.asStartElement().getName().getLocalPart();
            }
            else if (eventIsTheClosingElementOfFirstElement(currentElement, event)) {
                return event.toString();
            }

        }

        throw new IssueProvidingContentException("couldn't find a closing element for "+currentElement+". Parsed document was "+documentToProcess);
    }

    private boolean eventIsTheClosingElementOfFirstElement(String currentElement, XMLEvent event) {
        return event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(currentElement);
    }

    private boolean eventIsFirstElement(XMLEvent event, String currentElement) {
        return event.isStartElement() && currentElement == null;
    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextField(ELEMENT_TO_ADD, "the XML element to add"),
                             new TextArea(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED, "The Xpath under which the element needs to be added"));
    }

    @Override
    public String getDescriptionForUI() {
        return "Adds some XML content under a given XPath";
    }

    @Override
    public void init(Map<String, String> updateActionInfos) {

        this.elementToAdd = updateActionInfos.get(ELEMENT_TO_ADD);
        this.xpathUnderWhichElementNeedsToBeAdded = updateActionInfos.get(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED);
    }

}
