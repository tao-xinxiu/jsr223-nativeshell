package jsr223.nativeshell.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;


public class ShellScriptEngineFactoryTest {

    @Test
    public void testShellScriptEngineIsFound() {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

        assertNotNull(scriptEngineManager.getEngineByExtension("sh"));
        assertNotNull(scriptEngineManager.getEngineByName("shell"));
        assertEquals("shell", scriptEngineManager.getEngineByMimeType("application/x-sh").getFactory().getEngineName());
    }

    @Test
    public void testBashScriptEngineVersions() {
        ScriptEngine bashScriptEngine = new ScriptEngineManager().getEngineByExtension("sh");

        assertNotNull(bashScriptEngine.getFactory().getEngineVersion());
        assertNotNull(bashScriptEngine.getFactory().getLanguageVersion());
    }
}
