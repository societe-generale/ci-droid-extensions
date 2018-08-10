package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Given a profile name, will replace (or create it if it doesn't exist) the corresponding Maven profile with provided content. Full profile document is expected as input, starting at profile element, included.
 * <p>
 * Note : output XML will be encoded in UTF-8.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
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
    public String provideContent(String initialContent, ResourceToUpdate resourceToUpdate) {

        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document initialPomXml = builder.parse(new InputSource(new StringReader(initialContent)));

            Optional<Node> profilesSection = findProfilesSection(initialPomXml);
            if (!profilesSection.isPresent()) {
                log.warn("wasn't able to find existing existingProfiles section, or create one");
                //TODO throw proper exception
                return null;
            }

            NodeList existingProfiles = findExistingProfiles(initialPomXml);

            log.info("nb Maven existingProfiles found : {}", existingProfiles.getLength());

            boolean foundExpectedProfile = false;

            for (int i = 0; i < existingProfiles.getLength(); i++) {

                Element profile = (Element) existingProfiles.item(i);

                Node profilesNode = profile.getParentNode();

                Node idElement = profile.getElementsByTagName("id").item(0);

                if (idElement == null) {
                    log.info("no profile with an id element found");
                } else {
                    String profileId = idElement.getFirstChild().getNodeValue();

                    if (profileId.equals(profileName)) {
                        log.info("profile name matching expected one ({}) -> setting content in profile node", profileId);

                        foundExpectedProfile = true;

                        profilesNode.removeChild(profile);

                        insertProfileInRightSection(initialPomXml, profilesNode);

                    } else {
                        log.info("profile name found ({}) doesn't match expected one ({})", profileId, profileName);
                    }
                }
            }

            if (!foundExpectedProfile) {
                log.info("expected profile not found - creating it");
                insertProfileInRightSection(initialPomXml, profilesSection.get());
            }

            return convertXmlDocToString(initialPomXml);

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException | IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            log.warn("problem while parsing pom.xml and/or modifying it", e);
        }

        return null;
    }

    private String convertXmlDocToString(Document initialPomXml)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        final DOMImplementationRegistry domImplementationRegistry = DOMImplementationRegistry.newInstance();
        final DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementationRegistry.getDOMImplementation("LS");
        final LSSerializer serializer = domImplementationLS.createLSSerializer();

        serializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
        serializer.getDomConfig().setParameter("xml-declaration", true);

        LSOutput lsOutput = domImplementationLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        Writer stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);

        serializer.write(initialPomXml, lsOutput);

        return stringWriter.toString();
    }

    private NodeList findExistingProfiles(Document pomXml) throws XPathExpressionException {

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression profilesExpr = xpath.compile("/project/profiles/profile");

        return (NodeList) profilesExpr.evaluate(pomXml, XPathConstants.NODESET);

    }

    private Optional<Node> findProfilesSection(Document pomXMl) throws XPathExpressionException {

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        XPathExpression profilesSectionExpr = xpath.compile("/project/profiles");
        NodeList profiles = (NodeList) profilesSectionExpr.evaluate(pomXMl, XPathConstants.NODESET);

        if (profiles.getLength() == 1) {
            log.info("profilesSection exists..");

            return Optional.of(profiles.item(0));
        } else if (profiles.getLength() == 0) {
            log.info("profilesSection doesn't exist -> creating it");

            XPathExpression projectSectionExpr = xpath.compile("/project");
            NodeList project = (NodeList) projectSectionExpr.evaluate(pomXMl, XPathConstants.NODESET);

            Element newProfilesElement = pomXMl.createElement("profiles");
            Node projectRoot = project.item(0);

            return Optional.of(projectRoot.appendChild(newProfilesElement));
        } else {
            log.warn("not sure about profiles section status - size:{}", profiles.getLength());
        }

        return Optional.empty();
    }

    private void insertProfileInRightSection(Document initialPomXml, Node profilesNode)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document newProfileSnippet = builder.parse(new InputSource(new StringReader(newProfileContent)));
        NodeList nodesToImport = newProfileSnippet.getElementsByTagName("profile");
        Element element = (Element) nodesToImport.item(0);

        Node dup = initialPomXml.importNode(element, true);
        profilesNode.appendChild(dup);
    }

}
