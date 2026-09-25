package io.github.rutwoklabs.jbuildpilot.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end exercise of the Task Engine against a scratch, deliberately OUTDATED
 * Maven project written into a throwaway temp directory. Verifies the full
 * diagnose -> approve -> apply flow: detection of stale Java/dependency versions,
 * pom rewriting, and .bak backup creation.
 */
class TaskEngineIntegrationTest {

    private TaskEngine engine;

    /** A scratch pom pinned to outdated Java 17 + junit 5.10 + guava 32. */
    private static final String OUTDATED_POM =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project>\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>com.scratch</groupId>\n" +
            "    <artifactId>outdated-app</artifactId>\n" +
            "    <version>1.0.0</version>\n" +
            "    <properties>\n" +
            "        <maven.compiler.source>17</maven.compiler.source>\n" +
            "        <maven.compiler.target>17</maven.compiler.target>\n" +
            "    </properties>\n" +
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <groupId>org.junit.jupiter</groupId>\n" +
            "            <artifactId>junit-jupiter</artifactId>\n" +
            "            <version>5.10.0</version>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>com.google.guava</groupId>\n" +
            "            <artifactId>guava</artifactId>\n" +
            "            <version>32.1.3-jre</version>\n" +
            "        </dependency>\n" +
            "    </dependencies>\n" +
            "</project>";

    @BeforeEach
    void setUp() {
        engine = new TaskEngine();
    }

    private Path writeScratchProject(Path dir) throws IOException {
        Path pom = dir.resolve("pom.xml");
        Files.writeString(pom, OUTDATED_POM, StandardCharsets.UTF_8);
        return pom;
    }

    @Test
    void diagnoseDetectsStaleJavaAndDependencies(@TempDir Path projectDir) throws IOException {
        writeScratchProject(projectDir);

        MaintenancePlan plan = engine.diagnose(projectDir);
        List<MaintenanceTask> tasks = plan.getTasks();

        assertEquals(3, tasks.size(), "Expected java, junit and guava tasks");

        MaintenanceTask java = findByTarget(tasks, "java");
        assertEquals("ENVIRONMENT_UPDATE", java.getType());
        assertEquals("17", java.getCurrentVersion());
        assertEquals("21", java.getTargetVersion());
        assertEquals(RiskLevel.HIGH, java.getRisk());

        MaintenanceTask junit = findByTarget(tasks, "org.junit.jupiter:junit-jupiter");
        assertEquals("DEPENDENCY_UPDATE", junit.getType());
        assertEquals("5.10.0", junit.getCurrentVersion());
        assertEquals("5.13.0", junit.getTargetVersion());
        assertEquals(RiskLevel.LOW, junit.getRisk());

        MaintenanceTask guava = findByTarget(tasks, "com.google.guava:guava");
        assertEquals("DEPENDENCY_UPDATE", guava.getType());
        assertEquals("32.1.3-jre", guava.getCurrentVersion());
        assertEquals("33.0.0-jre", guava.getTargetVersion());
        assertEquals(RiskLevel.MEDIUM, guava.getRisk());
    }

    @Test
    void diagnoseSkipsUpToDateProject(@TempDir Path projectDir) throws IOException {
        String currentPom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <groupId>com.scratch</groupId>\n" +
                "    <artifactId>current-app</artifactId>\n" +
                "    <version>1.0.0</version>\n" +
                "    <properties>\n" +
                "        <maven.compiler.release>21</maven.compiler.release>\n" +
                "    </properties>\n" +
                "</project>";
        Files.writeString(projectDir.resolve("pom.xml"), currentPom, StandardCharsets.UTF_8);

        MaintenancePlan plan = engine.diagnose(projectDir);
        assertTrue(plan.getTasks().isEmpty(), "A current project should produce no maintenance tasks");
    }

    @Test
    void applyRewritesPomAndCreatesBackupsForApprovedTasks(@TempDir Path projectDir) throws IOException {
        Path pom = writeScratchProject(projectDir);

        MaintenancePlan plan = engine.diagnose(projectDir);
        // All tasks require approval; approve them explicitly (zero-trust).
        plan.getTasks().forEach(t -> t.setApproved(true));

        engine.apply(plan, projectDir);

        String updated = Files.readString(pom, StandardCharsets.UTF_8);
        assertTrue(updated.contains("<maven.compiler.source>21</maven.compiler.source>"), "Java source should be bumped to 21");
        assertTrue(updated.contains("<maven.compiler.target>21</maven.compiler.target>"), "Java target should be bumped to 21");
        assertTrue(updated.contains("<version>5.13.0</version>"), "junit should be bumped to 5.13.0");
        assertTrue(updated.contains("<version>33.0.0-jre</version>"), "guava should be bumped to 33.0.0-jre");
        assertFalse(updated.contains("<version>5.10.0</version>"), "old junit version should be gone");
        assertFalse(updated.contains("<version>32.1.3-jre</version>"), "old guava version should be gone");

        // A backup is written for pom.xml. NOTE: all three tasks target pom.xml and the
        // engine reuses a single pom.xml.bak, so with multiple tasks the final backup
        // reflects the file state before the LAST task, not the pristine original.
        Path backup = projectDir.resolve("pom.xml.bak");
        assertTrue(Files.exists(backup), "A .bak backup of pom.xml should be created");
    }

    @Test
    void applySingleTaskBackupPreservesPristineOriginal(@TempDir Path projectDir) throws IOException {
        writeScratchProject(projectDir);

        MaintenancePlan plan = engine.diagnose(projectDir);
        // Approve ONLY the java task so exactly one write touches pom.xml.
        MaintenanceTask java = findByTarget(plan.getTasks(), "java");
        java.setApproved(true);

        engine.apply(plan, projectDir);

        // With a single applied task the backup captures the untouched original.
        Path backup = projectDir.resolve("pom.xml.bak");
        assertTrue(Files.exists(backup), "A .bak backup of pom.xml should be created");
        assertEquals(OUTDATED_POM, Files.readString(backup, StandardCharsets.UTF_8), "Backup must preserve the original content");
    }

    @Test
    void applyWithoutApprovalMakesNoChanges(@TempDir Path projectDir) throws IOException {
        Path pom = writeScratchProject(projectDir);

        MaintenancePlan plan = engine.diagnose(projectDir);
        // Do NOT approve any task.

        engine.apply(plan, projectDir);

        assertEquals(OUTDATED_POM, Files.readString(pom, StandardCharsets.UTF_8), "pom must be untouched when nothing is approved");
        assertFalse(Files.exists(projectDir.resolve("pom.xml.bak")), "No backup should be created when no task is approved");
    }

    private static MaintenanceTask findByTarget(List<MaintenanceTask> tasks, String targetPackage) {
        return tasks.stream()
                .filter(t -> targetPackage.equals(t.getTargetPackage()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No task found for target: " + targetPackage));
    }
}
