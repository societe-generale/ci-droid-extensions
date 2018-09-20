package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

import static com.societegenerale.cidroid.extensions.actionToReplicate.AddXmlContentAction.ELEMENT_TO_ADD;
import static com.societegenerale.cidroid.extensions.actionToReplicate.AddXmlContentAction.XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;

@Slf4j

public class AddXmlContentActionTest {

    private AddXmlContentAction addXmlElementAction = new AddXmlContentAction();

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

    ClassLoader classLoader = getClass().getClassLoader();

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
                .hasMessageContaining(ELEMENT_TO_ADD);
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

        Model pomBeforeActionPerformed = pomModelreader.read(classLoader.getResourceAsStream(pomFile));

        // checking BEFORE the action that property is NOT there..
        assertThat(pomBeforeActionPerformed.getProperties().getProperty("project.coverage.directory")).isNull();

        String actualResult = addThis(pomFile,"<project.coverage.directory>${project.build.directory}/coverage-results</project.coverage.directory>");

        Model newPom = pomModelreader.read(new ByteArrayInputStream(actualResult.getBytes(OUTPUT_ENCODING)));
        assertThat(newPom.getProperties().getProperty("project.coverage.directory")).isEqualTo("${project.build.directory}/coverage-results");

    }

    @Test
    public void shouldAdd3ElementsAtOnce() throws IssueProvidingContentException, IOException, XmlPullParserException {

        Map<String,String> expectedPropertiesValues=new HashMap<>();
        expectedPropertiesValues.put("project.coverage.directory","${project.build.directory}/coverage-results");
        expectedPropertiesValues.put("sonar.language","java");
        expectedPropertiesValues.put("some.other.prop","someValue");


        shouldAddElementsAndAssertProperties(expectedPropertiesValues,"<project.coverage.directory>${project.build.directory}/coverage-results</project.coverage.directory>",
                                                                      "<sonar.language>java</sonar.language>",
                                                                      "<some.other.prop>someValue</some.other.prop>");
    }



    @Test
    public void shouldAdd2ContiguousElementsAtOnce() throws IssueProvidingContentException, IOException, XmlPullParserException {

        Map<String,String> expectedPropertiesValues=new HashMap<>();
        expectedPropertiesValues.put("project.coverage.directory","${project.build.directory}/coverage-results");
        expectedPropertiesValues.put("sonar.language","java");

        shouldAddElementsAndAssertProperties(expectedPropertiesValues,"<project.coverage.directory>${project.build.directory}/coverage-results</project.coverage.directory>",
                                                                      "<sonar.language>java</sonar.language>");
    }

    @Test
    public void shouldAdd2ElementsSeparatedBySpaceAtOnce() throws IOException, XmlPullParserException, IssueProvidingContentException {

        Map<String,String> expectedPropertiesValues=new HashMap<>();
        expectedPropertiesValues.put("project.coverage.directory","${project.build.directory}/coverage-results");
        expectedPropertiesValues.put("sonar.language","java");

        shouldAddElementsAndAssertProperties(expectedPropertiesValues,"<project.coverage.directory>${project.build.directory}/coverage-results</project.coverage.directory>",
                                                                      "   ",
                                                                      "<sonar.language>java</sonar.language>");
    }

    private void shouldAddElementsAndAssertProperties(Map<String,String> expectedPropertyValues, String... whatToAdd) throws IOException, XmlPullParserException, IssueProvidingContentException {

        String pomFile="dummyPomXml_dependenciesRemoval.xml";

        Model pomBeforeActionPerformed = pomModelreader.read(classLoader.getResourceAsStream(pomFile));

        // checking BEFORE the action that property is NOT there..
        assertThat(pomBeforeActionPerformed.getProperties().getProperty("project.coverage.directory")).isNull();
        assertThat(pomBeforeActionPerformed.getProperties().getProperty("sonar.language")).isNull();

        String actualResult = addThis(pomFile,whatToAdd);

        Model newPom = pomModelreader.read(new ByteArrayInputStream(actualResult.getBytes(OUTPUT_ENCODING)));

        expectedPropertyValues.entrySet().stream().forEach(e ->  assertThat(newPom.getProperties().getProperty(e.getKey())).isEqualTo(e.getValue()));
    }

    private String addThis(String pomFile, String... stuffToAdd) throws IOException, IssueProvidingContentException {

        additionalInfosForInstantiation.put(XPATH_UNDER_WHICH_ELEMENT_NEEDS_TO_BE_ADDED, "//*[local-name()='project']/*[local-name()='properties']");
        additionalInfosForInstantiation.put(ELEMENT_TO_ADD,String.join(StringUtils.EMPTY,stuffToAdd) );

        addXmlElementAction.init(additionalInfosForInstantiation);

        String pomXmlBeforeAction= IOUtils.toString(classLoader.getResourceAsStream(pomFile), StandardCharsets.UTF_8);

        return addXmlElementAction.provideContent(pomXmlBeforeAction);

    }

}