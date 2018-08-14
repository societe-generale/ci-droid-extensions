package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.societegenerale.cidroid.extensions.actionToReplicate.AddXmlElementAction.ELEMENT_TO_ADD;
import static com.societegenerale.cidroid.extensions.actionToReplicate.AddXmlElementAction.XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;

@Slf4j
public class AddXmlElementActionTest {

    private AddXmlElementAction addXmlElementAction = new AddXmlElementAction();

    private MavenXpp3Reader pomModelreader = new MavenXpp3Reader();

    private final Charset OUTPUT_ENCODING= StandardCharsets.UTF_8;

    private String rootWithNamespace="<?xml version=\"1.0\" encoding=\"UTF-8\"?><project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n";

    private String rootWithoutNamespace="<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>";

    private String coreContent =
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

        additionalInfosForInstantiation.put(ELEMENT_TO_ADD, "<element>Hello World</element>");
        additionalInfosForInstantiation.put(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED, "//project/build");

        addXmlElementAction.init(additionalInfosForInstantiation);

    }

    @Test
    public void shouldThrowExceptionIfInputDocumentIsNotValidXml() {

        assertThatThrownBy(() -> {
            addXmlElementAction.provideContent("<hello>");
        }).isInstanceOf(IssueProvidingContentException.class)
                .hasMessageContaining("original document");

    }

    @Test
    public void shouldThrowExceptionIfElementToAddIsNotValidXmlElement() {

        additionalInfosForInstantiation.put(ELEMENT_TO_ADD, "<element>Hello World");
        addXmlElementAction.init(additionalInfosForInstantiation);

        assertThatThrownBy(() -> {
            addXmlElementAction.provideContent(rootWithoutNamespace+coreContent);
        }).isInstanceOf(IssueProvidingContentException.class)
                .hasMessageContaining("elementToAdd");
    }

    @Test
    public void shouldAddElementIfReferenceXPathIsFoundAndEmpty() throws IssueProvidingContentException, IOException, SAXException {

        String actualResult = addXmlElementAction.provideContent("<project><build/></project>");

        log.info("actual result: " + actualResult);

        String expectedResult = "<project><build><element>Hello World</element></build></project>";

        XMLAssert.assertXMLEqual(actualResult, expectedResult);
    }

    @Test
    public void shouldAddElementIfReferenceXPathIsFound_withoutNamespace() throws IssueProvidingContentException, IOException, SAXException {

        String actualResult = addXmlElementAction.provideContent(rootWithoutNamespace+coreContent);

        String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>\n" +
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

                //expecting the element to be there
                "\t\t<element>Hello World</element>\n" +

                "\t</build>\n" +
                "\n" +
                "</project>";

        log.info("actual result: " + actualResult);

        XMLAssert.assertXMLEqual(actualResult, expectedResult);

    }

    @Test
    public void shouldAddElementIfReferenceXPathIsFound_withNamespace() throws IssueProvidingContentException, IOException, SAXException {

        additionalInfosForInstantiation.put(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED, "//*[local-name()='project']/*[local-name()='build']");

        addXmlElementAction.init(additionalInfosForInstantiation);

        String actualResult = addXmlElementAction.provideContent(rootWithNamespace+coreContent);

        String expectedResult = rootWithNamespace +
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

                //expecting the element to be there
                "\t\t<element>Hello World</element>\n" +

                "\t</build>\n" +
                "\n" +
                "</project>";

        log.info("actual result: " + actualResult);

        XMLAssert.assertXMLEqual(actualResult, expectedResult);

    }

    @Test
    public void shouldReturnSameDocumentWhenXPathNotFound() throws IssueProvidingContentException, IOException, SAXException {

        additionalInfosForInstantiation.put(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED, "//project/unknownElement");

        addXmlElementAction.init(additionalInfosForInstantiation);

        String actualResult = addXmlElementAction.provideContent(rootWithoutNamespace+coreContent);

        log.info("actual result: " + actualResult);

        XMLAssert.assertXMLEqual(actualResult, rootWithoutNamespace+coreContent);

    }

    @Test
    public void shouldAddMavenProperty() throws IssueProvidingContentException, IOException, XmlPullParserException {

        String pomFile="dummyPomXml_dependenciesRemoval.xml";

        String valueToAdd="${project.build.directory}/coverage-results";

        ClassLoader classLoader = getClass().getClassLoader();

        Model pomBeforeActionPerformed = pomModelreader.read(classLoader.getResourceAsStream(pomFile));
        // checking BEFORE the action that property is NOT there..
        assertThat(pomBeforeActionPerformed.getProperties().getProperty("project.coverage.directory")).isNull();

        additionalInfosForInstantiation.put(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED, "//*[local-name()='project']/*[local-name()='properties']");
        additionalInfosForInstantiation.put(ELEMENT_TO_ADD, "<project.coverage.directory>"+valueToAdd+"</project.coverage.directory>");

        addXmlElementAction.init(additionalInfosForInstantiation);

        String pomXmlBeforeAction= IOUtils.toString(classLoader.getResourceAsStream(pomFile), StandardCharsets.UTF_8);
        String actualResult = addXmlElementAction.provideContent(pomXmlBeforeAction);

        log.info("actual result: " + actualResult);

        Model newPom = pomModelreader.read(new ByteArrayInputStream(actualResult.getBytes(OUTPUT_ENCODING)));

        assertThat(newPom.getProperties().getProperty("project.coverage.directory")).isEqualTo(valueToAdd);
    }

}