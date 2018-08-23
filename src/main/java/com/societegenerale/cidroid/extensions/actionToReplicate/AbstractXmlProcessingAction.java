package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ListIterator;

public abstract class AbstractXmlProcessingAction {

    protected final String ORIGINAL_DOCUMENT="original document";

    /**
     * This is necessary, otherwise documentToAdd will be added in default namespace, and some unexpected data will be in the output.
     * Therefore, if documentToAdd elements don't have a namespace defined, we change it to the namespace of the document in which we insert documentToAdd
     * @param documentToAdd
     * @param lastElementInOriginalDocument
     */
    protected void putDocumentToAddUnderSameNamespaceAsParent(Document documentToAdd, Element lastElementInOriginalDocument) {
        Namespace parentNamespace=lastElementInOriginalDocument.getNamespace();
        documentToAdd.accept(new AddXmlElementAction.NamespaceChangingVisitor(Namespace.NO_NAMESPACE, parentNamespace));
    }

    protected Document parseStringIntoDocument(String documentToProcess, String elementInError) throws IssueProvidingContentException {

        SAXReader reader = new SAXReader();

        try {
            return reader.read(new InputSource(new StringReader(documentToProcess)));
        } catch (DocumentException e) {
            throw new IssueProvidingContentException("issue while parsing "+elementInError+" - is it a valid XML doc ?", e);
        }

    }

    protected Document parseStringIntoDocument(String documentToProcess) throws IssueProvidingContentException {

       return parseStringIntoDocument(documentToProcess,ORIGINAL_DOCUMENT);

    }

    /**
     * from https://stackoverflow.com/questions/1492428/javadom-how-do-i-set-the-base-namespace-of-an-already-created-document
     */
    protected class NamespaceChangingVisitor extends VisitorSupport {
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

    public String prettyPrint(Document originalDocument) throws IssueProvidingContentException {

        StringWriter sw = new StringWriter();

        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(sw, format);

        try {
            writer.write(originalDocument);
        }  catch(IOException e){
            throw new IssueProvidingContentException("problem while writing the new content during processing", e);
        }

        return sw.toString();
    }

}
