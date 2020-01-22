package jsr223.nativeshell;

import org.ow2.proactive.scheduler.common.SchedulerConstants;
import org.ow2.proactive.scheduler.task.SchedulerVars;
import org.ow2.proactive.utils.CookieBasedProcessTreeKiller;

import static jsr223.nativeshell.IOUtils.pipe;
import static jsr223.nativeshell.StringUtils.toEmptyStringIfNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptException;


public class NativeShellRunner {

    public static final Integer RETURN_CODE_OK = 0;

    private NativeShell nativeShell;

    public NativeShellRunner(NativeShell nativeShell) {
        this.nativeShell = nativeShell;
    }

    private boolean isVersionCheckEnabled() {
        String versionCheckProperty = System.getProperty(NativeShellScriptEngine.ENABLE_VERSION_PROPERTY_NAME);
        if (versionCheckProperty != null) {
            try {
                return Boolean.parseBoolean(versionCheckProperty);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public String getInstalledVersion() {
        if (isVersionCheckEnabled()) {
            try {
                return runAndGetOutput(nativeShell.getInstalledVersionCommand());
            } catch (Throwable e) {
                return "Could not determine version";
            }
        } else {
            return NativeShellScriptEngine.DEFAULT_VERSION;
        }
    }

    public String getMajorVersion() {
        if (isVersionCheckEnabled()) {
            try {
                return runAndGetOutput(nativeShell.getMajorVersionCommand());
            } catch (Throwable e) {
                return "Could not determine version";
            }
        } else {
            return NativeShellScriptEngine.DEFAULT_MAJOR_VERSION;
        }
    }

    public int run(String command, ScriptContext scriptContext) throws ScriptException {
        File commandAsTemporaryFile = commandAsTemporaryFile(command.trim());
        try {
            int exitValue = run(commandAsTemporaryFile, scriptContext);
            return exitValue;
        } finally {
            commandAsTemporaryFile.delete();
        }
    }

    private int run(File command, ScriptContext scriptContext) throws ScriptException{
        ProcessBuilder processBuilder = nativeShell.createProcess(command);

        processBuilder.environment();

        Map<String, String> environment = processBuilder.environment();
        addBindingsAsEnvironmentVariables(scriptContext, environment);
        CookieBasedProcessTreeKiller processTreeKiller = createProcessTreeKiller(scriptContext, environment);

        return run(processBuilder,
                   scriptContext.getReader(),
                   scriptContext.getWriter(),
                   scriptContext.getErrorWriter(), processTreeKiller);
    }

    private String runAndGetOutput(String command) {
        ProcessBuilder processBuilder = nativeShell.createProcess(command);
        StringWriter processOutput = new StringWriter();
        Reader closedInput = new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return -1;
            }

            @Override
            public void close() throws IOException {

            }
        };
        run(processBuilder, closedInput, processOutput, new StringWriter(), null);
        return processOutput.toString();
    }

    private static int run(ProcessBuilder processBuilder, Reader processInput, Writer processOutput,
                           Writer processError, final CookieBasedProcessTreeKiller processTreeKiller) {
        Process process = null;
        Thread shutdownHook = null;
        try {
            process = processBuilder.start();
            Thread input = writeProcessInput(process.getOutputStream(), processInput);
            Thread output = readProcessOutput(process.getInputStream(), processOutput);
            Thread error = readProcessOutput(process.getErrorStream(), processError);

            input.start();
            output.start();
            error.start();

            final Process shutdownHookProcessReference = process;
            shutdownHook = new Thread() {
                @Override
                public void run() {
                    destroyProcessAndWaitForItToBeDestroyed(shutdownHookProcessReference);
                    if (processTreeKiller != null) {
                        processTreeKiller.kill();
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            process.waitFor();
            output.join();
            error.join();
            input.interrupt(); // TODO better thing to do?

            return process.exitValue();
        } catch (InterruptedException interruptedException) {
            destroyProcessAndWaitForItToBeDestroyed(process);
            // Forward Interrupted Exception
            throw new RuntimeException(interruptedException);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
            if (processTreeKiller != null) {
                processTreeKiller.kill();
            }
        }
    }

    private static void destroyProcessAndWaitForItToBeDestroyed(Process process) {
        try {
            process.destroy();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addBindingsAsEnvironmentVariables(ScriptContext scriptContext, Map<String, String> environment) {
        for (Map.Entry<String, Object> binding : scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
            String bindingKey = binding.getKey();
            Object bindingValue = binding.getValue();

            if (bindingValue instanceof Object[]) {
                addArrayBindingAsEnvironmentVariable(bindingKey, (Object[]) bindingValue, environment);
            } else if (bindingValue instanceof Collection) {
                addCollectionBindingAsEnvironmentVariable(bindingKey, (Collection) bindingValue, environment);
            } else if (bindingValue instanceof Map) {
                addMapBindingAsEnvironmentVariable(bindingKey, (Map<?, ?>) bindingValue, environment);
            } else {
                environment.put(bindingKey, toEmptyStringIfNull(binding.getValue()));
            }
        }
    }

    public static CookieBasedProcessTreeKiller createProcessTreeKiller(ScriptContext scriptContext, Map<String, String> environment) {
        CookieBasedProcessTreeKiller processKiller = null;
        Map<String, String> genericInfo = (Map<String, String>) scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).get(SchedulerConstants.GENERIC_INFO_BINDING_NAME);
        Map<String, String> variables = (Map<String, String>) scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).get(SchedulerConstants.VARIABLES_BINDING_NAME);
        if (genericInfo != null && variables != null && !"true".equalsIgnoreCase(genericInfo.get(SchedulerConstants.DISABLE_PROCESS_TREE_KILLER_GENERIC_INFO))) {
            String cookieSuffix = "NativeShell_Job" + variables.get(SchedulerVars.PA_JOB_ID) + "Task" + variables.get(SchedulerVars.PA_TASK_ID);
            processKiller = CookieBasedProcessTreeKiller.createProcessChildrenKiller(cookieSuffix, environment);

        }
        return processKiller;
    }

    private void addMapBindingAsEnvironmentVariable(String bindingKey, Map<?, ?> bindingValue,
            Map<String, String> environment) {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) bindingValue).entrySet()) {
            environment.put(bindingKey + "_" + entry.getKey(),
                            (entry.getValue() == null ? "" : toEmptyStringIfNull(entry.getValue())));
        }
    }

    private void addCollectionBindingAsEnvironmentVariable(String bindingKey, Collection bindingValue,
            Map<String, String> environment) {
        Object[] bindingValueAsArray = bindingValue.toArray();
        addArrayBindingAsEnvironmentVariable(bindingKey, bindingValueAsArray, environment);
    }

    private void addArrayBindingAsEnvironmentVariable(String bindingKey, Object[] bindingValue,
            Map<String, String> environment) {
        for (int i = 0; i < bindingValue.length; i++) {
            environment.put(bindingKey + "_" + i,
                            (bindingValue[i] == null ? "" : toEmptyStringIfNull(bindingValue[i].toString())));
        }
    }

    private static Thread readProcessOutput(final InputStream processOutput, final Writer contextWriter) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pipe(new BufferedReader(new InputStreamReader(processOutput)), new BufferedWriter(contextWriter));
                } catch (IOException ignored) {
                }
            }
        });
    }

    private static Thread writeProcessInput(final OutputStream processOutput, final Reader contextWriter) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pipe(new BufferedReader(contextWriter), new OutputStreamWriter(processOutput));
                } catch (IOException closed) {
                    try {
                        processOutput.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
    }

    private File commandAsTemporaryFile(String command) {
        try {
            File commandAsFile = File.createTempFile("jsr223nativeshell-", nativeShell.getFileExtension());
            commandAsFile.setExecutable(true);
            // we add new line to the command because it is mandatory for Rexx scripts
            IOUtils.writeStringToFile(command + System.lineSeparator(), commandAsFile);
            return commandAsFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
