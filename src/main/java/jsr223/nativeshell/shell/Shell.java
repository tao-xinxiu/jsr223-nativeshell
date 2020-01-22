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
            if (shebangLine.startsWith(SHEBANG_PREFIX) && shebangLine.length() > SHEBANG_PREFIX.length()) {
                final String pathToInterpreter = shebangLine.substring(SHEBANG_PREFIX.length());
                final File interpreter = new File(pathToInterpreter);
                if (!interpreter.exists()) {
                    throw new ScriptException(String.format("Interpreter '%s' does not exist on the node.",
                                                            pathToInterpreter));
                } else if (interpreter.isDirectory()) {
                    throw new ScriptException(String.format("Path '%s' points to the directory.", pathToInterpreter));
                } else if (!interpreter.canExecute()) {
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
