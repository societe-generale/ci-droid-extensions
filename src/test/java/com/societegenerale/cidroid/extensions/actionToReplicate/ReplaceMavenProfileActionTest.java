package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.IssueProvidingContentException;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.assertj.core.api.Assertions;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class ReplaceMavenProfileActionTest {

    private String newProfileContent = "\t\t<profile>\n" +
            "\t\t\t<id>quality</id>\n" +
            "\t\t\t<properties>\n" +
            "\t\t\t\t<jacoco.version>0.8.1</jacoco.version>\n" +
            "\t\t\t\t<sonar-maven-plugin.version>3.4.0.905</sonar-maven-plugin.version>\n" +
            "\t\t\t</properties>\n" +
            "\t\t</profile>";

    private String initialPomXml;

    private MavenXpp3Reader pomModelreader = new MavenXpp3Reader();

    private ClassLoader classLoader = getClass().getClassLoader();

    private ReplaceMavenProfileAction replaceMavenProfileAction = new ReplaceMavenProfileAction("quality", newProfileContent);

    private Profile expectedQualityProfile;

    private Charset outputEncoding = StandardCharsets.UTF_8;

    @Before
    public void setup() throws IOException, XmlPullParserException {
        expectedQualityProfile = buildExpectedQualityProfile();
    }

    @Test
    public void shouldFinalizeBuild() {

        ReplaceMavenProfileAction action = new ReplaceMavenProfileAction();

        Map<String, String> additionalInfosForInstantiation = new HashMap<>();

        additionalInfosForInstantiation.put("profileName", "aProfile");
        additionalInfosForInstantiation.put("newProfileContent", "someProfileContent");

        action.init(additionalInfosForInstantiation);

        assertThat(action.getProfileName()).isEqualTo("aProfile");
        assertThat(action.getNewProfileContent()).isEqualTo("someProfileContent");
    }


    @Test
    public void shouldFormatXmlOutputProperly() throws IOException, XmlPullParserException, IssueProvidingContentException {

        String newProfileContentStrangelyFormatted = "<profile>   <id>quality</id>    <properties>        <sonar.language>java</sonar.language>       <sonar.java.coveragePlugin>jacoco</sonar.java.coveragePlugin>       <project.coverage.directory>${project.build.directory}/coverage-results</project.coverage.directory>        <sonar.jacoco.itReportPaths>${project.coverage.directory}/jacoco-it.exec</sonar.jacoco.itReportPaths>       <sonar.jacoco.reportPaths>${project.coverage.directory}/jacoco-ut.exec</sonar.jacoco.reportPaths>       <jacoco.version>0.8.1</jacoco.version>      <sonar-maven-plugin.version>3.4.0.905</sonar-maven-plugin.version>  </properties>   <build>         <plugins>           <plugin>                <groupId>org.apache.maven.plugins</groupId>                 <artifactId>maven-surefire-plugin</artifactId>              <configuration>                     <!-- Sets the VM argument line used when unit tests are run. -->                    <argLine>${surefireArgLine}</argLine>               </configuration>            </plugin>           <plugin>                <groupId>org.apache.maven.plugins</groupId>                 <artifactId>maven-failsafe-plugin</artifactId>              <executions>                    <execution>                         <goals>                             <goal>integration-test</goal>                           <goal>verify</goal>                         </goals>                        <configuration>                             <argLine>${failsafeArgLine}</argLine>                       </configuration>                    </execution>                </executions>           </plugin>           <plugin>                <groupId>org.jacoco</groupId>               <artifactId>jacoco-maven-plugin</artifactId>                <version>${jacoco.version}</version>                <executions>                    <execution>                         <id>pre-unit-tests</id>                         <goals>                             <goal>prepare-agent</goal>                      </goals>                        <configuration>                             <destFile>${project.coverage.directory}/jacoco-ut.exec</destFile>                           <propertyName>surefireArgLine</propertyName>                        </configuration>                    </execution>                    <execution>                         <id>pre-integration-tests</id>                      <goals>                             <goal>prepare-agent-integration</goal>                      </goals>                        <configuration>                             <destFile>${project.coverage.directory}/jacoco-it.exec</destFile>                           <propertyName>failsafeArgLine</propertyName>                        </configuration>                    </execution>                </executions>           </plugin>           <plugin>                <groupId>org.sonarsource.scanner.maven</groupId>                <artifactId>sonar-maven-plugin</artifactId>                 <version>${sonar-maven-plugin.version}</version>            </plugin>       </plugins>  </build> </profile>";

        replaceMavenProfileAction = new ReplaceMavenProfileAction("quality", newProfileContentStrangelyFormatted);

        initialPomXml = IOUtils.toString(classLoader.getResourceAsStream("dummyPomXml_withExistingQualityProfile.xml"), StandardCharsets.UTF_8);

        String transformedContent = replaceMavenProfileAction.provideContent(initialPomXml);

        Pattern pattern = Pattern.compile("(?m)^\\s*<profile>   <id>quality</id>    <properties>.*");

        Matcher matcher = pattern.matcher(transformedContent);

        boolean foundNonFormattedString = matcher.find();

        if (foundNonFormattedString) {
            System.out.println("found value that shouldn't be there : " + matcher.group(0));
        }

        assertThat(foundNonFormattedString).isFalse();

        //shouldn't throw any exception
        pomModelreader.read(new ByteArrayInputStream(transformedContent.getBytes(outputEncoding)));

    }

    @Test
    public void shouldReplaceExistingProfileWithProvidedContent() throws IOException, XmlPullParserException, IssueProvidingContentException {

        initialPomXml = IOUtils.toString(classLoader.getResourceAsStream("dummyPomXml_withExistingQualityProfile.xml"), StandardCharsets.UTF_8);

        Profile actualQualityProfile = fetchQualityProfileAfterProcessing(initialPomXml);

        assertThat(actualQualityProfile.getProperties()).isEqualTo(expectedQualityProfile.getProperties());
    }

    @Test
    public void shouldAddProfileWithProvidedContentWhenItDoesntExist() throws IOException, XmlPullParserException, IssueProvidingContentException {

        initialPomXml = IOUtils.toString(classLoader.getResourceAsStream("dummyPomXml_withoutQualityProfile.xml"), StandardCharsets.UTF_8);

        Profile actualQualityProfile = fetchQualityProfileAfterProcessing(initialPomXml);

        assertThat(actualQualityProfile.getProperties()).isEqualTo(expectedQualityProfile.getProperties());
    }

    @Test
    public void shouldAddProfileWithProvidedContentWhenNoProfilesSectionAtAll()
            throws IOException, XmlPullParserException, IssueProvidingContentException {

        initialPomXml = IOUtils.toString(classLoader.getResourceAsStream("dummyPomXml_withNoProfile.xml"), StandardCharsets.UTF_8);

        Profile actualQualityProfile = fetchQualityProfileAfterProcessing(initialPomXml);

        assertThat(actualQualityProfile.getProperties()).isEqualTo(expectedQualityProfile.getProperties());
    }

    @Test
    public void shouldAddProfileWithProvidedContentWhenEmptyProfilesSection()
            throws IOException, XmlPullParserException, IssueProvidingContentException {

        initialPomXml = IOUtils.toString(classLoader.getResourceAsStream("dummyPomXml_withEmptyProfilesSection.xml"), StandardCharsets.UTF_8);

        Profile actualQualityProfile = fetchQualityProfileAfterProcessing(initialPomXml);

        assertThat(actualQualityProfile.getProperties()).isEqualTo(expectedQualityProfile.getProperties());
    }

    private Profile fetchQualityProfileAfterProcessing(String initialPomXml)
            throws IOException, XmlPullParserException, IssueProvidingContentException {

        String transformedContent = replaceMavenProfileAction.provideContent(initialPomXml);

        Model newTransformedPom = pomModelreader.read(new ByteArrayInputStream(transformedContent.getBytes(outputEncoding)));

        Assertions.assertThat(newTransformedPom.getModelEncoding()).isEqualTo("UTF-8");

        List<Profile> profiles = newTransformedPom.getProfiles();

        List<Profile> qualityProfiles = profiles.stream().filter(profile -> profile.getId().equals("quality"))
                .collect(toList());

        Assertions.assertThat(qualityProfiles).hasSize(1);

        return qualityProfiles.get(0);

    }

    private Profile buildExpectedQualityProfile() throws IOException, XmlPullParserException {

        String tempPom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                +
                "<profiles>"
                +
                newProfileContent
                + "</profiles>"
                + "</project>";

        Model tempPomForExpectedProfile = pomModelreader.read(new ByteArrayInputStream(tempPom.getBytes(StandardCharsets.UTF_8)));

        return tempPomForExpectedProfile.getProfiles().stream().filter(profile -> profile.getId().equals("quality")).findFirst().get();

    }

}