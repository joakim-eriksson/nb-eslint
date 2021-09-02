package se.jocke.nb.eslint;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.netbeans.api.extexecution.base.BaseExecutionDescriptor;
import org.netbeans.api.extexecution.base.BaseExecutionService;
import org.netbeans.api.extexecution.base.ProcessBuilder;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.*;
import org.openide.awt.NotificationDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import se.jocke.nb.eslint.error.ErrorReporter;
import se.jocke.nb.eslint.error.LintError;
import se.jocke.nb.eslint.ui.options.ESLintOptionsModel;
import se.jocke.nb.eslint.ui.options.ESLintOptionsPanelController;

/**
 *
 * @author jocke
 */
public class ESLint {
    private static final ESLint ES_LINT = new ESLint();
    private static final Logger LOG = Logger.getLogger(ESLint.class.getName());
    public static final String ESLINT_CLI_NAME;

    static {
        if (Utilities.isWindows()) {
            ESLINT_CLI_NAME = "eslint.cmd"; // NOI18N
        } else {
            ESLINT_CLI_NAME = "eslint"; // NOI18N
        }
    }

    public Future<Integer> verify(final FileObject fileObject, final ErrorReporter reporter) {
        if (ESLintOptionsModel.getDefault().getESLintConfigOption().equals("manual")) {
            String command = ESLintOptionsModel.getDefault().getESLintPathOption();

            BaseExecutionDescriptor descriptor = new BaseExecutionDescriptor();

            descriptor = descriptor.errProcessorFactory(() -> InputProcessors.bridge(new LineProcessorAdapter() {
                @Override
                public void processLine(String string) {
                    ErrorManager.getDefault().log(string);
                }
            }));

            descriptor = descriptor.outProcessorFactory(() -> InputProcessors.bridge(new LineProcessorAdapter() {
                @Override
                public void processLine(String string) {
                    try {
                        final JSONParser jsonParser = new JSONParser();
                        final JSONArray jsonArray = (JSONArray) jsonParser.parse(string);

                        JSONObject lintObject = (JSONObject) jsonArray.get(0);
                        String filePath = lintObject.get("filePath").toString();

                        if (filePath.equals(FileUtil.toFile(fileObject).getAbsolutePath())) {
                            JSONArray messages = (JSONArray) lintObject.get("messages");

                            for (Object lintMessage : messages) {
                                String file = filePath;
                                int line = Integer.parseInt(((JSONObject) lintMessage).get("line").toString());
                                int columnStart = Integer.parseInt(((JSONObject) lintMessage).get("column").toString());
                                int columnEnd = Integer.parseInt(((JSONObject) lintMessage).get("endColumn").toString());
                                String message = ((JSONObject) lintMessage).get("message").toString();
                                int severity = Integer.parseInt(((JSONObject) lintMessage).get("severity").toString());

                                reporter.handle(new LintError(file, line, columnStart, columnEnd, severity, message));
                            }
                        }
                    } catch (ParseException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }

                @Override
                public void close() {
                    LOG.log(Level.FINE, "Scanning done");
                    reporter.done();
                }

            }));

            final ProcessBuilder builder = ProcessBuilder.getLocal();

            if (!command.isEmpty()) {
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

                final String config = findConfig();

                if (config.isEmpty()) {
                    LOG.log(Level.INFO, "Using project related config");
                } else {
                    LOG.log(Level.INFO, "Using custom config: {0}", config);
                    builder.setArguments(Arrays.asList(
                            "--config",
                            config));
                }

                builder.setArguments(Arrays.asList("--format",
                        "json",
                        fileObject.isFolder() ? "." : FileUtil.toFile(fileObject).getAbsolutePath()
                ));

                LOG.log(Level.INFO, "Running command {0}", command);

                service = BaseExecutionService.newService(() -> {
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
                }, descriptor);

                return service.run();
            }
        }

        return null;
    }

    private String findConfig() {
        if (ESLintOptionsModel.getDefault().getUseCustomConfigOption()) {
            return ESLintOptionsModel.getDefault().getCustomConfigPathOption();
        }

        return "";
    }

    public static ESLint getDefault() {
        return ES_LINT;
    }
}
