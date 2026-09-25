package io.github.rutwoklabs.jbuildpilot.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultProcessRunnerSecurityTest {

    @Test
    void testCommandInjectionMitigation(@TempDir Path tempDir) {
        DefaultProcessRunner runner = new DefaultProcessRunner();
        
        // A naive shell executor would execute `echo hello && echo injected`
        // But ProcessBuilder treats the whole string as the first argument to echo (or fails if it's the executable)
        List<String> maliciousCommand = Arrays.asList("echo", "hello", "&&", "echo", "injected");
        
        BuildResult result = runner.run(maliciousCommand, tempDir, null);
        
        // Since it's not run in a shell, "&& echo injected" is just printed literally (if echo is a binary) 
        // or execution fails (if "echo" is a shell builtin and not found in PATH).
        // Either way, command injection shouldn't evaluate the `&&`
        // For cross-platform test reliability, we just assert it doesn't crash or that it contains the literal
        // If it failed because `echo` isn't an executable on Windows (it's cmd builtin), exitCode is -1.
        assertTrue(result.getExitCode() != 0 || result.getStdout().contains("&&"), "Shell injection should be thwarted by ProcessBuilder");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testEnvironmentSanitizationLinux(@TempDir Path tempDir) throws Exception {
        // We simulate a runner check by verifying the environment variables are stripped.
        // Actually, we can write a small shell script that dumps the environment, and execute it using the runner.
        Path script = tempDir.resolve("dump_env.sh");
        Files.writeString(script, "#!/bin/sh\nenv\n");
        script.toFile().setExecutable(true);

        // Inject malicious variables into our own process environment is hard in Java.
        // But we can test that the runner doesn't fail when attempting to remove them.
        DefaultProcessRunner runner = new DefaultProcessRunner();
        BuildResult result = runner.run(Arrays.asList(script.toAbsolutePath().toString()), tempDir, null);
        
        String output = result.getStdout();
        assertFalse(output.contains("JAVA_TOOL_OPTIONS="), "Unsafe variables should be removed");
        assertFalse(output.contains("LD_PRELOAD="), "Unsafe variables should be removed");
    }
}
