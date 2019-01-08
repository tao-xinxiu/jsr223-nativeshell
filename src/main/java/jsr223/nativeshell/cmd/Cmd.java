package jsr223.nativeshell.cmd;

import java.io.File;

import javax.script.ScriptEngineFactory;

import jsr223.nativeshell.NativeShell;


public class Cmd implements NativeShell {

    @Override
    public ProcessBuilder createProcess(File commandAsFile) {
        return new ProcessBuilder("cmd", "/q", "/c", commandAsFile.getAbsolutePath());
    }

    @Override
    public ProcessBuilder createProcess(String command) {
        return new ProcessBuilder("cmd", "/c", command);
    }

    @Override
    public String getInstalledVersionCommand() {
        return "echo|set /p=%CmdExtVersion%";
    }

    @Override
    public String getMajorVersionCommand() {
        return getInstalledVersionCommand();
    }

    @Override
    public ScriptEngineFactory getScriptEngineFactory() {
        return new CmdScriptEngineFactory();
    }

    @Override
    public String getFileExtension() {
        return ".bat";
    }

}
