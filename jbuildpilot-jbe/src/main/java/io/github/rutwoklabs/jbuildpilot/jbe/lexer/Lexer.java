package io.github.rutwoklabs.jbuildpilot.jbe.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int columnStart = 0;
    
    private static final Map<String, TokenType> KEYWORDS;
    
    static {
        KEYWORDS = new HashMap<>();
        KEYWORDS.put("project", TokenType.PROJECT);
        KEYWORDS.put("java", TokenType.JAVA);
        KEYWORDS.put("version", TokenType.VERSION);
        KEYWORDS.put("build", TokenType.BUILD);
        KEYWORDS.put("system", TokenType.SYSTEM);
        KEYWORDS.put("run", TokenType.RUN);
        KEYWORDS.put("main", TokenType.MAIN);
        KEYWORDS.put("dependencies", TokenType.DEPENDENCIES);
        KEYWORDS.put("maven", TokenType.MAVEN);
        KEYWORDS.put("gradle", TokenType.GRADLE);
    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, getColumn()));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case '=': addToken(TokenType.ASSIGN); break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;
            case '\n':
                line++;
                columnStart = current;
                break;
            case '"': string(); break;
            default:
                if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new RuntimeException("Unexpected character '" + c + "' at line " + line);
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type, text);
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                columnStart = current + 1;
            }
            advance();
        }
        
        if (isAtEnd()) {
            throw new RuntimeException("Unterminated string at line " + line);
        }
        
        advance(); // The closing "
        
        // Trim the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || 
               (c >= 'A' && c <= 'Z') || 
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || (c >= '0' && c <= '9') || c == '.' || c == '-'; 
        // Note: added '.' and '-' for identifiers like 'maven' or versions if not quoted, 
        // though typically versions are quoted.
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private int getColumn() {
        return start - columnStart + 1;
    }

    private void addToken(TokenType type) {
        addToken(type, source.substring(start, current));
    }

    private void addToken(TokenType type, String lexeme) {
        tokens.add(new Token(type, lexeme, line, getColumn()));
    }
}
