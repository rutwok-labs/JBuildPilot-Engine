package io.github.rutwoklabs.jbuildpilot.jbe;

import io.github.rutwoklabs.jbuildpilot.jbe.lexer.Lexer;
import io.github.rutwoklabs.jbuildpilot.jbe.lexer.Token;
import io.github.rutwoklabs.jbuildpilot.jbe.lexer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LexerTest {

    @Test
    void testBasicLexing() {
        String code = "project \"MyGame\"\njava { version = \"21\" }";
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.PROJECT, tokens.get(0).type());
        assertEquals(TokenType.STRING, tokens.get(1).type());
        assertEquals("MyGame", tokens.get(1).lexeme());
        
        assertEquals(TokenType.JAVA, tokens.get(2).type());
        assertEquals(TokenType.LBRACE, tokens.get(3).type());
        assertEquals(TokenType.VERSION, tokens.get(4).type());
        assertEquals(TokenType.ASSIGN, tokens.get(5).type());
        assertEquals(TokenType.STRING, tokens.get(6).type());
        assertEquals("21", tokens.get(6).lexeme());
        assertEquals(TokenType.RBRACE, tokens.get(7).type());
        assertEquals(TokenType.EOF, tokens.get(8).type());
    }

    @Test
    void testWhitespaceAndNewlines() {
        String code = "\n\n  build  { \n system = maven \n } \n";
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.BUILD, tokens.get(0).type());
        assertEquals(TokenType.LBRACE, tokens.get(1).type());
        assertEquals(TokenType.SYSTEM, tokens.get(2).type());
        assertEquals(TokenType.ASSIGN, tokens.get(3).type());
        assertEquals(TokenType.MAVEN, tokens.get(4).type());
        assertEquals(TokenType.RBRACE, tokens.get(5).type());
        assertEquals(TokenType.EOF, tokens.get(6).type());
    }
}
