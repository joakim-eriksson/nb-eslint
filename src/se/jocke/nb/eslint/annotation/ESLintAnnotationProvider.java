package se.jocke.nb.eslint.annotation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.ErrorManager;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.AnnotationProvider;
import org.openide.text.Line;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import se.jocke.nb.eslint.Constants;
import se.jocke.nb.eslint.ESLint;
import se.jocke.nb.eslint.error.ErrorReporter;
import se.jocke.nb.eslint.error.LintError;
import se.jocke.nb.eslint.options.OptionsUtil;

/**
 *
 * @author jocke
 */
@ServiceProvider(service = AnnotationProvider.class)
public class ESLintAnnotationProvider extends FileChangeAdapter implements AnnotationProvider {
    private static final Map<FileObject, Set<Annotation>> MAPPING = new HashMap<>();
    private static final Logger LOG = Logger.getLogger(ESLintAnnotationProvider.class.getName());

    @Override
    public void annotate(Line.Set arg0, Lookup lookup) {
        apply(lookup.lookup(FileObject.class));
    }

    public void apply(final FileObject fileObject) {
        if (NbPreferences.forModule(ESLint.class).getBoolean(Constants.IS_ESLINT_ENABLED, false)) {
            if (OptionsUtil.isLintedFile(fileObject)) {
                LOG.log(Level.INFO, "Start index file {0}", fileObject.getMIMEType());

                if (MAPPING.containsKey(fileObject)) {
                    detachAll(fileObject);

                } else {
                    MAPPING.put(fileObject, new HashSet<Annotation>());
                    fileObject.addFileChangeListener(this);
                }

                try {
                    final DataObject dataObject = DataObject.find(fileObject);
                    final LineCookie lineCookie = dataObject.getLookup().lookup(LineCookie.class);

                    if (lineCookie == null) {
                        LOG.info("Line cookie null");

                        return;
                    }

                    ESLint.getDefault().verify(fileObject, new ErrorReporter() {
                        @Override
                        public void handle(LintError error) {
                            Line currentLine = lineCookie.getLineSet().getCurrent(error.getLine() - 1);
                            Line.Part currentPartLine = currentLine.createPart(error.getStartCol() - 1, error.getEndCol() - error.getStartCol());

                            final ESLintAnnotation annotation = ESLintAnnotation.create(
                                    ESLintAnnotation.Type.get(error.getSeverity()),
                                    error.getMessage(),
                                    error.getLine(),
                                    error.getStartCol(),
                                    error.getEndCol(),
                                    currentPartLine);

                            MAPPING.get(fileObject).add(annotation);

                            annotation.addPropertyChangeListener(new PropertyChangeListener() {
                                @Override
                                public void propertyChange(PropertyChangeEvent evt) {
                                    if (ESLintAnnotation.ATTACHED.equals(evt.getPropertyName())) {
                                        annotation.removePropertyChangeListener(this);
                                        if (MAPPING.containsKey(fileObject)) {
                                            MAPPING.get(fileObject).remove(annotation);
                                        }
                                    }
                                }
                            });
                        }

                        @Override
                        public void done() {
                            LOG.log(Level.FINE, "Scannig done of {0}", fileObject.getName());
                        }
                    });
                } catch (DataObjectNotFoundException ex) {
                    ErrorManager.getDefault().notify(ErrorManager.WARNING, ex);
                }
            }
        }
    }

    public void removeMapping(FileObject fileObject) {
        MAPPING.remove(fileObject);
        fileObject.removeFileChangeListener(this);
    }

    public void detachAll(final FileObject primaryFile) {
        HashSet<Annotation> copy = new HashSet<>(MAPPING.get(primaryFile));

        for (Annotation annotation : copy) {
            annotation.detach();
        }
    }

    @Override
    public void fileDeleted(FileEvent fe) {
        if (MAPPING.containsKey(fe.getFile())) {
            detachAll(fe.getFile());
            removeMapping(fe.getFile());
        }
    }

    @Override
    public void fileChanged(FileEvent fe) {
        apply(fe.getFile());
    }
}
