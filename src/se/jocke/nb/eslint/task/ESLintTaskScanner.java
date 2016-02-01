package se.jocke.nb.eslint.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.spi.queries.VisibilityQueryImplementation;
import org.netbeans.spi.tasklist.PushTaskScanner;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.netbeans.spi.tasklist.Task;
import org.netbeans.spi.tasklist.TaskScanningScope;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.util.Lookup;
import se.jocke.nb.eslint.ESLint;
import se.jocke.nb.eslint.error.ErrorReporter;
import se.jocke.nb.eslint.error.LintError;

public class ESLintTaskScanner extends PushTaskScanner {

    private static final Logger LOG = Logger.getLogger(ESLintTaskScanner.class.getName());

    private static final Map<String, String> ERROR_TYPE_TO_GROUP_MAP = new ConcurrentHashMap<>();

    static {
        ERROR_TYPE_TO_GROUP_MAP.put("ERROR", "nb-tasklist-error");
        ERROR_TYPE_TO_GROUP_MAP.put("WARNING", "nb-tasklist-warning");
    }

    private final List<FileChangeListener> changeListeners = new ArrayList<>();

    private Callback callback;

    private TaskScanningScope scope;

    private FileObject root;

    private final FileChangeAdapter fileCreatedListener = new FileChangeAdapter() {
        @Override
        public void fileDataCreated(FileEvent fe) {
            if (isTargetFile(fe.getFile())) {
                fe.getFile().addFileChangeListener(new FileChangeListener(fe.getFile()));
            }
        }
    };

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

        if (root != null) {
            root.removeRecursiveListener(fileCreatedListener);
        }

        for (FileChangeListener listener : changeListeners) {
            listener.dispose();
        }

        if (this.callback != null) {
            this.callback.clearAllTasks();
        }

        if (callback == null) {
            LOG.warning("Callback null!!!!!");
            return;
        }

        if (scope == null) {
            LOG.warning("Scope null!!!!!");
            return;
        }

        this.callback = callback;
        this.scope = scope;

        Project project = scope.getLookup().lookup(Project.class);

        if (project != null) {
            this.root = project.getProjectDirectory();
            LOG.log(Level.FINE, "Adding recursive listener to {0}", project);
            project.getProjectDirectory().addRecursiveListener(fileCreatedListener);

        } else {
            LOG.log(Level.FINE, "Will not listen for created files under {0}", project);
        }

        callback.started();

        scope.forEach(new Consumer<FileObject>() {

            @Override
            public void accept(final FileObject file) {
                if (isTargetFile(file)) {
                    FileChangeListener listener = new FileChangeListener(file);
                    file.addFileChangeListener(listener);
                    changeListeners.add(listener);
                    LOG.log(Level.FINE, "Start scanning file {0}", file.getPath());
                    Future<Integer> future = ESLint.getDefault().verify(file, new SimpleErrorReporter(file));
                    try {
                        future.get(10, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                        LOG.info("Timeout ESLint");
                    }
                }
            }
        });

        callback.finished();

    }

    private boolean isTargetFile(final FileObject file) {
        return scope.isInScope(file) && !file.isFolder() && "JS".equals(file.getExt().toUpperCase()) && !isNBFile(file) && isVisible(file);
    }

    private boolean isNBFile(FileObject file) {
        return file.getPath().startsWith("jsstubs") || file.getPath().startsWith("js-domstubs");
    }

    private boolean isVisible(FileObject fileObject) {

        FileObject parent = fileObject.getParent();

        VisibilityQueryImplementation query = Lookup.getDefault().lookup(VisibilityQueryImplementation.class);

        while (parent != null) {

            if (!query.isVisible(parent)) {
                return false;
            }

            parent = parent.getParent();
        }

        return true;
    }

    private class SimpleErrorReporter implements ErrorReporter {

        private final FileObject fileObject;
        private final List<Task> tasks;

        public SimpleErrorReporter(FileObject fileObject) {
            this.tasks = new ArrayList<>();
            this.fileObject = fileObject;
        }

        @Override
        public void handle(LintError error) {
            tasks.add(Task.create(fileObject, ERROR_TYPE_TO_GROUP_MAP.get(error.getType().toUpperCase()), error.getMessage(), error.getLine()));
        }

        @Override
        public void done() {
            callback.setTasks(fileObject, tasks);
        }

    }

    private final class FileChangeListener extends FileChangeAdapter {

        private final FileObject fileObject;

        public FileChangeListener(FileObject fileObject) {
            this.fileObject = fileObject;
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            callback.setTasks(fileObject, Collections.EMPTY_LIST);
            dispose();
        }

        public void dispose() {
            fileObject.removeFileChangeListener(this);
        }

        @Override
        public void fileChanged(FileEvent fe) {
            ESLint.getDefault().verify(fileObject, new SimpleErrorReporter(fileObject));
        }

    }
}
