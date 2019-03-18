package se.jocke.nb.eslint.options;

import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;
import org.openide.filesystems.FileObject;
import org.openide.util.NbPreferences;
import se.jocke.nb.eslint.Constants;
import se.jocke.nb.eslint.ESLint;

/**
 *
 * @author Stan Silvert
 */
public class OptionsUtil {
    private static final Logger LOG = Logger.getLogger(OptionsUtil.class.getName());
    private static final Preferences PREFS = NbPreferences.forModule(ESLint.class);
    
    // static utility class.  No instance allowed.
    private OptionsUtil() {}
    
    public static boolean isLintedFile(FileObject file) {
        if (file == null) return false;
        
        boolean lintJavascript = PREFS.getBoolean(Constants.LINT_JAVASCRIPT, true);
        boolean lintTypeScript = PREFS.getBoolean(Constants.LINT_TYPESCRIPT, false);
        String lintRegExp = PREFS.get(Constants.LINT_REGEXP, null);
        
        if (lintJavascript && file.getMIMEType().toLowerCase().contains("javascript")) return true;
        if (lintTypeScript && file.getMIMEType().toLowerCase().contains("typescript")) return true;
        
        try {
            if (lintRegExp != null && file.getNameExt().matches(lintRegExp)) return true;
        } catch (PatternSyntaxException e) {
            LOG.warning("Bad regular expression: " + e.getMessage());
        }
        
        return false;
    }
}
