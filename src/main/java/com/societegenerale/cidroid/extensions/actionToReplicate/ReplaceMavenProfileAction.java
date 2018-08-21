package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.societegenerale.cidroid.extensions.actionToReplicate.XMLUtils.prettyPrint;

/**
 * Given a profile name, will replace (or create it if it doesn't exist) the corresponding Maven profile with provided content. Full profile document is expected as input, starting at profile element, included.
 * <p>
 * Note : output XML will be encoded in UTF-8.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Slf4j
public class ReplaceMavenProfileAction implements ActionToReplicate {

    private String profileName;

    private String newProfileContent;

    @Override
    public void init(Map<String, String> updateActionInfos) {

        this.profileName = updateActionInfos.get("profileName");
        this.newProfileContent = updateActionInfos.get("newProfileContent");
    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextField("profileName", "profile name, to replace"),
                new TextField("newProfileContent", "new profile, starting with profile XML element"));
    }

    @Override
    public String getDescriptionForUI() {
        return "replace and existing Maven profile (or creates, if it doesn't exist)";
    }

    @Override
    public String provideContent(String initialContent, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {

        try {

            SAXReader reader = new SAXReader();

            Document doc=reader.read(new InputSource(new StringReader(initialContent)));

            List<Node> profilesRootSection = doc.selectNodes("//*[local-name()='project']/*[local-name()='profiles']");

            if(profilesRootSection.isEmpty()){
                //create profiles section

                log.warn("wasn't able to find existing existingProfiles section, or create one");
                //TODO throw proper exception
                return null;
            }

            List<Node> expectedProfileSection = doc.selectNodes("//*[local-name()='project']/*[local-name()='profiles']/*[local-name()='profile']/*[local-name()='id' and text()='"+profileName+"']");

            if(!expectedProfileSection.isEmpty()) {
                //remove profile first
                expectedProfileSection.get(0).getParent().detach();
            }

            Document profileToAdd=parseStringIntoDocument(newProfileContent,"profile to add");

            putDocumentToAddUnderSameNamespaceAsParent(profileToAdd, profilesRootSection.get(0).getParent());

            profilesRootSection.add(profileToAdd.getRootElement());


            return prettyPrint(doc);

        } catch (DocumentException | IOException e) {
            log.warn("problem while parsing pom.xml and/or modifying it", e);
        }

        return null;
    }

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
