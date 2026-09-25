package io.github.rutwoklabs.jbuildpilot.jbe.lexer;

public enum TokenType {
    // Keywords
    PROJECT, JAVA, VERSION, BUILD, SYSTEM, RUN, MAIN, DEPENDENCIES, MAVEN, GRADLE,
    
    // Symbols
    LBRACE, RBRACE, ASSIGN,
    
    // Literals & Identifiers
    STRING, IDENTIFIER,
    
    // End of file
    EOF
}
