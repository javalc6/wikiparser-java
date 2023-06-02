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
package wiki;

import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.LinkedHashMap;
import java.util.Map;

import wiki.parserfunctions.ParserFunction;
import wiki.parserfunctions.ParserFunctions;
import wiki.MagicWords;
import wiki.tools.WikiScanner;
import wiki.tools.WikiPage;
import static wiki.tools.Utilities.deleteAll;
import static wiki.tools.Utilities.flipTemplateName;
import static wiki.tools.Utilities.process_include;
import info.bliki.extensions.scribunto.template.Frame;
/*
The class TemplateParser implements light wiki template parser.
*/
final public class TemplateParser {
	final static boolean failsafe_parser = true;//must be true in production! false is for debug/test purposes only

	private final static String template_label = "Template:";
	private final static String lc_template_label = template_label.toLowerCase();

//main method parse string, returns evaluated string
	public String parse(String string, WikiPage wp) {//external
		StringBuilder sb = new StringBuilder();
		WikiScanner sh = new WikiScanner(delete_comments(string));
		template_body(sh, sb, sh::getStringWithoutOpening, wp, null);
        deleteAll(sb, "<nowiki>");
        deleteAll(sb, "</nowiki>");
		return sb.toString();
	}
	
	public String parseParameter(String string, WikiPage wp, Frame parent) {//internal usage (used only by parserfunctions)
		StringBuilder sb = new StringBuilder();
		WikiScanner sh = new WikiScanner(string);
		template_body(sh, sb, sh::getStringWithoutOpening, wp, parent);
		return sb.toString().trim();
	}

//template_body ::= [simple_text] { (parameter_holder | invocation ) [simple_text] }* [any text]
	private void template_body(WikiScanner sh, StringBuilder sb, Supplier<String> supplier, WikiPage wp, Frame parent) {
		String pre = supplier.get();
		if (pre != null)
			sb.append(pre);
		while (sh.getSequence("{{")) {
			int pointer = sh.getPointer(); //save pointer to be ready to retract in case of missing }}

			Character ch;
			if ((ch = sh.getCharInCharSet("{")) != null) {
//parameter_holder ::= "{{{" (parameter_name) [ "|" [default value] ] "}}}" -- default value
				boolean ph = parameter_holder(sh, sb, wp, parent);
				if (ph) {
					String str = supplier.get();//twin
					if (str != null)
						sb.append(str);
					continue;
				}
//here we have a pending literal {
				sh.setPointer(pointer - 1);//partially retract scanner
				sb.append("{");//save orphan { as literal
				continue;
			}

			int p1 = sh.getPointer();
			int p2 = sh.findNested(p1);//handling of nested template calls like {{la{{#if:1|be|re}}l|en|activity}}
			if (p2 != -1) {
				sh.setPointer(p2 + 2);

				String rep = null;
				if ((ch = sh.getCharInCharSet("{")) != null) {
//parameter_holder ::= "{{{" (parameter_name) [ "|" [default value] ] "}}}" -- default value
					StringBuilder sb2 = new StringBuilder();
					boolean ph = parameter_holder(sh, sb2, wp, parent);
					if (ph) {
						String str = supplier.get();//twin
						if (str != null)
							sb2.append(str);
						rep = sb2.toString();
					}// else rep = null;
				} else {
					rep = invocation_body(sh, wp, parent);
				}
				if (rep == null) {
					sh.dumpString("Ops, something wrong!");
					sh.setPointer(p2);
				} else {
					int end = sh.getPointer();
					sh.replaceNested(p2, end, rep);
					sh.setPointer(p1);
				}
			}

			String p = invocation_body(sh, wp, parent);
			if (p != null) {
				sb.append(p);
			} else {
				sh.setPointer(pointer);//retract scanner
				sb.append("{{");//push back unbalanced "{{"
			}
			String str = supplier.get();//twin
			if (str != null)
				sb.append(str);
		}
	}

	private boolean parameter_holder(WikiScanner sh, StringBuilder sb, WikiPage wp, Frame parent) {
/*parameter_holder ::= "{{{" (parameter_name) [ "|" [default value] ] "}}}"
Important: if param_name is not defined, then
{{{param_name}}} --> {{{param_name}}} (literal)
{{{param_name|}}} --> (empty string)
reference: https://www.mediawiki.org/wiki/Help:Parser_functions_in_templates
*/
		String param_name = sh.getIdentifierOrNumber();
		if (param_name != null) {
			String result = parent == null ? null : parent.getTemplateParameter(param_name);
			String def_value = (sh.getCharInCharSet("|")) != null ? sh.getStringParameter(null) : null;
			if (sh.getSequence("}}}")) {
				if (result == null)
					result = def_value == null ? "{{{" + param_name + "}}}" : parseParameter(def_value, wp, parent);//use literal or default value
				sb.append(result);
				return true;
			}		
		}
		return false;
	}

	private String invocation_body(WikiScanner sh, WikiPage wp, Frame parent) {
//invocation_body ::= magic_word_call | parser_function_call | template_call
//magic_word_call ::= magic_word [ ":" magic_parameter]
//parser_function_call ::= parser_function_name ":" parser_function_parameter { "|" [parser_function_parameter] }*
//template_call ::= template_identifier { "|" [template_parameter] }*
		StringBuilder sb = new StringBuilder();
		int pointer0 = sh.getPointer(); //save pointer to be ready to retract

		String identifier = sh.getTemplateIdentifier();
		if (identifier == null)
			return null;
		if (identifier.startsWith(":")) {//ignore transclusion of ordinary wiki page
			return null;
		}
		if (identifier.startsWith("subst:")) {//ignore subst:
			identifier = identifier.substring("subst:".length());
			pointer0 += "subst:".length();
		}
		if (identifier.startsWith("safesubst:")) {//ignore safesubst:
			identifier = identifier.substring("safesubst:".length());
			pointer0 += "safesubst:".length();
		}
		int pointer = sh.getPointer(); //save pointer to be ready to retract in case of invalid magic word or parser function
//check & process magic word
		int idx = identifier.indexOf(":");
		String name = idx != -1 ? identifier.substring(0, idx) : identifier;
		MagicWords.MagicWord mw = MagicWords.get(name);
		if (mw != null)	{
			String parameter = null;
			if (idx != -1) {//parameter present
				sh.setPointer(pointer0);//retract scanner at start of identifier
				sh.moveAfter(":");//move after : to get parameter
				StringBuilder param = new StringBuilder();
				template_body(sh, param, sh::getStringWithoutBracketsBar, wp, parent);
				parameter = param.toString();
				while (sh.getCharInCharSet("|") != null) {//ignore any further parameter(s)
					sh.getStringParameter(null);
				}
			}
			if (sh.getSequence("}}")) {
				String result = MagicWords.evaluate(mw, parameter, wp.getPagename(), wp.getRevision());
				if (result != null) {
					sb.append(result);
					return sb.toString();
				} else sh.setPointer(pointer);//retract scanner
			} else return null;
		}
//check & process parser function call
		ParserFunction pf = ParserFunctions.get(name);
		if (pf != null)	{
			if (idx != -1) {//first parameter present
				sh.setPointer(pointer0);//retract scanner at start of identifier
				sh.moveAfter(":");//move after : to get parameter
				StringBuilder param = new StringBuilder();
				template_body(sh, param, sh::getStringWithoutBracketsBar, wp, parent);
				ArrayList<String> parameters = new ArrayList<>();
				parameters.add(param.toString().trim());//first parameter

				Character ch;
				while ((ch = sh.getCharInCharSet("|")) != null) {//twin
					String paramx = sh.getStringParameter(null);
					parameters.add(paramx == null ? "" : paramx.trim());
				}
				if (sh.getSequence("}}")) {
					return pf.evaluate(wp, parameters, parent);
				} else return null;
			} else {//retract
				sh.setPointer(pointer);//retract scanner
			}
		}
		if (!identifier.contains("#")) {
//check & process template call
			if (identifier.toLowerCase().startsWith(lc_template_label)) {//TODO: handle also alias and language localizations
				identifier = identifier.substring(template_label.length());//remove template namespace
			}
			Character ch; int pos = 1;
			Map<String, String> parameterMap = new LinkedHashMap<>();
			while ((ch = sh.getCharInCharSet("|")) != null) {//twin
				String param_name = "";
				int[] equalPos = {-1};
				String paramx = sh.getStringParameter(equalPos);
				String value = "";
				if (paramx != null) {
					idx = equalPos[0];//findValidEqualSign(paramx);
//		System.out.println("parameter splitting: " + paramx + ", idx= " + idx);
					if (idx != -1 && !(param_name = paramx.substring(0, idx).trim()).isEmpty()) {//named parameter
						value = paramx.substring(idx + 1);//skip "="
					} else {//unnamed parameter
						value = paramx;
					}
				}
				if (param_name.isEmpty())
					param_name = Integer.toString(pos++);//unnamed parameter
//System.out.println("param_name="+param_name+", param_value="+value);
				parameterMap.put(param_name, parseParameter(value, wp, parent));
			}
			if (sh.getSequence("}}")) {
				return getParsedTemplate(identifier.replace('_', ' '), wp, parameterMap, parent);
			} else return null;
		} else if (failsafe_parser)
			return null;
		else throw new RuntimeException("unexpected identifier: " + identifier);
	}

	public String getParsedTemplate(String identifier, WikiPage wp, Map<String, String> parameterMap, Frame parent) {
		boolean trace_calls = wp.getTrace_calls();

		while (true) {
			String parsed_template = null;
			String template_text = wp.getTemplate(identifier);
			if (template_text != null) {
				if (!detect_loop(identifier, parent)) {
					template_text = process_include(delete_comments(template_text), true).replace("{{{|safesubst:}}}", "");//twin in TestSuite
					String redirect = getRedirect(template_text);
					if (redirect != null) {
						identifier = redirect;
						continue;
					}

					if (trace_calls) {
						System.out.print(template_label + identifier + "(");
						parameterMap.forEach((name, value) -> System.out.print(name + (value.isEmpty() ? "" : " = " + value) + ", "));
						System.out.println(")");
					}
					Frame frame = new Frame(template_label + identifier, parameterMap, parent, true);//frame of this template
					StringBuilder sb = new StringBuilder();
					WikiScanner sh = new WikiScanner(delete_comments(template_text));
					template_body(sh, sb, sh::getStringWithoutOpening, wp, frame);
					parsed_template = sb.toString();
				}
			}
			if (parsed_template != null) {
				return parsed_template;
			} else {
				if (trace_calls)
					System.out.println("Warning: template not found:" + identifier);
				return "[["+ template_label + identifier + "]]";
			}
		}
	}

	private boolean detect_loop(String identifier, Frame parent) {
		while (parent != null) {
			String parentpage = parent.getPage();
			if (identifier.equals(parentpage))
				return true;
			if (identifier.equals(flipTemplateName(parentpage)))
				return true;
			parent = parent.getParent();
		}
		return false;
	}

	public String getRedirect(String template_text) {
		int ibrac;
		if (template_text.length() > 9 && template_text.charAt(0) == '#' && ((ibrac = template_text.indexOf("[[")) != -1)) {
			String checkRedirect = template_text.substring(0, ibrac).toLowerCase();
			if (checkRedirect.startsWith("#redirect")) {
				int icolon = template_text.indexOf(":", ibrac);
				int ebrac = template_text.indexOf("]]", ibrac);
				if ((ebrac != -1) && (icolon != -1) && (icolon < ebrac)) {
					return template_text.substring(icolon + 1, ebrac).trim();
				}
			}
		}
		return null;//no redirect
	}

	private static String delete_comments(String str) { // delete html comments <!-- -->
		StringBuilder text = new StringBuilder(str);
		int comment;
		while ((comment = text.indexOf("<!--")) != -1) {
			int eoc = text.indexOf("-->", comment + 3);
			if (eoc != -1) {
				text.delete(comment, eoc + 3);
			} else {
				text.setLength(comment);
				break;
			}
		}
		return text.toString();
	}

}
