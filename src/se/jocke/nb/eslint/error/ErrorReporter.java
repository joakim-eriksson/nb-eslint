package se.jocke.nb.eslint.error;

/**
 *
 * @author jocke
 */
public interface ErrorReporter {
    
    void handle(LintError error);
}
