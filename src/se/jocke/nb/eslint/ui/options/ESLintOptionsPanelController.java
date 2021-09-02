package se.jocke.nb.eslint.ui.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.awt.StatusDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import se.jocke.nb.eslint.ESLint;

@OptionsPanelController.SubRegistration(
        location = "Html5",
        id = "ESLint",
        displayName = "#AdvancedOption_DisplayName_ESLint",
        keywords = "#AdvancedOption_Keywords_ESLint",
        keywordsCategory = "Html5/ESLint"
)
@org.openide.util.NbBundle.Messages({"AdvancedOption_DisplayName_ESLint=ESLint", "AdvancedOption_Keywords_ESLint=eslint javascript lint"})
public final class ESLintOptionsPanelController extends OptionsPanelController implements ActionListener, DocumentListener {
    
    private ESLintPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;
    private final ESLintOptionsModel optionsModel = ESLintOptionsModel.getDefault();
    
    public static final String OPTIONS_PATH = "Html5/ESLint"; // NOI18N

    @Override
    public void update() {
        load();
        changed = false;
    }
    
    @Override
    public void applyChanges() {
        if (!validateFields()) {
            return;
        }
        
        store();
        changed = false;
    }
    
    @Override
    public void cancel() {
        // need not do anything special, if no changes have been persisted yet
    }
    
    @Override
    public boolean isValid() {
        return true;
    }
    
    @Override
    public boolean isChanged() {
        return changed;
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("se.jocke.nb.eslint.ui.options.ESLintOptionsPanelController"); //NOI18N
    }
    
    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == panel.radEslintManualConfig) {
//            boolean isManualConfigActive = optionsModel.getESLintConfigOption().equals("manual");

            toggleElements(true);
        } else if (e.getSource() == panel.radEslintDisable) {
//            boolean isManualConfigActive = optionsModel.getESLintConfigOption().equals("disable");

            toggleElements(false);
        } else if (e.getSource() == panel.chbUseCustomConfig) {
            panel.lblCustomConfig.setEnabled(panel.chbUseCustomConfig.isSelected());
            panel.txtCustomConfigPath.setEnabled(panel.chbUseCustomConfig.isSelected());
            panel.lblCustomConfigDescription.setEnabled(panel.chbUseCustomConfig.isSelected());
            panel.btnBrowseCustomConfig.setEnabled(panel.chbUseCustomConfig.isSelected());
        } else if (e.getSource() == panel.btnBrowseEslintPath) {
            int returnVal = panel.fileChooser.showOpenDialog(panel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                panel.txtEslintPath.setText(panel.fileChooser.getSelectedFile().getPath());
            }
        } else if (e.getSource() == panel.btnSearchEslintPath) {
            List<String> ngCliPaths = FileUtils.findFileOnUsersPath(ESLint.ESLINT_CLI_NAME);
            
            if (ngCliPaths.isEmpty()) {
                StatusDisplayer.getDefault().setStatusText(Bundle.ESLintOptionsPanel_executable_notFound());
            } else {
                panel.txtEslintPath.setText(ngCliPaths.get(0));
            }
        } else if (e.getSource() == panel.btnBrowseCustomConfig) {
            int returnVal = panel.fileChooser.showOpenDialog(panel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                panel.txtCustomConfigPath.setText(panel.fileChooser.getSelectedFile().getPath());
            }
        }
        
        changed();
    }
    
    private void toggleElements(boolean isManualConfigActive) {
        panel.lblEslintCli.setEnabled(isManualConfigActive);
        panel.txtEslintPath.setEnabled(isManualConfigActive);
        panel.btnBrowseEslintPath.setEnabled(isManualConfigActive);
        panel.btnSearchEslintPath.setEnabled(isManualConfigActive);
        panel.lblEslintPathDescription.setEnabled(isManualConfigActive);
        panel.chbUseCustomConfig.setEnabled(isManualConfigActive);
        panel.lblEslintFileExtensionsRegEx.setEnabled(isManualConfigActive);
        panel.txtFileExtensionsRegEx.setEnabled(isManualConfigActive);
        panel.txtCustomConfigPath.setEnabled(panel.chbUseCustomConfig.isSelected() && panel.radEslintManualConfig.isSelected());
        panel.btnBrowseCustomConfig.setEnabled(panel.chbUseCustomConfig.isSelected() && panel.radEslintManualConfig.isSelected());
    }
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        changed();
    }
    
    @Override
    public void removeUpdate(DocumentEvent e) {
        changed();
    }
    
    @Override
    public void changedUpdate(DocumentEvent e) {
    }
    
    private Boolean validateFields() {
        return true;
    }
    
    private ESLintPanel getPanel() {
        if (panel == null) {
            panel = new ESLintPanel();
            
            panel.radEslintDisable.addActionListener(this);
            panel.radEslintAutomaticConfig.addActionListener(this);
            panel.radEslintManualConfig.addActionListener(this);
            
            panel.txtEslintPath.setText(optionsModel.getESLintPathOption());
            panel.txtEslintPath.getDocument().addDocumentListener(this);
            panel.btnBrowseEslintPath.addActionListener(this);
            panel.btnSearchEslintPath.addActionListener(this);
            
            panel.chbUseCustomConfig.addActionListener(this);
            panel.txtCustomConfigPath.setText(optionsModel.getCustomConfigPathOption());
            panel.txtCustomConfigPath.getDocument().addDocumentListener(this);
            panel.btnBrowseCustomConfig.addActionListener(this);
            
            panel.txtFileExtensionsRegEx.setText(optionsModel.getFileExtensionsRegExOption());
            panel.txtFileExtensionsRegEx.getDocument().addDocumentListener(this);
        }
        
        return panel;
    }
    
    private void changed() {
        fireChanged();
        
        if (!changed) {
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
    
    private void load() {
        getPanel();
        
        boolean isManualConfigActive = optionsModel.getESLintConfigOption().equals("manual");

//        if (isManualConfigActive) {
        panel.radEslintManualConfig.setSelected(isManualConfigActive);
        
        toggleElements(isManualConfigActive);
//        } else {
//            panel.radEslintDisable.setSelected(true);
//        }

        panel.txtEslintPath.setText(optionsModel.getESLintPathOption());
        
        panel.chbUseCustomConfig.setSelected(optionsModel.getUseCustomConfigOption());
        panel.txtCustomConfigPath.setText(optionsModel.getCustomConfigPathOption());
        
        panel.txtFileExtensionsRegEx.setText(optionsModel.getFileExtensionsRegExOption());
    }
    
    private void store() {
        getPanel();
        
        optionsModel.setESLintConfigOption(panel.radBtnGroupEslintConfig.getSelection().getActionCommand());
        optionsModel.setESLintPathOption(panel.txtEslintPath.getText());
        optionsModel.setUseCustomConfigOption(panel.chbUseCustomConfig.isSelected());
        optionsModel.setCustomConfigPathOption(panel.txtCustomConfigPath.getText());
        optionsModel.setFileExtensionsRegExOption(panel.txtFileExtensionsRegEx.getText());
    }
    
    private void fireChanged() {
        changed = !optionsModel.getESLintConfigOption().equals(panel.radBtnGroupEslintConfig.getSelection().getActionCommand())
                || !optionsModel.getESLintPathOption().equals(panel.txtEslintPath.getText())
                || optionsModel.getUseCustomConfigOption() != panel.chbUseCustomConfig.isSelected()
                || !optionsModel.getCustomConfigPathOption().equals(panel.txtCustomConfigPath.getText())
                || !optionsModel.getFileExtensionsRegExOption().equals(panel.txtFileExtensionsRegEx.getText());
    }
}
