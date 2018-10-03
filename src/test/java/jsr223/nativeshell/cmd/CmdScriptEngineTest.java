package jsr223.nativeshell.cmd;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.StringReader;
import java.io.StringWriter;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;


public class CmdScriptEngineTest {

    private NativeShellScriptEngine scriptEngine;

    private StringWriter scriptOutput;

    private StringWriter scriptError;

    private static String nl = System.lineSeparator();

    @BeforeClass
    public static void runOnlyOnWindows() {
        assumeTrue(System.getProperty("os.name").toLowerCase().contains("windows"));
    }

    @Before
    public void setup() {
        scriptEngine = new NativeShellScriptEngine(new Cmd());
        scriptOutput = new StringWriter();
        scriptEngine.getContext().setWriter(scriptOutput);
        scriptError = new StringWriter();
        scriptEngine.getContext().setErrorWriter(scriptError);
    }

    @Test
    public void evaluate_echo_command() throws Exception {
        Integer returnCode = (Integer) scriptEngine.eval("echo hello");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     scriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertEquals("hello" + nl, scriptOutput.toString());
    }

    @Test
    public void evaluate_failing_command() throws Exception {
        Integer returnCode = null;
        boolean exceptionThrown = false;
        try {
            returnCode = (Integer) scriptEngine.eval("nonexistingcommandwhatsoever");
        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        assertNull(returnCode);
        assertNotEquals(NativeShellRunner.RETURN_CODE_OK,
                        scriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertTrue(scriptError.toString().length() > 0);
        // Disabled the following check as it is language-specific
        //assertEquals("cmd: nonexistingcommandwhatsoever: command not found" + nl, scriptError.toString());
    }

    @Test
    public void evaluate_use_bindings() throws Exception {
        ScriptEngine bashScriptEngine = scriptEngine;

        bashScriptEngine.put("string", "aString");
        bashScriptEngine.put("integer", 42);
        bashScriptEngine.put("float", 42.0);

        Integer returnCode = (Integer) bashScriptEngine.eval("echo %string% %integer% %float%");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     bashScriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertEquals("aString 42 42.0" + nl, scriptOutput.toString());
    }

    @Test
    public void evaluate_different_calls() throws Exception {
        ScriptEngine bashScriptEngine = scriptEngine;

        assertEquals(NativeShellRunner.RETURN_CODE_OK, bashScriptEngine.eval("echo %string%"));
        assertEquals(NativeShellRunner.RETURN_CODE_OK, bashScriptEngine.eval(new StringReader("echo %string%")));
    }

    @Test
    public void evaluate_different_calls_with_bindings() throws Exception {
        ScriptEngine bashScriptEngine = scriptEngine;

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("string", "aString");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, bashScriptEngine.eval("echo %string%", bindings));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     bashScriptEngine.eval(new StringReader("echo %string%"), bindings));
        assertEquals("aString" + nl + "aString" + nl, scriptOutput.toString());
    }

    @Test
    public void evaluate_different_calls_with_context() throws Exception {
        ScriptEngine bashScriptEngine = scriptEngine;

        SimpleScriptContext context = new SimpleScriptContext();
        context.setAttribute("string", "aString", ScriptContext.ENGINE_SCOPE);
        context.setWriter(scriptOutput);
        context.setErrorWriter(scriptError);

        assertEquals(NativeShellRunner.RETURN_CODE_OK, bashScriptEngine.eval("echo %string%", context));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     bashScriptEngine.eval(new StringReader("echo %string%"), context));
        assertEquals("aString" + nl + "aString" + nl, scriptOutput.toString());
    }

    @Ignore("slow")
    @Test
    public void evaluate_script_with_large_output() throws Exception {
        ScriptEngine bashScriptEngine = scriptEngine;

        assertEquals(NativeShellRunner.RETURN_CODE_OK, bashScriptEngine.eval("FOR /L %%G IN (1,1,10000) DO echo %%G"));
        assertTrue(scriptOutput.toString().contains("10000"));
    }

    @Ignore("slow")
    @Test
    public void evaluate_large_script() throws Exception {
        ScriptEngine bashScriptEngine = scriptEngine;

        String largeScript = "";
        for (int i = 0; i < 5000; i++) {
            largeScript += "echo aString" + i + nl;
        }

        assertEquals(NativeShellRunner.RETURN_CODE_OK, bashScriptEngine.eval(largeScript));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     bashScriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertTrue(scriptOutput.toString().contains("aString4999"));
    }
}
