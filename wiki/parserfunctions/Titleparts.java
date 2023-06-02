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
import info.bliki.extensions.scribunto.template.Frame;

//reference: https://www.mediawiki.org/wiki/Special:MyLanguage/Help:Extension:ParserFunctions

public final class Titleparts extends ParserFunction {

    public final static ParserFunction Instance = new Titleparts();

    @Override
	public String evaluate(WikiPage wp, ArrayList<String> parameters, Frame parent) {
        if (parameters.size() > 0) {
			TemplateParser tp = wp.getTemplateParser();
            String pagename = tp.parseParameter( parameters.get(0), wp, parent);
            int num = 0;
            if (parameters.size() > 1) {
                try {
                    num = Integer.parseInt(tp.parseParameter( parameters.get(1), wp, parent));
                } catch (NumberFormatException nfe) {
//ignore, default is 0
                }
            }
            int first = 1;
            if (parameters.size() > 2) {
                try {
                    first = Integer.parseInt(tp.parseParameter( parameters.get(2), wp, parent));
                    if (first == 0) {
                        first = 1;
                    }
                } catch (NumberFormatException nfe) {
//ignore, default is 1
                }
            }
			int idx;
			if (first > 0) {
				idx = -1;
				while (first > 1) {
					idx = pagename.indexOf('/', ++idx);
					if (idx < 0)
						return "";
					first--;
				}
				if (idx > 0)
					pagename = pagename.substring(idx + 1);
			} else {
				// Negative values for first segment translates to
				// "add this value to the total number of segments", loosely equivalent
				// to "count from the right":
				idx = pagename.length();
				while (first < 0) {
					idx = pagename.lastIndexOf('/', --idx);
					if (idx < 0)
						return pagename;
					first++;
				}
				pagename = pagename.substring(idx + 1);
			}
			if (num >= 0) {
				idx = -1;
				while (num > 0) {
					idx = pagename.indexOf('/', ++idx);
					if (--num == 0)
						return idx < 0 ? pagename : pagename.substring(0, idx);
					if (idx < 0)
						return pagename;
				}
			} else {
				idx = pagename.length();
				while (num < 0) {
					idx = pagename.lastIndexOf('/', --idx);
					if (++num == 0) {
						if (idx >= 0)
							break;
						return "";
					}
					if (idx < 0)
						return "";
				}
				pagename = pagename.substring(0, idx);
			}
			return pagename;
        }
        return ""; 
	}
}