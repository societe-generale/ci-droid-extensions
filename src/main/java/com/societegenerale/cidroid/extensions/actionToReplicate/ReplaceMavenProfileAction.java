package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
public class ReplaceMavenProfileAction extends AddXmlContentAction {

    private String profileName;

    private String newProfileContent;

    private final String PROFILES_XPATH = "//*[local-name()='project']/*[local-name()='profiles']";

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

        Document doc = parseStringIntoDocument(initialContent);

        List<Node> profilesRootSection = findProfilesNode(doc);

        if (profilesRootSection.isEmpty()) {
            //create profiles section
            doc.getRootElement().addElement("profiles");
            profilesRootSection = findProfilesNode(doc);
        }

        List<Node> expectedProfileSection = findProfileNodeWithId(doc, profileName);

        if (!expectedProfileSection.isEmpty()) {
            removeExistingProfile(expectedProfileSection);
        }

        Document profileToAdd = parseStringIntoDocument(newProfileContent, "profile to add");

        putDocumentToAddUnderSameNamespaceAsParent(profileToAdd, profilesRootSection.get(0).getParent());

        doc.getRootElement().element("profiles").add(profileToAdd.getRootElement());

        return prettyPrint(doc);

    }

    private Node removeExistingProfile(List<Node> expectedProfileSection) {
        return expectedProfileSection.get(0).getParent().detach();
    }

    private List<Node> findProfileNodeWithId(Document doc, String profileId) {
        return doc.selectNodes(PROFILES_XPATH + "/*[local-name()='profile']/*[local-name()='id' and text()='" + profileId + "']");
    }

    private List<Node> findProfilesNode(Document doc) {
        return doc.selectNodes(PROFILES_XPATH);
    }

}
