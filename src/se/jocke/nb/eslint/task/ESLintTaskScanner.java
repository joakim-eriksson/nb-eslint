package se.jocke.nb.eslint.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.tasklist.PushTaskScanner;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.netbeans.spi.tasklist.Task;
import org.netbeans.spi.tasklist.TaskScanningScope;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import se.jocke.nb.eslint.Constants;
import se.jocke.nb.eslint.ESLint;
import se.jocke.nb.eslint.error.ErrorReporter;
import se.jocke.nb.eslint.error.LintError;
import static se.jocke.nb.eslint.options.OptionsUtil.isLintedFile;

public class ESLintTaskScanner extends PushTaskScanner {

    private static final Logger LOG = Logger.getLogger(ESLintTaskScanner.class.getName());

    private static final Map<String, String> ERROR_TYPE_TO_GROUP_MAP = new ConcurrentHashMap<>();

    static {
        ERROR_TYPE_TO_GROUP_MAP.put("ERROR", "nb-tasklist-error");
        ERROR_TYPE_TO_GROUP_MAP.put("WARNING", "nb-tasklist-warning");
    }

    private Callback callback;

    private final List<Stoppable> listeners = new ArrayList<>();

    public ESLintTaskScanner(String name, String desc) {
        super(name, desc, null);
    }

    public static ESLintTaskScanner create() {
        return new ESLintTaskScanner(msg("LBL_task"), msg("DESC_task"));
    }

    private static String msg(String key) throws MissingResourceException {
        return NbBundle.getMessage(ESLint.class, key);
    }

    @Override
    public void setScope(final TaskScanningScope scope, final Callback callback) {
        if (NbPreferences.forModule(ESLint.class).getBoolean(Constants.IS_ESLINT_ENABLED, false)) {
            for (Stoppable stoppable : listeners) {
                stoppable.stop();
            }

            listeners.clear();

            if (this.callback != null) {
                this.callback.clearAllTasks();
            }

            if (callback == null) {
                LOG.warning("Callback null!!!!!");
                return;
            }

            this.callback = callback;

            Collection<? extends Project> projects = scope.getLookup().lookupAll(Project.class);

            FileObject file = scope.getLookup().lookup(FileObject.class);

            callback.started();

            Future<Integer> future = null;

            if (!projects.isEmpty()) {
                for (Project project : projects) {
                    JSFileRecursiveListener listener = new JSFileRecursiveListener(project.getProjectDirectory());
                    future = ESLint.getDefault().verify(project.getProjectDirectory(), new SimpleErrorReporter());
                    listeners.add(listener);
                    listener.start();
                }
            } else if (isLintedFile(file)) {
                Project project = FileOwnerQuery.getOwner(file);
                if (project != null) {
                    ESLintIgnore ignore = ESLintIgnore.get(project.getProjectDirectory());
                    if (!ignore.isIgnored(file)) {
                        JSFileListener listener = new JSFileListener(file);
                        future = ESLint.getDefault().verify(file, new SimpleErrorReporter());
                        listeners.add(listener);
                        listener.start();
                    }
                } else {
                    LOG.log(Level.WARNING, "Project for file not found {0}", file);
                }
            }

            if (future != null) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            callback.finished();
        }
    }

    private class SimpleErrorReporter implements ErrorReporter {

        private final Map<FileObject, List<Task>> tasks;

        public SimpleErrorReporter() {
            this.tasks = new HashMap<>();
        }

        @Override
        public void handle(LintError error) {
            FileObject fileObject = FileUtil.toFileObject(new File(error.getFile()));
            if (!tasks.containsKey(fileObject)) {
                tasks.put(fileObject, new ArrayList<Task>());
            }
            tasks.get(fileObject).add(Task.create(fileObject, ERROR_TYPE_TO_GROUP_MAP.get(error.getType().toUpperCase()), error.getMessage(), error.getLine()));
        }

        @Override
        public void done() {
            for (Map.Entry<FileObject, List<Task>> entry : tasks.entrySet()) {
                callback.setTasks(entry.getKey(), entry.getValue());
            }
        }

    }

    private interface Stoppable {

        void stop();
    }

    private class JSFileRecursiveListener extends FileChangeAdapter implements Stoppable {
        private final FileObject root;

        private final ESLintIgnore ignore;

        public JSFileRecursiveListener(FileObject root) {
            this.root = root;
            this.ignore = ESLintIgnore.get(root);
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            if (isLintedFile(fe.getFile()) && !ignore.isIgnored(fe.getFile())) {
                callback.setTasks(fe.getFile(), Collections.EMPTY_LIST);
            }
        }

        @Override
        public void fileChanged(FileEvent fe) {
            if (isLintedFile(fe.getFile()) && !ignore.isIgnored(fe.getFile())) {
                callback.setTasks(fe.getFile(), Collections.EMPTY_LIST);
                ESLint.getDefault().verify(fe.getFile(), new SimpleErrorReporter());
            }
        }

        public void start() {
            root.addRecursiveListener(this);
        }

        @Override
        public void stop() {
            root.removeRecursiveListener(this);
        }
    }

    private class JSFileListener extends FileChangeAdapter implements Stoppable {
        private final FileObject file;

        public JSFileListener(FileObject file) {
            this.file = file;
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            callback.setTasks(fe.getFile(), Collections.EMPTY_LIST);
        }

        @Override
        public void fileChanged(FileEvent fe) {
            callback.setTasks(fe.getFile(), Collections.EMPTY_LIST);
            ESLint.getDefault().verify(fe.getFile(), new SimpleErrorReporter());
        }

        public void start() {
            file.addFileChangeListener(this);
        }

        @Override
        public void stop() {
            file.removeFileChangeListener(this);
        }
    }
}
