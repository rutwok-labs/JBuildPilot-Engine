package io.github.rutwoklabs.jbuildpilot.jbe;

import io.github.rutwoklabs.jbuildpilot.jbe.ast.AstNodes.*;
import io.github.rutwoklabs.jbuildpilot.jbe.model.JbeProgram;
import io.github.rutwoklabs.jbuildpilot.jbe.validator.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatorTest {

    @Test
    void testValidAst() {
        ProgramNode ast = new ProgramNode(List.of(
            new ProjectNode("TestProj"),
            new JavaNode("17"),
            new BuildNode("maven")
        ));
        
        Validator validator = new Validator();
        JbeProgram prog = validator.validate(ast);
        
        assertEquals("TestProj", prog.getProjectName());
        assertEquals("17", prog.getJavaVersion());
        assertEquals("maven", prog.getBuildSystem());
    }

    @Test
    void testDuplicateProjectThrows() {
        ProgramNode ast = new ProgramNode(List.of(
            new ProjectNode("Proj1"),
            new ProjectNode("Proj2")
        ));
        
        Validator validator = new Validator();
        assertThrows(RuntimeException.class, () -> validator.validate(ast));
    }
}
