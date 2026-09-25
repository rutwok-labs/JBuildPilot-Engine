package io.github.rutwoklabs.jbuildpilot.builder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultProcessRunner implements ProcessRunner {

    /** Upper bound on how long a single build/run invocation may take. */
    private static final long DEFAULT_TIMEOUT_MINUTES = 60;

    @Override
    public BuildResult run(List<String> command, Path directory, Path javaHome) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(directory.toFile());

            // SECURITY: Sanitize environment variables to prevent injection
            // Build tools can be exploited via variables like MAVEN_OPTS, JAVA_TOOL_OPTIONS, etc.
            java.util.Map<String, String> env = pb.environment();
            env.remove("JAVA_TOOL_OPTIONS");
            env.remove("_JAVA_OPTIONS");
            env.remove("MAVEN_OPTS");
            env.remove("GRADLE_OPTS");
            env.remove("LD_PRELOAD");
            env.remove("LD_LIBRARY_PATH");

            if (javaHome != null) {
                pb.environment().put("JAVA_HOME", javaHome.toAbsolutePath().toString());
            }

            process = pb.start();

            // Drain stdout and stderr concurrently. Reading them sequentially can
            // deadlock: a process that fills the stderr pipe buffer blocks on write
            // while we are still blocked reading stdout that never completes.
            final Process started = process;
            final AtomicReference<String> stderrHolder = new AtomicReference<>("");
            final AtomicReference<Exception> stderrError = new AtomicReference<>();
            Thread stderrThread = new Thread(() -> {
                try {
                    stderrHolder.set(readStream(started.getErrorStream()));
                } catch (Exception e) {
                    stderrError.set(e);
                }
            }, "jbuildpilot-stderr-reader");
            stderrThread.setDaemon(true);
            stderrThread.start();

            String stdout = readStream(process.getInputStream());

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                stderrThread.join(TimeUnit.SECONDS.toMillis(5));
                return new BuildResult(-1, stdout,
                        "Process timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes and was terminated.");
            }

            stderrThread.join(TimeUnit.SECONDS.toMillis(5));

            String stderr = stderrHolder.get();
            if (stderrError.get() != null) {
                stderr = stderr + "\n[stderr read error] " + safeMessage(stderrError.get());
            }

            return new BuildResult(process.exitValue(), stdout, stderr);
        } catch (InterruptedException e) {
            // Preserve interrupt status and do not leave an orphaned process behind.
            Thread.currentThread().interrupt();
            return new BuildResult(-1, "", "Process was interrupted.");
        } catch (Exception e) {
            return new BuildResult(-1, "", safeMessage(e));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return msg != null ? msg : t.getClass().getSimpleName();
    }

    private String readStream(InputStream is) throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }
}
