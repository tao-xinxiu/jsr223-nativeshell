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
package jsr223.nativeshell;

import javax.script.ScriptException;

import jsr223.nativeshell.bash.Bash;
import jsr223.nativeshell.cmd.Cmd;


public class Main {

    public static void main(String[] args) throws ScriptException {
        NativeShell shell = null;
        if ("cmd".equals(args[0])) {
            shell = new Cmd();
        } else if ("bash".equals(args[0])) {
            shell = new Bash();
        } else {
            System.err.println("First argument must be shell name (cmd/bash)");
            System.exit(-1);
        }

        String script = "";
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            script += arg + " ";
        }

        Object returnCode = new NativeShellScriptEngine(shell).eval(script);
        System.exit((Integer) returnCode);
    }
}
