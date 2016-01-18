package se.jocke.nb.eslint;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.extexecution.base.BaseExecutionDescriptor;
import org.netbeans.api.extexecution.base.BaseExecutionService;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.util.NbPreferences;
import se.jocke.nb.eslint.error.ErrorReporter;
import se.jocke.nb.eslint.error.LintError;

/**
 *
 * @author jocke
 */
public class ESLint {

    private static final ESLint ES_LINT = new ESLint();
  
    private static final Pattern LINT_PATTERN = Pattern.compile("(.*):\\s+line\\s+(\\d+),\\s+col\\s(\\d+),\\s+(\\w*)\\s-(.*)");
    
    private static final Logger LOG = Logger.getLogger(ESLint.class.getName());

    public void verify(FileObject fileObject, final ErrorReporter reporter) {

        final Preferences prefs = NbPreferences.forModule(ESLint.class);

        String command = prefs.get(Constants.ESLINT_PATH, "/usr/local/bin");

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
                            LOG.log(Level.INFO, "Line {0}", string);
                            ErrorManager.getDefault().log(string);
                        }
                    }
                });
            }
        });

        final org.netbeans.api.extexecution.base.ProcessBuilder builder = org.netbeans.api.extexecution.base.ProcessBuilder.getLocal();
        
        LOG.log(Level.INFO, "Running command {0}", command);
        
        builder.setExecutable(command);
        
        builder.setArguments(Arrays.asList(
                "--config",
                prefs.get(Constants.ESLINT_CONF, Paths.get(System.getProperty("user.home"), ".eslintrc").toString()),
                "--format",
                "compact",
                fileObject.getPath()
        ));

        BaseExecutionService service = BaseExecutionService.newService(new Callable<Process>() {
            @Override
            public Process call() throws Exception {
                return builder.call();
            }
        }, descriptor);

        service.run();
    }

    public static ESLint getDefault() {
        return ES_LINT;
    }
    
    private abstract class LineProcessorAdapter implements LineProcessor {

        @Override
        public void reset() {
            
        }

        @Override
        public void close() {
            
        }
        
    }
}
