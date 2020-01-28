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
package jsr223.nativeshell.php;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ow2.proactive.scheduler.common.SchedulerConstants;

import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;


public class PhpScriptEngineTest {

    public static final String SCRIPT_READ_ENV_VAR = "<?php\n" + "echo getenv(\"string\").\"\\n\";\n" + "?>";

    public static final String PHP_ECHO_HELLO = "<?php echo \"hello\".\"\\n\"; ?>";

    private NativeShellScriptEngine scriptEngine;

    private StringWriter scriptOutput;

    private StringWriter scriptError;

    private static String nl = System.lineSeparator();

    @BeforeClass
    public static void runOnlyIfPhpIsAvailable() {
        boolean phpIsAvailable = false;
        try {
            Process p = Runtime.getRuntime().exec("php -version");
            int returnCode = p.waitFor();
            phpIsAvailable = (returnCode == 0);
        } catch (Exception e) {
            // php notAvailable
            e.printStackTrace();
        }
        assumeTrue(phpIsAvailable);
    }

    @Before
    public void setup() {
        scriptEngine = new NativeShellScriptEngine(new Php());
        scriptOutput = new StringWriter();
        scriptEngine.getContext().setWriter(scriptOutput);
        scriptError = new StringWriter();
        scriptEngine.getContext().setErrorWriter(scriptError);
    }

    @Test
    public void evaluate_echo_command() throws Exception {
        Integer returnCode = (Integer) scriptEngine.eval(PHP_ECHO_HELLO);

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     scriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertEquals("hello\n", scriptOutput.toString());
    }

    @Test
    public void evaluate_failing_command() throws Exception {
        Integer returnCode = null;
        boolean exceptionThrown = false;
        try {
            returnCode = (Integer) scriptEngine.eval("<?php\nfwrite(STDERR, \"someproblem\");\nexit(1);\n?>");
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
        ScriptEngine phpScriptEngine = scriptEngine;

        phpScriptEngine.put("string", "aString");
        phpScriptEngine.put("integer", 42);
        phpScriptEngine.put("float", 42.0);

        Integer returnCode = (Integer) phpScriptEngine.eval("<?php\n" +
                                                            "echo getenv(\"string\").\" \".getenv(\"integer\").\" \".getenv(\"float\").\"\\n\";\n" +
                                                            "?>");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     phpScriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertEquals("aString 42 42.0\n", scriptOutput.toString());
    }

    @Test
    public void evaluate_different_calls_with_bindings() throws Exception {
        ScriptEngine phpScriptEngine = scriptEngine;

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("string", "aString");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, phpScriptEngine.eval(SCRIPT_READ_ENV_VAR, bindings));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     phpScriptEngine.eval(new StringReader(SCRIPT_READ_ENV_VAR), bindings));
        assertEquals("aString\naString\n", scriptOutput.toString());
    }

    @Test
    public void evaluate_different_calls_with_context() throws Exception {
        ScriptEngine phpScriptEngine = scriptEngine;

        SimpleScriptContext context = new SimpleScriptContext();
        context.setAttribute("string", "aString", ScriptContext.ENGINE_SCOPE);
        context.setWriter(scriptOutput);
        context.setErrorWriter(scriptError);

        assertEquals(NativeShellRunner.RETURN_CODE_OK, phpScriptEngine.eval(SCRIPT_READ_ENV_VAR, context));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     phpScriptEngine.eval(new StringReader(SCRIPT_READ_ENV_VAR), context));
        assertEquals("aString\naString\n", scriptOutput.toString());
    }

    @Test
    public void evaluate_use_contentType() throws Exception {
        ScriptEngine phpScriptEngine = scriptEngine;

        Bindings bindings = phpScriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);

        bindings.put(SchedulerConstants.GENERIC_INFO_BINDING_NAME,
                     Collections.singletonMap("content.type", "text/html"));

        String script = "<html>\n" + "    <head><title>PHP Test</title></head>\n" + "    <body>" + PHP_ECHO_HELLO +
                        "    </body>\n" + "</html>";
        Object answer = phpScriptEngine.eval(script);
        assertTrue(answer instanceof byte[]);

        String outputAsString = new String((byte[]) answer);

        System.out.println(outputAsString);
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     phpScriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertEquals("<html>\n" + "    <head><title>PHP Test</title></head>\n" + "    <body>hello\n" + "    </body>\n" +
                     "</html>\n".replaceAll("\\r\\n?", "\n"), outputAsString.replaceAll("\\r\\n?", "\n"));
    }

    @Test
    public void evaluate_script_with_large_output() throws Exception {
        ScriptEngine phpScriptEngine = scriptEngine;

        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     phpScriptEngine.eval("for ($x = 0; $x <= 10000; $x++) {\n" + "     echo $x;\n" + "   }"));
        assertTrue(scriptOutput.toString().contains("10000"));
    }

    @Test
    public void evaluate_large_script() throws Exception {
        ScriptEngine phpScriptEngine = scriptEngine;

        String largeScript = "";
        for (int i = 0; i < 5000; i++) {
            largeScript += "echo \"hello" + i + "\".\"\\n\";" + nl;
        }

        assertEquals(NativeShellRunner.RETURN_CODE_OK, phpScriptEngine.eval(largeScript));
        assertEquals(NativeShellRunner.RETURN_CODE_OK,
                     phpScriptEngine.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME));
        assertTrue(scriptOutput.toString().contains("hello4999"));
    }
}
