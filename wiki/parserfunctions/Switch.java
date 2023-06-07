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

import wiki.TemplateParser;
import wiki.tools.WikiPage;
import static wiki.tools.Utilities.findValidEqualSign;
import info.bliki.extensions.scribunto.template.Frame;

//reference: https://www.mediawiki.org/wiki/Special:MyLanguage/Help:Extension:ParserFunctions

public final class Switch extends ParserFunction {

    public final static ParserFunction Instance = new Switch();

    @Override
	public String evaluate(WikiPage wp, ArrayList<String> parameters, Frame parent) {
        if (parameters.size() > 1) {
			TemplateParser tp = wp.getTemplateParser();
			String comparison = tp.parseParameter( parameters.get(0), wp, parent);
			String defaultResult = "";
			boolean fallthrough = false;
			for (int i = 1; i < parameters.size(); i++) {
				String value =  parameters.get(i);
				int idx = findValidEqualSign(value);

				String left = tp.parseParameter(idx != -1 ? value.substring(0, idx).trim() : value, wp, parent);
				if (idx != -1) {
					if ("#default".equals(left)) {
						defaultResult = tp.parseParameter(value.substring(idx + 1), wp, parent).trim();
						continue;
					}
					if (fallthrough || equals(comparison, left))
						return tp.parseParameter(value.substring(idx + 1), wp, parent).trim();
				} else {
					if (i == parameters.size() - 1)//last ?
						return left;
					else if (equals(comparison, left))
						fallthrough = true;
				}
			}
			return defaultResult;
        }
        return ""; 
	}

	private boolean equals(String first, String second) {
		try {
			return Double.parseDouble(first) == Double.parseDouble(second);
		} catch (NumberFormatException e) {
			return first.equals(second);
		}
	}


}