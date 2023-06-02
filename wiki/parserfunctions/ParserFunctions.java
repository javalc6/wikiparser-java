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

import java.util.HashMap;

/* references:
https://www.mediawiki.org/wiki/Help:Extension:ParserFunctions
https://www.mediawiki.org/wiki/Manual:Namespace#Built-in_namespaces
https://www.mediawiki.org/wiki/Parser_function_hooks
https://github.com/wikimedia/mediawiki-extensions-ParserFunctions/blob/master/includes/ParserFunctions.php
 */
public final class ParserFunctions {
	    private static final HashMap<String, ParserFunction> parserFunctionMap = new HashMap<>();
		static {
			parserFunctionMap.put("formatnum", Formatnum.Instance);
			parserFunctionMap.put("fullurl", Fullurl.Instance);
			parserFunctionMap.put("fullurle", Fullurle.Instance);
			parserFunctionMap.put("localurl", Localurl.Instance);
			parserFunctionMap.put("localurle", Localurle.Instance);
			parserFunctionMap.put("padleft", Padleft.Instance);
			parserFunctionMap.put("padright", Padright.Instance);
			parserFunctionMap.put("plural", Plural.Instance);
			parserFunctionMap.put("#dateformat", FormatDate.Instance);
			parserFunctionMap.put("#expr", Expr.Instance);
			parserFunctionMap.put("#formatdate", FormatDate.Instance);
			parserFunctionMap.put("#if", If.Instance);
			parserFunctionMap.put("#ifeq", Ifeq.Instance);
			parserFunctionMap.put("#iferror", Iferror.Instance);
			parserFunctionMap.put("#ifexist", Ifexist.Instance);
			parserFunctionMap.put("#ifexpr", Ifexpr.Instance);
			parserFunctionMap.put("#invoke", Invoke.Instance);
			parserFunctionMap.put("#switch", Switch.Instance);
			parserFunctionMap.put("#tag", Tag.Instance);
			parserFunctionMap.put("#time", Time.Instance);
			parserFunctionMap.put("#timel", Time.Instance);
			parserFunctionMap.put("#titleparts", Titleparts.Instance);
		}

	public static boolean contains(String name) {
		return parserFunctionMap.containsKey(name.toLowerCase());
    }

    public static ParserFunction get(String name) {
		return parserFunctionMap.get(name.toLowerCase());
    }


}
