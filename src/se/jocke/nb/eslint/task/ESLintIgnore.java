package se.jocke.nb.eslint.task;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author jocke
 */
public class ESLintIgnore {

    private final FileObject root;

    private final List<PathMatcher> pathMatchers;

    private ESLintIgnore(FileObject root, List<PathMatcher> pathMatchers) {
        this.root = root;
        this.pathMatchers = new ArrayList<>(pathMatchers);
        this.pathMatchers.add(FileSystems.getDefault().getPathMatcher("glob:node_modules/**/*"));
    }

    public static ESLintIgnore get(FileObject fileObject) {
        if (fileObject.isFolder() && fileObject.getFileObject(".eslintignore") != null) {
            try {
                FileObject ignore = fileObject.getFileObject(".eslintignore");
                List<PathMatcher> pathMatchers = new ArrayList<>();
                final List<String> lines = ignore.asLines();
                for (String glob : lines) {
                    glob = glob.endsWith("/") ? glob + "**/*" : glob;
                    pathMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
                }
                return new ESLintIgnore(fileObject, pathMatchers);
            } catch (IOException iOException) {
                Logger.getLogger(ESLintIgnore.class.getName()).log(Level.WARNING, "Failed to read ignore file");
                return new ESLintIgnore(fileObject, Collections.EMPTY_LIST);
            }
        } else if (fileObject.isFolder()) {
            return new ESLintIgnore(fileObject, Collections.EMPTY_LIST);

        } else {
            throw new IllegalArgumentException("Not a folder " + fileObject);
        }
    }

    public boolean isIgnored(FileObject fileObject) {
        Path filePath = FileUtil.toFile(fileObject).toPath();
        Path dirPath = FileUtil.toFile(root).toPath();
        final Path relPath = dirPath.relativize(filePath);
        for (PathMatcher pathMatcher : pathMatchers) {
            if (pathMatcher.matches(relPath)) {
                return true;
            }
        }

        return false;
    }
}
