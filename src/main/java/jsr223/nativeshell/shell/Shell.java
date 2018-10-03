package jsr223.nativeshell.shell;

import java.io.File;

import javax.script.ScriptEngineFactory;

import jsr223.nativeshell.NativeShell;


public class Shell implements NativeShell {
    @Override
    public ProcessBuilder createProcess(File commandAsFile) {
        return new ProcessBuilder(commandAsFile.getAbsolutePath());
    }

    @Override
    public ProcessBuilder createProcess(String command) {
        throw new RuntimeException("One line shell sctipt is not supported: shebang nodation required");
    }

    @Override
    public String getInstalledVersionCommand() {
        return "1.0";
    }

    @Override
    public String getMajorVersionCommand() {
        return "1.0";
    }

    @Override
    public ScriptEngineFactory getScriptEngineFactory() {
        return new ShellEngineFactory();
    }

    @Override
    public String getFileExtension() {
        return ".sh";
    }
}
