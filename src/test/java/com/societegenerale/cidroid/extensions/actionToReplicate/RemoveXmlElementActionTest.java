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

import static com.societegenerale.cidroid.extensions.actionToReplicate.RemoveXmlElementAction.XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class RemoveXmlElementActionTest {

    private RemoveXmlElementAction removeXmlElementAction = new RemoveXmlElementAction();

    private String rootWithNamespace="<?xml version=\"1.0\" encoding=\"UTF-8\"?><project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n";

    private String rootWithoutNamespace="<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>";


    private String coreContent = "\n" +
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

    }

    @Test
    public void shouldRemoveElementIfReferenceXPathIsFound_withNamespace() throws IssueProvidingContentException, IOException, SAXException {

        defineXpathElementToRemove("//*[local-name()='project']/*[local-name()='build']/*[local-name()='existingElementInBuild']");

        String actualResult = removeXmlElementAction.provideContent(rootWithNamespace+coreContent);

        log.info("actual result: " + actualResult);

        String expectedResult = rootWithNamespace +
                "           <dependencies></dependencies><build></build></project>";

        XMLAssert.assertXMLEqual(actualResult, expectedResult);
    }

    @Test
    public void shouldRemoveElementIfReferenceXPathIsFound_withoutNamespace() throws IssueProvidingContentException, IOException, SAXException {

        defineXpathElementToRemove("/project/build/existingElementInBuild");

        String actualResult = removeXmlElementAction.provideContent(rootWithoutNamespace+coreContent);

        log.info("actual result: " + actualResult);

        String expectedResult = rootWithoutNamespace +
                "           <dependencies></dependencies><build></build></project>";

        XMLAssert.assertXMLEqual(actualResult, expectedResult);
    }

    @Test
    public void shouldReturnSameDocumentWhenXPathNotFound() throws IssueProvidingContentException, IOException, SAXException {

        defineXpathElementToRemove("//project/unknownElement");

        String actualResult = removeXmlElementAction.provideContent(rootWithoutNamespace+coreContent);

        log.info("actual result: " + actualResult);

        XMLAssert.assertXMLEqual(actualResult, rootWithoutNamespace+coreContent);

    }

    @Test
    public void shouldThrowExceptionIfInputDocumentIsNotValidXml() {

        assertThatThrownBy(() -> {
            removeXmlElementAction.provideContent("<hello>");
        }).isInstanceOf(IssueProvidingContentException.class)
                .hasMessageContaining("original document");

    }

    private void defineXpathElementToRemove(String xpathElementToRemove){
        additionalInfosForInstantiation.put(XPATH_ELEMENT_THAT_NEEDS_TO_BE_REMOVED, xpathElementToRemove);

        removeXmlElementAction.init(additionalInfosForInstantiation);
    }

}