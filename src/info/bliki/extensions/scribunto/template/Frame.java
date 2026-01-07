package info.bliki.extensions.scribunto.template;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.Map;

import java.nio.charset.StandardCharsets;

import static wiki.NameSpaces.isNameSpace;
import static wiki.tools.Utilities.normaliseTitle;

/*reference: 
https://www.mediawiki.org/wiki/Extension:Scribunto/Lua_reference_manual#mw.getCurrentFrame
https://www.mediawiki.org/wiki/Extension:Scribunto/Lua_reference_manual#frame-object
*/

public final class Frame {
    private final String page;
    private final Map<String, String> templateParameters;
    private final Frame parent;
    private final boolean isSubst;

    public Frame(String page, Map<String, String> templateParameters, Frame parent, boolean isSubst) {
        this.templateParameters = templateParameters;
        this.page = page;
        this.parent = parent;
        this.isSubst = isSubst;
    }

    public Frame newChild(String pageName, Map<String, String> templateParameters, boolean isSubst) {
        return new Frame(pageName, templateParameters, this, isSubst);
    }

    public LuaValue getArgument(String name) {
        String value = templateParameters != null ? templateParameters.get(name) : null;
        if (value != null) {
            return toLuaString(value);
        } else {
            return LuaValue.NIL;
        }
    }

    public Map<String, String> getTemplateParameters() {
        return new HashMap<>(templateParameters);
    }

	public String getTemplateParameter(String name) {
		return templateParameters.get(name);
	}

    public LuaValue getAllArguments() {
        LuaTable table = new LuaTable();
        for (Map.Entry<String, String> entry: templateParameters.entrySet()) {
            try {
                final int numberedParam = Integer.parseInt(entry.getKey());
                table.set(LuaValue.valueOf(numberedParam), toLuaString(entry.getValue()));
            } catch (NumberFormatException e) {
                table.set(toLuaString(entry.getKey()), toLuaString(entry.getValue()));
            }
        }
        return table;
    }

	public static LuaString toLuaString(String string) {
        return LuaString.valueOf(string.getBytes(StandardCharsets.UTF_8));
    }

    public Frame getParent() {
        return parent;
    }

    public String getTitle() {//fullPagename
        return page;
    }

    public String getPage() {//page name without namespace
        int idx = page.indexOf(':');
        if (idx != -1) {
            String namespace = normaliseTitle(page.substring(0, idx), true);
            if (namespace.length() > 0 && isNameSpace(namespace))
                return page.substring(idx + 1);
        }
        return page;
    }

    public String getNamespace() {
        int idx = page.indexOf(':');
        if (idx != -1) {
            String namespace = normaliseTitle(page.substring(0, idx), true);
            if (namespace.length() > 0 && isNameSpace(namespace))
                return namespace;
        }
        return "";
    }

    public boolean isSubsting() {
        return isSubst;
    }

    public String getFrameId() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }
}
