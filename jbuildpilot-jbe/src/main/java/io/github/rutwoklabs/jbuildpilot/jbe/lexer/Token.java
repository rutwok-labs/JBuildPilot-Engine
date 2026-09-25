package io.github.rutwoklabs.jbuildpilot.jbe.lexer;

public record Token(TokenType type, String lexeme, int line, int column) {
    @Override
    public String toString() {
        return type + (lexeme.isEmpty() ? "" : " '" + lexeme + "'") + " [" + line + ":" + column + "]";
    }
}
