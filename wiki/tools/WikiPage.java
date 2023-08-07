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
package wiki.tools;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import info.bliki.extensions.scribunto.engine.lua.ScribuntoLuaEngine;

import wiki.TemplateParser;

import wiki.NameSpaces.NameSpace;
import static wiki.NameSpaces.getNameSpace;
import static wiki.NameSpaces.getNameSpaceNumber;
import static wiki.tools.Utilities.getResourceBundle;
import static wiki.tools.Utilities.getResourceString;
import static wiki.tools.Utilities.flipTemplateName;
/*
This class in an helper for wiki page belonging to main NameSpace
*/

final public class WikiPage {
	private final String pagename;
	private final Date revision;
	private final Locale locale;
	private final TemplateParser tp;
	private final HashMap<String, String> name2template;
	private final HashMap<String, String> name2module;
	private final HashMap<String, String> name2content;//optional, may be null
	private final boolean trace_calls;
	private final boolean provide_fake_content;
	private final String redirect_alias;

	private ScribuntoLuaEngine SLE = null;

	public WikiPage(String name, Date rev, Locale locale, TemplateParser tp, 
		HashMap<String, String> name2template, HashMap<String, String> name2module, 
		boolean trace_calls, HashMap<String, String> name2content, boolean provide_fake_content) {//trace_calls and provide_fake_content are only for test purposes
		pagename = name;
		revision = rev;
		this.locale = locale;
		this.tp = tp;
		this.name2template = name2template;
		this.name2module = name2module;
		this.name2content = name2content;
		this.trace_calls = trace_calls;
		this.provide_fake_content = provide_fake_content;

		ResourceBundle resourceBundle = getResourceBundle(locale);
		if (resourceBundle != null)	{
			String _template = getResourceString(resourceBundle, "template");
			if (_template != null) {
				NameSpace template_ns = getNameSpace(10);
				template_ns.add_alias(_template);
			}
			String _module = getResourceString(resourceBundle, "module");
			if (_module != null) {
				NameSpace module_ns = getNameSpace(828);
				module_ns.add_alias(_module);
			}
			redirect_alias = getResourceString(resourceBundle, "redirect");
		} else redirect_alias = null;
	}

	public String getPagename() {
		return pagename;
	}

	public Date getRevision() {
		return revision;
	}

	public Locale getLocale() {
		return locale;
	}

	public TemplateParser getTemplateParser() {
		return tp;
	}

	public String getTemplate(String identifier) {
		String template = name2template.get(identifier.replace('_', ' '));
		if (template == null)
			return name2template.get(flipTemplateName(identifier));
		else return template;
	}

	public String getModule(String identifier) {
		String module = name2module.get(identifier.replace('_', ' '));
		if (module == null)
			return name2module.get(flipTemplateName(identifier));
		else return module;
	}

	public boolean getTrace_calls() {
		return trace_calls;
	}

	public boolean ifExists(String fullpagename) {
		boolean isTemplate = false;
		int idx = fullpagename.indexOf(":");
		if (idx != -1) {
			String ns = fullpagename.substring(0, idx);
			Integer ns_id = getNameSpaceNumber(ns);
			if (ns_id != null && ns_id == 10)
				isTemplate = true;
		}

		if (isTemplate)
			return getTemplate(fullpagename.substring(idx + 1)) != null;
		else return getContent(fullpagename) != null;
	}

	public String getContent(String fullpagename) {
		String content = null;
		if (name2content != null) {
			if (fullpagename.startsWith("interwiki:"))
				fullpagename = fullpagename.substring("interwiki:".length());
			int idx = fullpagename.indexOf("#");
			if (idx != -1)
				fullpagename = fullpagename.substring(0, idx);
			content = name2content.get(fullpagename.replace('_', ' '));
		}
		if (content == null && provide_fake_content)//only for test purposes
			return fullpagename;
		return content;
	}

	public String getRedirect(String text) {
		int ibrac;
		if (!text.isEmpty() && text.charAt(0) == '#' && ((ibrac = text.indexOf("[[")) != -1)) {
			String checkRedirect = text.substring(1, ibrac).toLowerCase();
			if (checkRedirect.startsWith("redirect") || (redirect_alias != null && checkRedirect.startsWith(redirect_alias))) {
				int icolon = text.indexOf(":", ibrac);
				int ebrac = text.indexOf("]]", ibrac);
				if ((ebrac != -1) && (icolon != -1) && (icolon < ebrac)) {
					return text.substring(icolon + 1, ebrac).trim();
				}
			}
		}
		return null;//no redirect
	}

    public ScribuntoLuaEngine createScribuntoEngine() {
		if (SLE == null)
			SLE = new ScribuntoLuaEngine(this);
        return SLE;
    }

}//end of class WikiPage