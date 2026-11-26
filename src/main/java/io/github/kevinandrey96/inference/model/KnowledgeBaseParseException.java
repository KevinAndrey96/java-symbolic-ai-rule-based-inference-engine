package io.github.kevinandrey96.inference.model;

/**
 * Raised when a facts or rules source does not follow the expected syntax.
 */
public class KnowledgeBaseParseException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int lineNumber;

    public KnowledgeBaseParseException(int lineNumber, String message) {
        super("line " + lineNumber + ": " + message);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
