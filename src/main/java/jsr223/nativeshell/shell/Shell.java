package jsr223.nativeshell.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import jsr223.nativeshell.NativeShell;


public class Shell implements NativeShell {

    private static final String SHEBANG_PREFIX = "#!";

    @Override
    public ProcessBuilder createProcess(File commandAsFile) throws ScriptException {
        try (FileReader fileReader = new FileReader(commandAsFile);
             BufferedReader reader = new BufferedReader(fileReader)) {
            final String shebangLine = reader.readLine();
            if( shebangLine.startsWith(SHEBANG_PREFIX) && shebangLine.length() > SHEBANG_PREFIX.length()) {
                final String pathToInterpreter = shebangLine.substring(SHEBANG_PREFIX.length());
                final File interpreter = new File(pathToInterpreter);
                if (!interpreter.exists()) {
                    throw new ScriptException(String.format("Interpreter '%s' does not exist on the node.", pathToInterpreter));
                } else if (interpreter.isDirectory()){
                    throw new ScriptException(String.format("Path '%s' points to the directory.", pathToInterpreter));
                } else if (!interpreter.canExecute()){
                    throw new ScriptException(String.format("Interpreter '%s' cannot be executed.", pathToInterpreter));
                }
            } else if (shebangLine.toLowerCase().contains("rexx")) {
                // nothing to do because I dont know how to check if Rexx interpreter exists
            } else {
                throw new ScriptException("Incorrect shebang notation: " + shebangLine);
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
