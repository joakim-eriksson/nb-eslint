package se.jocke.nb.eslint.dataObjects;

import java.io.IOException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject.Registration;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.NbBundle.Messages;

@Messages({
    "LBL_ESLintrc_yaml_LOADER=ESLint config file (YAML)"
})
@MIMEResolver.Registration(displayName = "ESLint config file (YAML)", resource = "../eslint-resolver.xml", position = 127)
@Registration(displayName = "#LBL_ESLintrc_yaml_LOADER", iconBase = "se/jocke/nb/eslint/eslint.png", mimeType = "text/eslintrc+x-yaml")

@ActionReferences({
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
            position = 900,
            separatorAfter = 1000
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300
    ),
    @ActionReference(
            path = "Loaders/text/eslintrc+x-yaml/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400
    )
})
public class ESLintrcYamlDataObject extends MultiDataObject {

    public ESLintrcYamlDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);

        registerEditor("text/eslintrc+x-yaml", false);
    }

    @Override
    protected int associateLookup() {
        return 1;
    }
}
