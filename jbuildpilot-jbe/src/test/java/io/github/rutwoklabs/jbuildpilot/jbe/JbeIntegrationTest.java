package io.github.rutwoklabs.jbuildpilot.jbe;

import io.github.rutwoklabs.jbuildpilot.jbe.ast.AstNodes.ProgramNode;
import io.github.rutwoklabs.jbuildpilot.jbe.lexer.Lexer;
import io.github.rutwoklabs.jbuildpilot.jbe.model.JbeProgram;
import io.github.rutwoklabs.jbuildpilot.jbe.parser.Parser;
import io.github.rutwoklabs.jbuildpilot.jbe.validator.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JbeIntegrationTest {

    @Test
    void testCompleteJbePipeline() {
        String code = 
            "project \"MyGame\"\n" +
            "java {\n" +
            "  version = \"21\"\n" +
            "}\n" +
            "build {\n" +
            "  system = gradle\n" +
            "}\n" +
            "run {\n" +
            "  main = \"com.example.game.Main\"\n" +
            "}\n" +
            "dependencies {\n" +
            "  maven \"group:artifact:1.0.0\"\n" +
            "}\n";

        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.tokenize());
        ProgramNode ast = parser.parse();
        
        Validator validator = new Validator();
        JbeProgram program = validator.validate(ast);

        assertEquals("MyGame", program.getProjectName());
        assertEquals("21", program.getJavaVersion());
        assertEquals("gradle", program.getBuildSystem());
        assertEquals("com.example.game.Main", program.getMainClass());
        assertEquals(1, program.getDependencies().size());
        assertEquals("maven:group:artifact:1.0.0", program.getDependencies().get(0));
    }
}
