package jsr223.nativeshell.shell;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;

public class ShellEngineFactory implements ScriptEngineFactory {

    private static final String NAME = "shell";

    private static final String ENGINE = "Shell interpreter";

    private static final String ENGINE_VERSION = new NativeShellRunner(new Shell()).getInstalledVersion();

    private static final String LANGUAGE = "Shell";

    private static final String LANGUAGE_VERSION = new NativeShellRunner(new Shell()).getMajorVersion();

    private static final Map<String, Object> parameters = new HashMap<String, Object>();

    static {
        parameters.put(ScriptEngine.NAME, NAME);
        parameters.put(ScriptEngine.ENGINE, ENGINE);
        parameters.put(ScriptEngine.ENGINE_VERSION, ENGINE_VERSION);
        parameters.put(ScriptEngine.LANGUAGE, LANGUAGE);
        parameters.put(ScriptEngine.LANGUAGE_VERSION, LANGUAGE_VERSION);
    }

    @Override
    public String getEngineName() {
        return "shell";
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("sh");
    }

    @Override
    public List<String> getMimeTypes() {
        return Collections.singletonList("application/x-sh");
    }

    @Override
    public List<String> getNames() {
        return asList("shell", "Shell");
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    @Override
    public String getMethodCallSyntax(String s, String s1, String... strings) {
        return null;
    }

    @Override
    public String getOutputStatement(String s) {
        return null;
    }

    @Override
    public String getProgram(String... strings) {
        return null;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new NativeShellScriptEngine(new Shell());
    }
}
