package io.github.rutwoklabs.jbuildpilot.jbe;

import io.github.rutwoklabs.jbuildpilot.jbe.ast.AstNodes.*;
import io.github.rutwoklabs.jbuildpilot.jbe.lexer.Lexer;
import io.github.rutwoklabs.jbuildpilot.jbe.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {

    @Test
    void testValidSyntax() {
        String code = "project \"MyGame\" \n build { system = gradle }";
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.tokenize());
        
        ProgramNode ast = parser.parse();
        assertEquals(2, ast.declarations.size());
        
        ProjectNode pNode = (ProjectNode) ast.declarations.get(0);
        assertEquals("MyGame", pNode.name);
        
        BuildNode bNode = (BuildNode) ast.declarations.get(1);
        assertEquals("gradle", bNode.system);
    }

    @Test
    void testInvalidSyntaxThrows() {
        String code = "project { }"; // Missing string, unexpected {
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.tokenize());
        
        assertThrows(RuntimeException.class, parser::parse);
    }
}
