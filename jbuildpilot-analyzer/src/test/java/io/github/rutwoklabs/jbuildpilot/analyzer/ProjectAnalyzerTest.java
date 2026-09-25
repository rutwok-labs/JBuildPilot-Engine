package io.github.rutwoklabs.jbuildpilot.analyzer;

import io.github.rutwoklabs.jbuildpilot.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectAnalyzerTest {

    private ProjectAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ProjectAnalyzer();
    }

    @Test
    void shouldAnalyzeMavenProjectWithJavaRelease(@TempDir Path tempDir) throws Exception {
        String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <groupId>com.test</groupId>\n" +
                "    <artifactId>test-maven</artifactId>\n" +
                "    <version>1.0.0</version>\n" +
                "    <properties>\n" +
                "        <maven.compiler.release>21</maven.compiler.release>\n" +
                "    </properties>\n" +
                "</project>";
        Files.writeString(tempDir.resolve("pom.xml"), pomXml);
        Files.createFile(tempDir.resolve("mvnw"));

        ProjectModel model = analyzer.analyze(tempDir);

        assertEquals("test-maven", model.getMetadata().getName());
        assertEquals("com.test", model.getMetadata().getGroupId());
        
        List<Requirement> reqs = model.getRequirements();
        assertEquals(2, reqs.size());
        
        assertTrue(reqs.stream().anyMatch(r -> r.getType() == RequirementType.MAVEN));
        
        Requirement javaReq = reqs.stream().filter(r -> r.getType() == RequirementType.JAVA).findFirst().orElseThrow();
        assertTrue(javaReq.getVersionConstraint().isSatisfiedBy(Version.of("21")));
    }

    @Test
    void shouldAnalyzeGradleGroovyProjectWithSourceCompat(@TempDir Path tempDir) throws Exception {
        String buildGradle = "plugins { id 'java' }\n" +
                "sourceCompatibility = '17'\n" +
                "targetCompatibility = '17'";
        Files.writeString(tempDir.resolve("build.gradle"), buildGradle);
        Files.createFile(tempDir.resolve("gradlew.bat"));

        ProjectModel model = analyzer.analyze(tempDir);

        List<Requirement> reqs = model.getRequirements();
        assertEquals(3, reqs.size()); // Gradle + Java + Plugin

        assertTrue(reqs.stream().anyMatch(r -> r.getType() == RequirementType.GRADLE));
        
        Requirement javaReq = reqs.stream().filter(r -> r.getType() == RequirementType.JAVA).findFirst().orElseThrow();
        assertTrue(javaReq.getVersionConstraint().isSatisfiedBy(Version.of("17")));
    }

    @Test
    void shouldAnalyzeGradleKotlinDslWithToolchain(@TempDir Path tempDir) throws Exception {
        String buildGradleKts = "java {\n" +
                "    toolchain {\n" +
                "        languageVersion = JavaLanguageVersion.of(21)\n" +
                "    }\n" +
                "}";
        Files.writeString(tempDir.resolve("build.gradle.kts"), buildGradleKts);

        ProjectModel model = analyzer.analyze(tempDir);

        List<Requirement> reqs = model.getRequirements();
        assertEquals(2, reqs.size()); // Gradle + Java

        Requirement javaReq = reqs.stream().filter(r -> r.getType() == RequirementType.JAVA).findFirst().orElseThrow();
        assertTrue(javaReq.getVersionConstraint().isSatisfiedBy(Version.of("21")));
    }

    @Test
    void shouldReturnEmptyRequirementsForProjectWithNoBuildFile(@TempDir Path tempDir) {
        ProjectModel model = analyzer.analyze(tempDir);

        assertEquals(tempDir.getFileName().toString(), model.getMetadata().getName());
        assertTrue(model.getRequirements().isEmpty());
    }

    @Test
    void shouldAnalyzeMavenWithSourceAndTargetProperties(@TempDir Path tempDir) throws Exception {
        String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <groupId>com.legacy</groupId>\n" +
                "    <artifactId>legacy-maven</artifactId>\n" +
                "    <properties>\n" +
                "        <maven.compiler.source>1.8</maven.compiler.source>\n" +
                "        <maven.compiler.target>1.8</maven.compiler.target>\n" +
                "    </properties>\n" +
                "</project>";
        Files.writeString(tempDir.resolve("pom.xml"), pomXml);

        ProjectModel model = analyzer.analyze(tempDir);

        Requirement javaReq = model.getRequirements().stream()
                .filter(r -> r.getType() == RequirementType.JAVA)
                .findFirst().orElseThrow();
        assertTrue(javaReq.getVersionConstraint().isSatisfiedBy(Version.of("1.8")));
    }
}
