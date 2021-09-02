package se.jocke.nb.eslint.ui.options;

import java.nio.file.Paths;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;
import se.jocke.nb.eslint.ESLint;

/**
 *
 * @author Chris
 */
public class ESLintOptionsModel {

    private static ESLintOptionsModel instance;

    public static ESLintOptionsModel getDefault() {
        if (instance == null) {
            instance = new ESLintOptionsModel();
        }
        return instance;
    }

    private static Preferences getPreferences() {
        return NbPreferences.forModule(ESLint.class);
    }

    /**
     * @return the esLintConfig
     */
    public String getESLintConfigOption() {
        return getPreferences().get("esLintConfig", "manual");
    }

    /**
     * @param esLintConfig the esLintConfig to set
     */
    public void setESLintConfigOption(String esLintConfig) {
        getPreferences().put("esLintConfig", esLintConfig);
    }

    /**
     * @return the esLintPath
     */
    public String getESLintPathOption() {
        return getPreferences().get("esLintPath", "");
    }

    /**
     * @param esLintPath the esLintPath to set
     */
    public void setESLintPathOption(String esLintPath) {
        getPreferences().put("esLintPath", esLintPath);
    }

    /**
     * @return the useCustomConfig
     */
    public boolean getUseCustomConfigOption() {
        return getPreferences().getBoolean("useCustomConfig", false);
    }

    /**
     * @param useCustomConfig the useCustomConfig to set
     */
    public void setUseCustomConfigOption(boolean useCustomConfig) {
        getPreferences().putBoolean("useCustomConfig", useCustomConfig);
    }

    /**
     * @return the customConfigPath
     */
    public String getCustomConfigPathOption() {
        return getPreferences().get("customConfigPath", Paths.get(System.getProperty("user.home"), ".eslintrc.js").toString());
    }

    /**
     * @param customConfigPath the customConfigPath to set
     */
    public void setCustomConfigPathOption(String customConfigPath) {
        getPreferences().put("customConfigPath", customConfigPath);
    }

    /**
     * @return the fileExtensionsRegEx
     */
    public String getFileExtensionsRegExOption() {
        return getPreferences().get("fileExtensionsRegEx", ".*?\\.[j,t]sx?$|.*?\\.vue$");
    }

    /**
     * @param fileExtensionsRegEx the fileExtensionsRegEx to set
     */
    public void setFileExtensionsRegExOption(String fileExtensionsRegEx) {
        getPreferences().put("fileExtensionsRegEx", fileExtensionsRegEx);
    }
}
