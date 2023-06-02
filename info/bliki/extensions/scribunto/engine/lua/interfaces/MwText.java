package info.bliki.extensions.scribunto.engine.lua.interfaces;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import static info.bliki.extensions.scribunto.template.Frame.toLuaString;

// https://github.com/wikimedia/mediawiki-extensions-Scribunto/blob/master/includes/Engines/LuaCommon/TextLibrary.php
public class MwText implements MwInterface {
    @Override
    public String name() {
        return "mw.text";
    }

    @Override
    public LuaTable getInterface() {
        LuaTable table = new LuaTable();

        table.set("unstrip", unstrip());
        table.set("unstripNoWiki", unstripNoWiki());
        table.set("killMarkers", killMarkers());
        table.set("getEntityTable", getEntityTable());
        table.set("jsonEncode", jsonEncode());
        table.set("jsonDecode", jsonDecode());
        return table;
    }

    // Replaces MediaWiki <nowiki> strip markers with the corresponding text.
    // Other types of strip markers are not changed.
    private LuaValue unstripNoWiki() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return toLuaString(unstripNoWiki(arg.tojstring()));
            }
        };
    }
	public static String unstripTag(String str, String tag) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i = 0, marker;

		String nowiki_marker = MwHtml.MARKER_PREFIX + "-" + tag;
        while (i < length && ((marker = str.indexOf(nowiki_marker, i)) != -1)) {
			sb.append(str, i, marker);
			i = marker + nowiki_marker.length();
			int end = str.indexOf(MwHtml.MARKER_SUFFIX, i);
			if (end != -1) {
				sb.append("<");
				sb.append(tag);
				sb.append(">");
				i = end + MwHtml.MARKER_SUFFIX.length();
			} else break;
        }
		if (i < length)
			sb.append(str, i, length);
        
        return sb.toString();
	}
	public static String unstripNoWiki(String str) {
		return str.contains("nowiki") ? unstripTag(unstripTag(str, "nowiki"), "/nowiki") : str;
	}

    // Removes all MediaWiki strip markers from a string. \127'"`UNIQ--tagname-8 hex digits-QINU`"'\127
    private LuaValue killMarkers() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return toLuaString(killMarkers(arg.tojstring()));
            }
        };
    }

	private static String killMarkers(String str) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i = 0, marker;
        
        while (i < length && ((marker = str.indexOf(MwHtml.MARKER_PREFIX, i)) != -1)) {
			sb.append(str, i, marker);
			i = marker + MwHtml.MARKER_PREFIX.length();
			int end = str.indexOf(MwHtml.MARKER_SUFFIX, i);
			if (end != -1) {
				i = end + MwHtml.MARKER_SUFFIX.length();
			} else break;
        }
		if (i < length)
			sb.append(str, i, length);
        
        return sb.toString();
	}

    // includes/json/FormatJson.php, mostly wrapper around PHP's json_encode
    private LuaValue jsonEncode() {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue value, LuaValue flags) {
				System.out.println("warning: jsonEncode was called, but it is not implemented");
                return null;
            }
        };
    }

    // includes/json/FormatJson.php
    private LuaValue jsonDecode() {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue json, LuaValue flags) {
				System.out.println("warning: jsonDecode was called, but it is not implemented");
                return null;
            }
        };
    }

    private LuaValue getEntityTable() {
        return new ZeroArgFunction() {
            @Override
            public LuaValue call() {
				System.out.println("warning: getEntityTable was called, but it is not implemented");
                return NIL;
            }
        };
    }

    private LuaValue unstrip() {//killMarkers(unstripNoWiki(s))
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return toLuaString(killMarkers(unstripNoWiki(arg.tojstring())));
            }
        };
    }

    @Override
    public LuaValue getSetupOptions() {
        LuaTable table = new LuaTable();
        table.set("nowiki_protocols", new LuaTable());
        table.set("comma", ", ");
        table.set("and", " and ");
        table.set("ellipsis", "...");
        return table;
    }
}
