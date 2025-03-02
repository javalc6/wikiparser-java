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

import wiki.parserfunctions.expr.ExprParser;
import wiki.TemplateParser;
import wiki.tools.WikiPage;
import info.bliki.extensions.scribunto.template.Frame;

//reference: https://www.mediawiki.org/wiki/Special:MyLanguage/Help:Extension:ParserFunctions

public final class Expr extends ParserFunction {

    public final static ParserFunction Instance = new Expr();

    @Override
	public String evaluate(WikiPage wp, ArrayList<String> parameters, Frame parent) {
        if (!parameters.isEmpty()) {
			TemplateParser tp = wp.getTemplateParser();
			String expression = tp.parseParameter( parameters.get(0), wp, parent);
			if (!expression.isEmpty()) {
				try {
					ExprParser ep = new ExprParser();
					return ep.doExpression(expression);
				} catch (Exception e) {
					// e.printStackTrace();
					return format_error("Expression error: " + e.getMessage());
				}
			}
        }
        return ""; 
	}

}