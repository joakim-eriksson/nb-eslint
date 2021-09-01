package se.jocke.nb.eslint.error;

/**
 *
 * @author jocke
 */
public class LintError {

    private final String file;
    private final int line;
    private final int startColumn;
    private final int endColumn;
    private final int severity; // HINT: 0 = off, 1 = warning, 2 = error
    private final String message;

    public LintError(String file, int line, int startCol, int endCol, int severity, String message) {
        this.file = file;
        this.line = line;
        this.startColumn = startCol;
        this.endColumn = endCol;
        this.severity = severity;
        this.message = message;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getStartCol() {
        return startColumn;
    }
    
    public int getEndCol() {
        return endColumn;
    }

    public int getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }
}
