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

import java.io.Reader;
import java.io.Serializable;
import java.util.Map;

import javax.script.*;

import org.ow2.proactive.scheduler.common.SchedulerConstants;


public class NativeShellScriptEngine extends AbstractScriptEngine {

    public static final String ENABLE_VERSION_PROPERTY_NAME = "jsr223.nativeshell.enableVersionCheck";

    public static final String DEFAULT_VERSION = "1.0.0";

    public static final String DEFAULT_MAJOR_VERSION = "1";

    public static final String EXIT_VALUE_BINDING_NAME = "EXIT_VALUE";

    private NativeShell nativeShell;

    public NativeShellScriptEngine(NativeShell nativeShell) {
        this.nativeShell = nativeShell;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        int exitValue = new NativeShellRunner(nativeShell).run(script, context);
        if (context.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(SchedulerConstants.VARIABLES_BINDING_NAME)) {
            Map<String, Serializable> variables = (Map<String, Serializable>) context.getBindings(ScriptContext.ENGINE_SCOPE)
                                                                                     .get(SchedulerConstants.VARIABLES_BINDING_NAME);
            variables.put(EXIT_VALUE_BINDING_NAME, exitValue);
        }
        context.getBindings(ScriptContext.ENGINE_SCOPE).put(EXIT_VALUE_BINDING_NAME, exitValue);
        if (exitValue != 0) {
            throw new ScriptException("Script failed with exit code " + exitValue);
        }
        return exitValue;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return eval(IOUtils.toString(reader), context);
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return nativeShell.getScriptEngineFactory();
    }
}
