package io.github.rutwoklabs.jbuildpilot.builder;

public class BuildResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public BuildResult(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public boolean isSuccessful() {
        return exitCode == 0;
    }
}
