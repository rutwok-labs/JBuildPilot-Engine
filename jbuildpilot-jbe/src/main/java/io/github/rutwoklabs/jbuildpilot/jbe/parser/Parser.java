package io.github.rutwoklabs.jbuildpilot.jbe.parser;

import io.github.rutwoklabs.jbuildpilot.jbe.ast.AstNodes.*;
import io.github.rutwoklabs.jbuildpilot.jbe.ast.Node;
import io.github.rutwoklabs.jbuildpilot.jbe.lexer.Token;
import io.github.rutwoklabs.jbuildpilot.jbe.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public ProgramNode parse() {
        List<Node> declarations = new ArrayList<>();
        while (!isAtEnd()) {
            declarations.add(declaration());
        }
        return new ProgramNode(declarations);
    }

    private Node declaration() {
        if (match(TokenType.PROJECT)) return projectDeclaration();
        if (match(TokenType.JAVA)) return javaBlock();
        if (match(TokenType.BUILD)) return buildBlock();
        if (match(TokenType.RUN)) return runBlock();
        if (match(TokenType.DEPENDENCIES)) return dependenciesBlock();

        throw error(peek(), "Unexpected token in declaration: " + peek().lexeme());
    }

    private ProjectNode projectDeclaration() {
        Token name = consume(TokenType.STRING, "Expect project name string.");
        return new ProjectNode(name.lexeme());
    }

    private JavaNode javaBlock() {
        consume(TokenType.LBRACE, "Expect '{' after 'java'.");
        consume(TokenType.VERSION, "Expect 'version' in java block.");
        consume(TokenType.ASSIGN, "Expect '=' after 'version'.");
        Token version = consume(TokenType.STRING, "Expect java version string.");
        consume(TokenType.RBRACE, "Expect '}' after java block.");
        return new JavaNode(version.lexeme());
    }

    private BuildNode buildBlock() {
        consume(TokenType.LBRACE, "Expect '{' after 'build'.");
        consume(TokenType.SYSTEM, "Expect 'system' in build block.");
        consume(TokenType.ASSIGN, "Expect '=' after 'system'.");
        
        Token system;
        if (match(TokenType.MAVEN)) {
            system = previous();
        } else if (match(TokenType.GRADLE)) {
            system = previous();
        } else {
            throw error(peek(), "Expect 'maven' or 'gradle' for build system.");
        }
        
        consume(TokenType.RBRACE, "Expect '}' after build block.");
        return new BuildNode(system.lexeme());
    }

    private RunNode runBlock() {
        consume(TokenType.LBRACE, "Expect '{' after 'run'.");
        consume(TokenType.MAIN, "Expect 'main' in run block.");
        consume(TokenType.ASSIGN, "Expect '=' after 'main'.");
        Token mainClass = consume(TokenType.STRING, "Expect main class string.");
        consume(TokenType.RBRACE, "Expect '}' after run block.");
        return new RunNode(mainClass.lexeme());
    }

    private DependenciesNode dependenciesBlock() {
        consume(TokenType.LBRACE, "Expect '{' after 'dependencies'.");
        List<DependencyNode> deps = new ArrayList<>();
        
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            Token type;
            if (match(TokenType.MAVEN)) {
                type = previous();
            } else if (match(TokenType.GRADLE)) {
                type = previous();
            } else {
                throw error(peek(), "Expect 'maven' or 'gradle' dependency type.");
            }
            
            Token coord = consume(TokenType.STRING, "Expect dependency coordinates string.");
            deps.add(new DependencyNode(type.lexeme(), coord.lexeme()));
        }
        
        consume(TokenType.RBRACE, "Expect '}' after dependencies block.");
        return new DependenciesNode(deps);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private RuntimeException error(Token token, String message) {
        return new RuntimeException("Parse Error at [" + token.line() + ":" + token.column() + "]: " + message);
    }
}
