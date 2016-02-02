package se.jocke.nb.eslint;

import java.beans.PropertyVetoException;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.LocalFileSystem;
import se.jocke.nb.eslint.task.ESLintIgnore;

/**
 *
 * @author jocke
 */
public class ESLintTest {

    private FileObject dir;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws PropertyVetoException, IOException {
        LocalFileSystem fs = new LocalFileSystem();
        fs.setRootDirectory(folder.getRoot());
        dir = fs.getRoot();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void shouldIgnoreNoneGivenNoIgnoreFileFound() throws IOException {
        ESLintIgnore ignore = ESLintIgnore.get(dir);
        assertFalse(ignore.isIgnored(dir.createData("test", "js")));
    }

    @Test
    public void shouldIgnoreNodeModulesAlways() throws IOException {
        ESLintIgnore ignore = ESLintIgnore.get(dir);
        FileObject nodeModules = dir.createFolder("node_modules");
        assertTrue(ignore.isIgnored(nodeModules.createData("test", "js")));
    }

}
