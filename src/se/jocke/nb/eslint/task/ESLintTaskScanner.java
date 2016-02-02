package se.jocke.nb.eslint.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private Callback callback;
    
    private FileObject root;

    private FileObject single;

    private final Set<FileObject> watched = Collections.newSetFromMap(new ConcurrentHashMap<FileObject, Boolean>());

    private final FileChangeAdapter fileCreatedListener = new FileChangeAdapter() {

        @Override
        public void fileDeleted(FileEvent fe) {
            if (watched.contains(fe.getFile())) {
                callback.setTasks(fe.getFile(), Collections.EMPTY_LIST);
            }
        }

        @Override
        public void fileChanged(FileEvent fe) {
            if (watched.contains(fe.getFile())) {
                callback.setTasks(fe.getFile(), Collections.EMPTY_LIST);
                ESLint.getDefault().verify(fe.getFile(), new SimpleErrorReporter());
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

        if (single != null) {
            single.removeFileChangeListener(fileCreatedListener);
        }

        if (this.callback != null) {
            this.callback.clearAllTasks();
        }

        if (callback == null) {
            LOG.warning("Callback null!!!!!");
            return;
        }
        
        this.callback = callback;

        Project project = scope.getLookup().lookup(Project.class);
        FileObject file = scope.getLookup().lookup(FileObject.class);

        callback.started();

        Future<Integer> future = null;

        if (project != null) {
            this.root = project.getProjectDirectory();
            LOG.log(Level.FINE, "Adding recursive listener to {0}", project);
            project.getProjectDirectory().addRecursiveListener(fileCreatedListener);
            future = ESLint.getDefault().verify(project.getProjectDirectory(), new SimpleErrorReporter());

        } else if (file != null && !file.isFolder() && file.getExt().equalsIgnoreCase("js")) {
            this.single = file;
            this.single.addFileChangeListener(fileCreatedListener);
            this.watched.add(file);
            future = ESLint.getDefault().verify(file, new SimpleErrorReporter());
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
            watched.addAll(tasks.keySet());
            for (Map.Entry<FileObject, List<Task>> entry : tasks.entrySet()) {
                callback.setTasks(entry.getKey(), entry.getValue());
            }
        }

    }
}
