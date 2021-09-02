package se.jocke.nb.eslint.ui.options;

import org.openide.filesystems.FileObject;

/**
 *
 * @author Stan Silvert
 */
public class OptionsUtil {
    // static utility class.  No instance allowed.
    private OptionsUtil() {
    }

    public static boolean isLintedFile(FileObject file) {
        if (file == null) {
            return false;
        }

        String lintRegExp = ESLintOptionsModel.getDefault().getFileExtensionsRegExOption();

        return (lintRegExp != null && file.getNameExt().matches(lintRegExp));
    }
}
