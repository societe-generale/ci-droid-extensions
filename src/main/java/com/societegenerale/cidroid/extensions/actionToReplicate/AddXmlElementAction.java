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
import java.util.*;

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

        Document originalDocument = parseStringIntoDocument(documentToProcess);

        List<Node> elementUnderXpathWeLookFor = originalDocument.selectNodes(xpathUnderWhichElementNeedsToBeAdded);

        if (elementUnderXpathWeLookFor.isEmpty()) {
            return documentToProcess;
        }

        Node lastNodeMatching = elementUnderXpathWeLookFor.get(elementUnderXpathWeLookFor.size() - 1);

        List<Document> documentsToAdd = parseStringIntoDocuments(elementToAdd, ELEMENT_TO_ADD);

        Element lastElementInOriginalDocument = (Element) lastNodeMatching;

        for (Document documentToAdd : documentsToAdd) {
            putDocumentToAddUnderSameNamespaceAsParent(documentToAdd, lastElementInOriginalDocument);
            lastElementInOriginalDocument.add(documentToAdd.getRootElement());
        }

        return prettyPrint(originalDocument);

    }

    protected List<Document> parseStringIntoDocuments(String documentToProcess, String elementInError) throws IssueProvidingContentException {

        List<String> endBlocksForRootElements = endBlocksForRootElements(documentToProcess);

        List<Document> documents = new ArrayList<>();

        SAXReader reader = new SAXReader();

        try {

            for (int i = 0; i < endBlocksForRootElements.size(); i++) {

                String endBlock = endBlocksForRootElements.get(i);

                String xmlBlock = documentToProcess.substring(0, documentToProcess.indexOf(endBlock) + endBlock.length());

                Document doc = reader.read(new InputSource(new StringReader(xmlBlock)));

                documents.add(doc);

                if (i != endBlocksForRootElements.size() - 1) {
                    documentToProcess = documentToProcess.substring(documentToProcess.indexOf(endBlock) + endBlock.length() + 1);
                }
            }

            return documents;
        } catch (DocumentException e) {
            throw new IssueProvidingContentException("issue while parsing " + elementInError + " - is it a valid XML doc ?", e);
        }

    }

    private List<String> endBlocksForRootElements(String documentToProcess) throws IssueProvidingContentException {

        List<String> endBlocks = new ArrayList<>();

        List<XMLEvent> nextBlock = Collections.emptyList();

        do {
            nextBlock = readNextXmlBlock(documentToProcess);

            if (!nextBlock.isEmpty()) {

                System.out.println("got new block, containing " + nextBlock.size() + " events");

                String closingEvent = nextBlock.get(nextBlock.size() - 1).toString();

                endBlocks.add(closingEvent);

                documentToProcess = documentToProcess.substring(documentToProcess.indexOf(closingEvent) + closingEvent.length());
            }

        }
        while (!documentToProcess.isEmpty() && !nextBlock.isEmpty());

        return endBlocks;

    }

    private List<XMLEvent> readNextXmlBlock(String documentToProcess) throws IssueProvidingContentException {

        List<XMLEvent> eventsFormingABlock = new ArrayList<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;

        try {
            reader = factory.createXMLEventReader(IOUtils.toInputStream(documentToProcess, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        String currentElement = null;

        while (reader.hasNext()) {
            final XMLEvent event;

            try {
                event = reader.nextEvent();
            } catch (XMLStreamException e) {

                if(e.getMessage().contains("The markup in the document following the root element must be well-formed")){
                    log.info("problem while parsing document  - but it's very likely because there are several root elements, it will be handled", e);
                    break;
                }
                else{
                    throw new IssueProvidingContentException("issue while parsing "+ELEMENT_TO_ADD+" "+documentToProcess+" - is it a valid XML doc ?", e);
                }

            }

            System.out.println("event : " + event.toString());

            if (event.isEndDocument()) {
                break;
            }

            if (event.isStartElement() && currentElement == null) {
                currentElement = event.asStartElement().getName()
                        .getLocalPart();

               // eventsFormingABlock.add(event);
            } else if (event.isEndElement()) {

                if (event.asEndElement().getName().getLocalPart().equals(currentElement)) {

                    eventsFormingABlock.add(event);

                    String closingElement = "</" + currentElement + ">";

                    String restOfDocumentToProcess = documentToProcess.substring(documentToProcess.indexOf(closingElement) + closingElement.length());

                    currentElement = null;

                    System.out.println("rest of doc: " + restOfDocumentToProcess);
                }
            } else if (currentElement != null) {
                eventsFormingABlock.add(event);
            }
        }

        return eventsFormingABlock;
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
