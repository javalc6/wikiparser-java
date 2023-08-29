/*
License Information, 2023 Livio (javalc6)

Feel free to modify, re-use this software, please give appropriate
credit by referencing this Github repository.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

IMPORTANT NOTICE
Note that this software is freeware and it is not designed, licensed or
intended for use in mission critical, life support and military purposes.
The use of this software is at the risk of the user. 

DO NOT USE THIS SOFTWARE IF YOU DON'T AGREE WITH STATED CONDITIONS.
*/
package wiki.parserfunctions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import wiki.TemplateParser;
import wiki.tools.WikiPage;
import static wiki.tools.Utilities.findValidEqualSign;
import info.bliki.extensions.scribunto.template.Frame;
import info.bliki.extensions.scribunto.ScribuntoException;
import info.bliki.extensions.scribunto.engine.lua.ScribuntoLuaEngine;

import org.luaj.vm2.LuaError;
/* references:
https://www.mediawiki.org/wiki/Extension:Scribunto#.23invoke
https://www.mediawiki.org/wiki/Extension:Scribunto/Lua_reference_manual
https://github.com/wikimedia/mediawiki-extensions-Scribunto/tree/master/includes/Engines/LuaCommon
*/

public final class Invoke extends ParserFunction {

    public final static ParserFunction Instance = new Invoke();

    @Override
	public String evaluate(WikiPage wp, ArrayList<String> parameters, Frame parent) {
        if (parameters.size() > 1) {
			TemplateParser tp = wp.getTemplateParser();

			ScribuntoLuaEngine sle = wp.createScribuntoEngine();
			boolean trace_calls = wp.getTrace_calls();
			String module_name = tp.parseParameter( parameters.get(0), wp, parent);
			String function_name = tp.parseParameter( parameters.get(1), wp, parent);
			Map<String, String> parameterMap = new LinkedHashMap<>();//skip first two parameter (module and function names)
			int pos = 1;
			for (int i = 2; i < parameters.size(); i++) {
				String param = parameters.get(i);
				int idx = findValidEqualSign(param);
//System.out.println("i=" + i + ", parameter splitting: " + param + ", idx= " + idx);
				if (idx != -1) {
					parameterMap.put(param.substring(0, idx).trim(), tp.parseParameter( param.substring(idx + 1), wp, parent).trim());
				} else {
					parameterMap.put(Integer.toString(pos++), tp.parseParameter( param, wp, parent));//unnamed parameter
				}
			}
			if (trace_calls)
				System.out.println("MODULE:" + module_name + "." + function_name + "(" + parameterMap + ")");
			try {
				return sle.invoke(module_name, function_name, parent, parameterMap, false, trace_calls);
			} catch (LuaError | ScribuntoException ex) {
				if (!trace_calls)
					System.out.println("MODULE:" + module_name + "." + function_name + "(" + parameterMap + ")");
				ex.printStackTrace();//debugging
				return format_error("Module error: " + ex.getMessage());
			}
        } else return format_error("Module error: You must specify a function to call.");
	}
}