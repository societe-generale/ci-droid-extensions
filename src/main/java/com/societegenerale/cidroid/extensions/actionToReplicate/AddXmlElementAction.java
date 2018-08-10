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
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.societegenerale.cidroid.extensions.actionToReplicate.XMLUtils.prettyPrint;

/**
 * An action that will look for an xpath element, and if found, will add an element in it, in last position (if there are existing children)
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@Slf4j
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

        putDocumentToAddUnderSameNamespaceAsParent(documentToAdd, lastElementInOriginalDocument);

        lastElementInOriginalDocument.add(documentToAdd.getRootElement());

        try {
            return prettyPrint(originalDocument);
        }
        catch(IOException e){
            throw new IssueProvidingContentException("problem while writing the new content during processing", e);
        }

    }

    /**
     * This is necessary, otherwise documentToAdd will be added in default namespace, and some unexpected data will be in the output.
     * Therefore, if documentToAdd elements don't have a namespace defined, we change it to the namespace of the document in which we insert documentToAdd
     * @param documentToAdd
     * @param lastElementInOriginalDocument
     */
    private void putDocumentToAddUnderSameNamespaceAsParent(Document documentToAdd, Element lastElementInOriginalDocument) {
        Namespace parentNamespace=lastElementInOriginalDocument.getNamespace();
        documentToAdd.accept(new NamespaceChangingVisitor(Namespace.NO_NAMESPACE, parentNamespace));
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

    /**
     * from https://stackoverflow.com/questions/1492428/javadom-how-do-i-set-the-base-namespace-of-an-already-created-document
     */
    private class NamespaceChangingVisitor extends VisitorSupport {
        private Namespace from;
        private Namespace to;

        public NamespaceChangingVisitor(Namespace from, Namespace to) {
            this.from = from;
            this.to = to;
        }

        public void visit(Element node) {
            Namespace ns = node.getNamespace();

            if (ns.getURI().equals(from.getURI())) {
                QName newQName = new QName(node.getName(), to);
                node.setQName(newQName);
            }

            ListIterator namespaces = node.additionalNamespaces().listIterator();
            while (namespaces.hasNext()) {
                Namespace additionalNamespace = (Namespace) namespaces.next();
                if (additionalNamespace.getURI().equals(from.getURI())) {
                    namespaces.remove();
                }
            }
        }

    }

}
