package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import com.societegenerale.cidroid.api.ResourceToUpdate;
import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import com.societegenerale.cidroid.api.actionToReplicate.fields.ExpectedField;
import com.societegenerale.cidroid.api.actionToReplicate.fields.TextField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.dom4j.*;

import java.util.*;

/**
 * An action that will look in a pom.xml for an artifactID (optionally under a groupId if provided) under dependencies, dependenciesManagement or plugins, pluginsManagement, and will remove the dependency/plugin from the pom.xml
 */
@Data
@NoArgsConstructor
@ToString
public class RemoveMavenDependencyOrPluginAction extends AbstractXmlProcessingAction implements ActionToReplicate {

    protected static final String ARTIFACT_ID = "artifactId";

    protected static final String GROUP_ID = "groupId";

    private final String XPATH_FOR_DEPENDENCIES = "/mvn4:project/mvn4:dependencies/mvn4:dependency/mvn4:artifactId";

    private final String XPATH_FOR_DEPENDENCIES_MANAGEMENT = "/mvn4:project/mvn4:dependencyManagement/mvn4:dependencies/mvn4:dependency/mvn4:artifactId";

    private final String XPATH_FOR_PLUGINS = "/mvn4:project/mvn4:build/mvn4:plugins/mvn4:plugin/mvn4:artifactId";

    private final String XPATH_FOR_PLUGIN_MANAGEMENT = "/mvn4:project/mvn4:build/mvn4:pluginManagement/mvn4:plugins/mvn4:plugin/mvn4:artifactId";

    private String artifactId;

    private String groupId = null;

    private static Map namespaceUris = new HashMap();

    static {
        namespaceUris.put("mvn4", "http://maven.apache.org/POM/4.0.0");
    }

    public RemoveMavenDependencyOrPluginAction(String artifactId) {
        this.artifactId = artifactId;
    }

    public RemoveMavenDependencyOrPluginAction(String artifactId, String groupId) {
        this.artifactId = artifactId;
        this.groupId = groupId;
    }

    @Override
    public String provideContent(String documentToProcess, ResourceToUpdate resourceToUpdate) throws IssueProvidingContentException {

        Document pomXml = parseStringIntoDocument(documentToProcess, "");

        List<Node> dependenciesArtifact = new ArrayList<>();

        dependenciesArtifact.addAll(findNodesMatchingInDoc(pomXml, XPATH_FOR_DEPENDENCIES));
        dependenciesArtifact.addAll(findNodesMatchingInDoc(pomXml, XPATH_FOR_DEPENDENCIES_MANAGEMENT));
        dependenciesArtifact.addAll(findNodesMatchingInDoc(pomXml, XPATH_FOR_PLUGIN_MANAGEMENT));
        dependenciesArtifact.addAll(findNodesMatchingInDoc(pomXml, XPATH_FOR_PLUGINS));

        dependenciesArtifact.stream()
                //keep the ones with matchingArtifactId
                .filter(node -> node.getText().equals(artifactId))
                //if groupId has been defined, take it into consideration for element identification
                .filter(node -> keepArtifactConsideringGroupId(node))
                //remove parent (ie either plugin or dependency) from the document
                .forEach(node -> node.getParent().detach());

        return prettyPrint(pomXml);
    }

    @Override
    public List<ExpectedField> getExpectedUIFields() {
        return Arrays.asList(new TextField(ARTIFACT_ID, "the artifactId of the plugin or dependency to remove"),
                new TextField(GROUP_ID, "Optional - the groupId of the plugin or dependency to remove"));
    }

    @Override
    public String getDescriptionForUI() {
        return "will remove a dependency or plugin in pom.xml, depending on provided artifactId";
    }

    private List<Node> findNodesMatchingInDoc(Document pomXml, String xpath) {
        XPath xpathForPluginManagement = pomXml.createXPath(xpath);
        xpathForPluginManagement.setNamespaceURIs(namespaceUris);
        return xpathForPluginManagement.selectNodes(pomXml);
    }

    private boolean keepArtifactConsideringGroupId(Node artifactNode) {

        if (groupId == null) {
            //if groupId is not provided, consider artifact matches
            return true;
        }

        Element parentDependency = artifactNode.getParent();

        return dependencyHasMatchingGroupId(parentDependency);
    }

    private boolean dependencyHasMatchingGroupId(Element parentDependency) {

        XPath xpathGroupIdSelector = DocumentHelper.createXPath("./mvn4:groupId[text()=\"" + groupId + "\"]");
        xpathGroupIdSelector.setNamespaceURIs(namespaceUris);

        return !xpathGroupIdSelector.selectNodes(parentDependency).isEmpty();
    }

    @Override
    public void init(Map<String, String> updateActionInfos) {
        this.artifactId = updateActionInfos.get(ARTIFACT_ID);
        this.groupId = updateActionInfos.get(GROUP_ID);
    }
}
