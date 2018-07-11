package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import lombok.extern.slf4j.Slf4j;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.societegenerale.cidroid.extensions.actionToReplicate.AddXmlElementAction.XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED;
import static com.societegenerale.cidroid.extensions.actionToReplicate.RemoveXmlElementAction.XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class RemoveXmlElementActionTest {

    private RemoveXmlElementAction removeXmlElementAction = new RemoveXmlElementAction();

    private String initialXmlDoc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>\n" +
            "\n" +
            "\t<dependencies>\n" +
            "\n" +
            "\t</dependencies>\n" +
            "\n" +
            "\t<build>\n" +
            "\n" +
            "\t\t<existingElementInBuild>\n" +
            "\t\t\tinteresting stuff !!\n" +
            "\t\t</existingElementInBuild>\n" +
            "\t\t\n" +
            "\t</build>\n" +
            "\n" +
            "</project>";

    private Map<String, String> additionalInfosForInstantiation = new HashMap<>();

    @Before
    public void setup() {

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

        additionalInfosForInstantiation.put(XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED, "//project/build/existingElementInBuild");

        removeXmlElementAction.init(additionalInfosForInstantiation);

    }

    @Test
    public void shouldRemoveElementIfReferenceXPathIsFound() throws IssueProvidingContentException, IOException, SAXException {

        String actualResult = removeXmlElementAction.provideContent(initialXmlDoc);

        log.info("actual result: " + actualResult);

        String expectedResult = "<project><dependencies></dependencies><build></build></project>";

        XMLAssert.assertXMLEqual(actualResult, expectedResult);
    }

    @Test
    public void shouldReturnSameDocumentWhenXPathNotFound() throws IssueProvidingContentException, IOException, SAXException {

        additionalInfosForInstantiation.put(XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED, "//project/unknownElement");

        removeXmlElementAction.init(additionalInfosForInstantiation);

        String actualResult = removeXmlElementAction.provideContent(initialXmlDoc);

        log.info("actual result: " + actualResult);

        XMLAssert.assertXMLEqual(actualResult, initialXmlDoc);

    }

    @Test
    public void shouldThrowExceptionIfInputDocumentIsNotValidXml() {

        assertThatThrownBy(() -> {
            removeXmlElementAction.provideContent("<hello>");
        }).isInstanceOf(IssueProvidingContentException.class)
                .hasMessageContaining("original document");

    }

}