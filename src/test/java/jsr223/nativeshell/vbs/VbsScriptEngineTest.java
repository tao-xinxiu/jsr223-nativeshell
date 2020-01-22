/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package jsr223.nativeshell.vbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.StringReader;
import java.io.StringWriter;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;


public class VbsScriptEngineTest {

    public static final String SCRIPT_READ_ENV_VAR = "Set wshShell = CreateObject( \"WScript.Shell\" )\n" +
                                                     "Set wshProcessEnv = wshShell.Environment( \"PROCESS\" )\n" +
                                                     "WScript.Echo wshProcessEnv( \"string\" )";

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
        scriptEngine = new NativeShellScriptEngine(new Vbs());
        scriptOutput = new StringWriter();
        scriptEngine.getContext().setWriter(scriptOutput);
        scriptError = new StringWriter();
        scriptEngine.getContext().setErrorWriter(scriptError);
    }

    @Test
    public void evaluate_echo_command() throws Exception {
        Integer returnCode = (Integer) scriptEngine.eval("Wscript.Echo \"hello\"");

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
            // only compilation error induce non-zero exit value, unfortunately
            returnCode = (Integer) scriptEngine.eval("Wscript.Echo \"hello");
        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        assertNull(returnCode);
        assertNotEquals(NativeShellRunner.RETURN_CODE_OK,
                        scriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertTrue(scriptError.toString().length() > 0);
    }

    @Test
    public void evaluate_use_bindings() throws Exception {
        ScriptEngine vbsScriptEngine = scriptEngine;

        vbsScriptEngine.put("string", "aString");
        vbsScriptEngine.put("integer", 42);
        vbsScriptEngine.put("float", 42.0);

        Integer returnCode = (Integer) vbsScriptEngine.eval("Set wshShell = CreateObject( \"WScript.Shell\" )\n" +
                                                            "Set wshProcessEnv = wshShell.Environment( \"PROCESS\" )\n" +
                                                            "WScript.Echo wshProcessEnv( \"string\" ) & \" \" & wshProcessEnv( \"integer\" ) & \" \" & wshProcessEnv( \"float\" )");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     vbsScriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertEquals("aString 42 42.0" + nl, scriptOutput.toString());
    }

    @Test
    public void evaluate_different_calls_with_bindings() throws Exception {
        ScriptEngine vbsScriptEngine = scriptEngine;

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("string", "aString");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, vbsScriptEngine.eval(SCRIPT_READ_ENV_VAR, bindings));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     vbsScriptEngine.eval(new StringReader(SCRIPT_READ_ENV_VAR), bindings));
        assertEquals("aString" + nl + "aString" + nl, scriptOutput.toString());
    }

    @Test
    public void evaluate_different_calls_with_context() throws Exception {
        ScriptEngine vbsScriptEngine = scriptEngine;

        SimpleScriptContext context = new SimpleScriptContext();
        context.setAttribute("string", "aString", ScriptContext.ENGINE_SCOPE);
        context.setWriter(scriptOutput);
        context.setErrorWriter(scriptError);

        assertEquals(NativeShellRunner.RETURN_CODE_OK, vbsScriptEngine.eval(SCRIPT_READ_ENV_VAR, context));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     vbsScriptEngine.eval(new StringReader(SCRIPT_READ_ENV_VAR), context));
        assertEquals("aString" + nl + "aString" + nl, scriptOutput.toString());
    }

    @Test
    public void evaluate_script_with_large_output() throws Exception {
        ScriptEngine vbsScriptEngine = scriptEngine;

        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     vbsScriptEngine.eval("For x = 1 To 10000\n" + "     WScript.Echo x\n" + "   Next"));
        assertTrue(scriptOutput.toString().contains("10000"));
    }

    @Test
    public void evaluate_large_script() throws Exception {
        ScriptEngine vbsScriptEngine = scriptEngine;

        String largeScript = "";
        for (int i = 0; i < 5000; i++) {
            largeScript += "WScript.Echo \"hello" + i + "\"" + nl;
        }

        assertEquals(NativeShellRunner.RETURN_CODE_OK, vbsScriptEngine.eval(largeScript));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     vbsScriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertTrue(scriptOutput.toString().contains("hello4999"));
    }
}
