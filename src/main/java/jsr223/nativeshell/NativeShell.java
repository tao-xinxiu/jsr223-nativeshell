package jsr223.nativeshell;

import java.io.File;

import javax.script.ScriptEngineFactory;


public interface NativeShell {
    ProcessBuilder createProcess(File commandAsFile);

    ProcessBuilder createProcess(String command);

    String getInstalledVersionCommand();

    String getMajorVersionCommand();

    ScriptEngineFactory getScriptEngineFactory();

    String getFileExtension();
}
