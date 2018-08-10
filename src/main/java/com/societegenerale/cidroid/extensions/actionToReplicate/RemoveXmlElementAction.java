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
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.societegenerale.cidroid.extensions.actionToReplicate.XMLUtils.prettyPrint;

/**
 * An action that will look for an xpath element, and if found, will remove it from the document.
 *
 * <b>Caveat</b> : if you use this action for an XML document with a namespace (typically, a pom.xml),
 * you need to provide an {@link RemoveXmlElementAction#xpathElementToRemove} that takes this into account.
 *<br>
 * For example, See unit test class, or https://stackoverflow.com/questions/10813653/xpath-select-node-based-in-a-condition-with-local-name
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@Slf4j
public class RemoveXmlElementAction implements ActionToReplicate {

    protected static final String XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED = "xpathElementToRemove";

    private String xpathElementToRemove;

    @Override
    public String provideContent(String documentToProcess, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {

        Document originalDocument;

        SAXReader reader = new SAXReader();

        try {
            originalDocument= reader.read(new InputSource(new StringReader(documentToProcess)));
        } catch (DocumentException e) {
            throw new IssueProvidingContentException("issue while parsing original document - is it a valid XML doc ?", e);
        }

        List<Node> elementUnderXpathWeLookFor = originalDocument.selectNodes(xpathElementToRemove);

        if (elementUnderXpathWeLookFor.isEmpty()) {
            log.info(xpathElementToRemove+" didn't match any element - not removing any element");
            return documentToProcess;
        }

        elementUnderXpathWeLookFor.stream().forEach(e -> e.detach());

        try {
            return prettyPrint(originalDocument);
        }
        catch(IOException e){
            throw new IssueProvidingContentException("problem while writing the new content during processing", e);
        }

    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextField(XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED, "The Xpath to be removed"));
    }

    @Override
    public void init(Map<String, String> updateActionInfos) {

        this.xpathElementToRemove = updateActionInfos.get(XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED);
    }
}
