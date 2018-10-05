package jsr223.nativeshell;

import java.io.File;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;


public interface NativeShell {
    ProcessBuilder createProcess(File commandAsFile) throws ScriptException;

    ProcessBuilder createProcess(String command);

    String getInstalledVersionCommand();

    String getMajorVersionCommand();

    ScriptEngineFactory getScriptEngineFactory();

    String getFileExtension();
}
