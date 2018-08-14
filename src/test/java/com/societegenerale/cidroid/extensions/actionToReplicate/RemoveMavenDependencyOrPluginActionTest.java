package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveMavenDependencyOrPluginActionTest {

    private ClassLoader classLoader = getClass().getClassLoader();

    private String initialPomXml;

    private RemoveMavenDependencyOrPluginAction action;

    private MavenXpp3Reader pomModelreader = new MavenXpp3Reader();

    private final Charset OUTPUT_ENCODING= StandardCharsets.UTF_8;

    @Before
    public void setup() throws IOException {
        initialPomXml = IOUtils
                .toString(classLoader.getResourceAsStream("dummyPomXml_dependenciesRemoval.xml"), StandardCharsets.UTF_8);
    }


    @Test
    public void shouldRemoveDependencyInDependenciesBlock() throws IssueProvidingContentException, IOException, XmlPullParserException {

        action=new RemoveMavenDependencyOrPluginAction("lombok");

        String result=action.provideContent(initialPomXml);

        System.out.println(result);

        Model newPom = pomModelreader.read(new ByteArrayInputStream(result.getBytes(OUTPUT_ENCODING)));

        assertOnlyOneDependencyWithArtifact(newPom.getDependencies(),"logback-classic");
    }

    @Test
    public void shouldRemoveDependencyInDependenciesManagementBlock() throws IssueProvidingContentException, IOException, XmlPullParserException {

        action=new RemoveMavenDependencyOrPluginAction("maven-model");

        String result=action.provideContent(initialPomXml);

        Model newPom = pomModelreader.read(new ByteArrayInputStream(result.getBytes(OUTPUT_ENCODING)));

        assertOnlyOneDependencyWithArtifact(newPom.getDependencyManagement().getDependencies(),"junit");

    }

    @Test
    public void shouldNotRemoveDependencyIfGroupIdDoesntMatch()
            throws IssueProvidingContentException, IOException, SAXException {

        action=new RemoveMavenDependencyOrPluginAction("maven-model","someDummyGroupId");

        String result=action.provideContent(initialPomXml);

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLAssert.assertXMLEqual(initialPomXml,result);

    }

    @Test
    public void shouldRemoveDependencyIfGroupIdMatches()
            throws IssueProvidingContentException, IOException, XmlPullParserException {

        action=new RemoveMavenDependencyOrPluginAction("maven-model","org.apache.maven");

        String result=action.provideContent(initialPomXml);

        Model newPom = pomModelreader.read(new ByteArrayInputStream(result.getBytes(OUTPUT_ENCODING)));

        assertOnlyOneDependencyWithArtifact(newPom.getDependencyManagement().getDependencies(),"junit");

    }


    @Test
    public void shouldRemovePluginInPluginsBlock() throws IssueProvidingContentException, IOException, XmlPullParserException {

        action=new RemoveMavenDependencyOrPluginAction("maven-release-plugin");

        String result=action.provideContent(initialPomXml);

        System.out.println(result);

        Model newPom = pomModelreader.read(new ByteArrayInputStream(result.getBytes(OUTPUT_ENCODING)));

        assertOnlyOneDependencyWithArtifact(newPom.getBuild().getPlugins(),"maven-deploy-plugin");
    }

    @Test
    public void shouldRemovePluginInPluginManagementBlock() throws IssueProvidingContentException, IOException, XmlPullParserException {

        action=new RemoveMavenDependencyOrPluginAction("maven-surefire-plugin");

        String result=action.provideContent(initialPomXml);

        Model newPom = pomModelreader.read(new ByteArrayInputStream(result.getBytes(OUTPUT_ENCODING)));

        assertOnlyOneDependencyWithArtifact(newPom.getBuild().getPluginManagement().getPlugins(),"jacoco-maven-plugin");

    }


    private void assertOnlyOneDependencyWithArtifact(List dependencies, String expectedArtifactId){

        assertThat(dependencies).hasSize(1);

        String actualArtifactId="NOT_FOUND";

        //both dependency and plugin have an artifactId attribute, but no common interface
        //so trying first to cast into Dependency, then Plugin
        try{
            Dependency dependency = (Dependency)dependencies.get(0);
            actualArtifactId=dependency.getArtifactId();
        }
        catch(ClassCastException e){
            Plugin plugin= (Plugin) dependencies.get(0);
            actualArtifactId=plugin.getArtifactId();
        }

        assertThat(actualArtifactId).isEqualTo(expectedArtifactId);
    }




}