/*
License Information, 2024 Livio (javalc6)

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
import java.util.Locale;

import wiki.TemplateParser;
import wiki.tools.WikiPage;
import info.bliki.extensions.scribunto.template.Frame;

import static wiki.tools.Utilities.getLanguageNames;
import static wiki.tools.Utilities.getLocale;

//reference: https://www.mediawiki.org/wiki/Help:Magic_words##language
//language names: https://github.com/wikimedia/mediawiki-extensions-cldr/tree/master/CldrNames

public final class Language extends ParserFunction {

    public final static ParserFunction Instance = new Language();

    @Override
	public String evaluate(WikiPage wp, ArrayList<String> parameters, Frame parent) {
		Locale loc;
        if (parameters.size() > 0) {
			TemplateParser tp = wp.getTemplateParser();
			loc = getLocale(tp.parseParameter(parameters.get(0), wp, parent));
			if (parameters.size() > 1) {
				Locale target = getLocale(tp.parseParameter(parameters.get(1), wp, parent));
	            return loc.getDisplayLanguage(target);
			}
        } else loc = wp.getLocale();
        return loc.getDisplayLanguage(wp.getLocale());
	}
}