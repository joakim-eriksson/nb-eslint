package se.jocke.nb.eslint.error;

/**
 *
 * @author jocke
 */
public class LintError {

    private final String file;
    private final int line;
    private final int col;
    private final String type;
    private final String message;

    public LintError(String file, int line, int col, String type, String message) {
        this.file = file;
        this.line = line;
        this.col = col;
        this.type = type;
        this.message = message;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
    
}
