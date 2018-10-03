package jsr223.nativeshell.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.StringWriter;

import javax.script.ScriptException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;


public class ShellScriptEngineTest {

    private NativeShellScriptEngine scriptEngine;

    private StringWriter scriptOutput;

    private StringWriter scriptError;

    @BeforeClass
    public static void runOnlyOnLinux() {
        assumeTrue(System.getProperty("os.name").contains("Linux"));
    }

    @Before
    public void setup() {
        scriptEngine = new NativeShellScriptEngine(new Shell());
        scriptOutput = new StringWriter();
        scriptEngine.getContext().setWriter(scriptOutput);
        scriptError = new StringWriter();
        scriptEngine.getContext().setErrorWriter(scriptError);
    }

    @Test
    public void evaluateBashScript() throws Exception {
        Integer returnCode = (Integer) scriptEngine.eval("#!/bin/sh\n" + "ls -l ");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
    }


    @Test
    public void evaluatePerlScript() throws Exception {
        Integer returnCode = (Integer) scriptEngine.eval("#!/usr/bin/perl\nprint 'Hello';");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
        assertEquals("Hello", scriptOutput.toString());
    }


    @Test(expected = ScriptException.class)
    public void evaluate_failing_command() throws Exception {
        scriptEngine.eval("nonexistingcommandwhatsoever");
    }
}
