package se.jocke.nb.eslint;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.extexecution.base.BaseExecutionDescriptor;
import org.netbeans.api.extexecution.base.BaseExecutionService;
import org.netbeans.api.extexecution.base.ProcessBuilder;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.*;
import org.openide.awt.NotificationDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import se.jocke.nb.eslint.error.ErrorReporter;
import se.jocke.nb.eslint.error.LintError;
import se.jocke.nb.eslint.options.ESLintOptionsPanelController;

/**
 *
 * @author jocke
 */
public class ESLint {

    private static final ESLint ES_LINT = new ESLint();

    private static final Pattern LINT_PATTERN = Pattern.compile("(.*):\\s+line\\s+(\\d+),\\s+col\\s(\\d+),\\s+(\\w*)\\s-(.*)");

    private static final Logger LOG = Logger.getLogger(ESLint.class.getName());

    public static final String ESLINT_CLI_NAME;

    static {
        if (Utilities.isWindows()) {
            ESLINT_CLI_NAME = "eslint.cmd"; // NOI18N
        } else {
            ESLINT_CLI_NAME = "eslint"; // NOI18N
        }
    }

    public Future<Integer> verify(FileObject fileObject, final ErrorReporter reporter) {
        if (NbPreferences.forModule(ESLint.class).getBoolean(Constants.IS_ESLINT_ENABLED, false)) {
            final Preferences prefs = NbPreferences.forModule(ESLint.class);

            String command = prefs.get(Constants.ESLINT_PATH, "");

            BaseExecutionDescriptor descriptor = new BaseExecutionDescriptor();

            descriptor = descriptor.errProcessorFactory(new BaseExecutionDescriptor.InputProcessorFactory() {
                @Override
                public InputProcessor newInputProcessor() {
                    return InputProcessors.bridge(new LineProcessorAdapter() {
                        @Override
                        public void processLine(String string) {
                            ErrorManager.getDefault().log(string);
                        }
                    });
                }
            });

            descriptor = descriptor.outProcessorFactory(new BaseExecutionDescriptor.InputProcessorFactory() {
                @Override
                public InputProcessor newInputProcessor() {
                    return InputProcessors.bridge(new LineProcessorAdapter() {
                        @Override
                        public void processLine(String string) {
                            Matcher matcher = LINT_PATTERN.matcher(string);
                            if (matcher.matches()) {
                                String file = matcher.group(1);
                                int line = Integer.parseInt(matcher.group(2));
                                int col = Integer.parseInt(matcher.group(3));
                                String type = matcher.group(4);
                                String message = matcher.group(5);
                                reporter.handle(new LintError(file, line, col, type, message));
                            } else {
                                LOG.log(Level.FINE, "Line {0}", string);
                            }
                        }

                        @Override
                        public void close() {
                            LOG.log(Level.FINE, "Scanning done");
                            reporter.done();
                        }

                    });
                }
            });

            final ProcessBuilder builder = ProcessBuilder.getLocal();

            if (!command.isEmpty()) {
                LOG.log(Level.INFO, "Running command {0}", command);

                if (fileObject.isFolder()) {
                    builder.setWorkingDirectory(FileUtil.toFile(fileObject).getAbsolutePath());
                } else {
                    final Project owner = FileOwnerQuery.getOwner(fileObject);

                    if (owner != null) {
                        Project project = ProjectUtils.getInformation(owner).getProject();

                        if (project != null) {
                            FileObject projectDirectory = project.getProjectDirectory();
                            builder.setWorkingDirectory(projectDirectory.getPath());
                        }
                    }
                }

                BaseExecutionService service = null;

                builder.setExecutable(command.trim());

                final String config = findConfig(fileObject);

                if (config.isEmpty()) {
                    LOG.log(Level.INFO, "Using project related config");
                } else {
                    LOG.log(Level.INFO, "Using custom config: {0}", config);
                    builder.setArguments(Arrays.asList(
                            "--config",
                            config));
                }

                builder.setArguments(Arrays.asList("--format",
                        "compact",
                        fileObject.isFolder() ? "." : FileUtil.toFile(fileObject).getAbsolutePath()
                ));

                service = BaseExecutionService.newService(new Callable<Process>() {
                    @Override
                    public Process call() throws Exception {
                        try {
                            return builder.call();
                        } catch (IOException err) {
                            NotificationDisplayer.getDefault().notify("ESLint Error: Check the options", NotificationDisplayer.Priority.HIGH.getIcon(), "There is a problem while using ESLint, please check your options while clicking here. Probably the path to the ESLint CLI is not correct.", new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent arg0) {
                                    OptionsDisplayer.getDefault().open(ESLintOptionsPanelController.OPTIONS_PATH);
                                }
                            });

                            return null;
                        }
                    }
                }, descriptor);

                return service.run();
            }
        }

        return null;
    }

    public static String findConfig(FileObject fileObject) {
        Preferences prefs = NbPreferences.forModule(ESLint.class);

        if (prefs.getBoolean(Constants.ESLINT_USE_CUSTOM_CONF, false)) {
            return prefs.get(Constants.ESLINT_CONF, Paths.get(System.getProperty("user.home"), ".eslintrc.js").toString());
        }

        return "";
    }

    public static ESLint getDefault() {
        return ES_LINT;
    }
}
