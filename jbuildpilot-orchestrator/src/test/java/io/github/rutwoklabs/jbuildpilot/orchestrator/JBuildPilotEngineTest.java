package io.github.rutwoklabs.jbuildpilot.orchestrator;

import io.github.rutwoklabs.jbuildpilot.builder.BuildEngine;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionType;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionManager;
import io.github.rutwoklabs.jbuildpilot.core.permission.PermissionSet;
import io.github.rutwoklabs.jbuildpilot.jbe.lexer.Lexer;
import io.github.rutwoklabs.jbuildpilot.jbe.model.JbeProgram;
import io.github.rutwoklabs.jbuildpilot.jbe.parser.Parser;
import io.github.rutwoklabs.jbuildpilot.jbe.validator.Validator;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.EngineContext;
import io.github.rutwoklabs.jbuildpilot.orchestrator.engine.JBuildPilotEngine;
import io.github.rutwoklabs.jbuildpilot.orchestrator.plan.ExecutionPlan;
import io.github.rutwoklabs.jbuildpilot.orchestrator.plan.PlanCompiler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JBuildPilotEngineTest {

    private String code;

    @BeforeEach
    void setUp() {
        code = 
            "project \"MyGame\"\n" +
            "java { version = \"21\" }\n" +
            "build { system = gradle }\n" +
            "run { main = \"com.example.game.Main\" }\n" +
            "dependencies { maven \"group:artifact:1.0.0\" }\n";
    }

    @Test
    void testSuccessfulExecutionWithPermissions() {
        // Compile from JBE to Plan
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.tokenize());
        JbeProgram program = new Validator().validate(parser.parse());
        ExecutionPlan plan = PlanCompiler.compile(program);

        assertEquals(5, plan.getSteps().size()); // Java, Tool, Dependency, Build, Run

        // Grant ALL permissions for this test
        PermissionManager pm = new PermissionManager(PermissionSet.all(), null);
        MockContext context = new MockContext(pm);

        JBuildPilotEngine engine = new JBuildPilotEngine();
        engine.execute(plan, context);

        assertTrue(context.getLogs().contains("Execution Plan Completed Successfully."));
    }

    @Test
    void testExecutionFailsWhenPermissionDenied() {
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.tokenize());
        JbeProgram program = new Validator().validate(parser.parse());
        ExecutionPlan plan = PlanCompiler.compile(program);

        // Explicitly deny EXECUTE_BUILD
        PermissionSet permissions = PermissionSet.all();
        permissions.revoke(PermissionType.EXECUTE_BUILD);
        PermissionManager pm = new PermissionManager(permissions, null);
        MockContext context = new MockContext(pm);

        JBuildPilotEngine engine = new JBuildPilotEngine();
        
        SecurityException ex = assertThrows(SecurityException.class, () -> engine.execute(plan, context));
        assertTrue(ex.getMessage().contains("execute build"));
    }

    @Test
    void testExecutionFailsWhenBuildReturnsNonZeroExitCode() {
        ExecutionPlan plan = new ExecutionPlan("FailingBuild",
                List.of(new io.github.rutwoklabs.jbuildpilot.orchestrator.plan.BuildStep("maven")));

        PermissionManager pm = new PermissionManager(PermissionSet.all(), null);
        MockContext context = new MockContext(pm) {
            @Override
            public BuildEngine getBuildEngine(String system) {
                return req -> new io.github.rutwoklabs.jbuildpilot.builder.BuildResult(1, "", "compilation failure");
            }
        };

        JBuildPilotEngine engine = new JBuildPilotEngine();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> engine.execute(plan, context));
        assertTrue(ex.getMessage().contains("Build failed"));
        assertFalse(context.getLogs().contains("Execution Plan Completed Successfully."),
                "A failed build must not report the plan as completed successfully.");
    }

    private static class MockContext implements EngineContext {
        private final PermissionManager permissionManager;
        private final List<String> logs = new ArrayList<>();

        MockContext(PermissionManager permissionManager) {
            this.permissionManager = permissionManager;
        }

        @Override
        public PermissionManager getPermissionManager() {
            return permissionManager;
        }

        @Override
        public BuildEngine getBuildEngine(String system) {
            return new BuildEngine() {
                @Override
                public io.github.rutwoklabs.jbuildpilot.builder.BuildResult execute(io.github.rutwoklabs.jbuildpilot.builder.BuildRequest req) {
                    return new io.github.rutwoklabs.jbuildpilot.builder.BuildResult(0, "Mock Output", "");
                }
            };
        }

        @Override
        public Path getProjectDirectory() {
            return Path.of(".");
        }

        @Override
        public Path getJavaHome() {
            return Path.of(".");
        }

        @Override
        public void log(String message) {
            logs.add(message);
        }

        public List<String> getLogs() {
            return logs;
        }
    }
}
