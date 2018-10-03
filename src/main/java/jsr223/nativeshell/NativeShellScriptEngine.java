package jsr223.nativeshell;

import java.io.Reader;
import java.io.Serializable;
import java.util.Map;

import javax.script.*;


public class NativeShellScriptEngine extends AbstractScriptEngine {

    public static final String ENABLE_VERSION_PROPERTY_NAME = "jsr223.nativeshell.enableVersionCheck";

    public static final String DEFAULT_VERSION = "1.0.0";

    public static final String DEFAULT_MAJOR_VERSION = "1";

    public static final String EXIT_VALUE_BINDING_NAME = "EXIT_VALUE";

    public static final String VARIABLES_BINDING_NAME = "variables";

    private NativeShell nativeShell;

    public NativeShellScriptEngine(NativeShell nativeShell) {
        this.nativeShell = nativeShell;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        int exitValue = new NativeShellRunner(nativeShell).run(script, context);
        if (context.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(VARIABLES_BINDING_NAME)) {
            Map<String, Serializable> variables = (Map<String, Serializable>) context.getBindings(ScriptContext.ENGINE_SCOPE)
                                                                                     .get(VARIABLES_BINDING_NAME);
            variables.put(EXIT_VALUE_BINDING_NAME, exitValue);
        }
        context.getBindings(ScriptContext.ENGINE_SCOPE).put(EXIT_VALUE_BINDING_NAME, exitValue);
        if (exitValue != 0) {
            throw new ScriptException("Script failed with exit code " + exitValue);
        }
        return exitValue;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return eval(IOUtils.toString(reader), context);
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return nativeShell.getScriptEngineFactory();
    }
}
