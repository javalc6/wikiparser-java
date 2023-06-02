package info.bliki.extensions.scribunto.engine.lua.interfaces;

import java.util.HashMap;
import java.util.Map.Entry;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import wiki.tools.WikiPage;
import wiki.NameSpaces.NameSpace;
import static wiki.NameSpaces.getNameSpaces;
import static wiki.NameSpaces.getNameSpaceNumber;

import static info.bliki.extensions.scribunto.template.Frame.toLuaString;

import static org.luaj.vm2.LuaValue.NIL;

public class MwSite implements MwInterface {
    private final WikiPage wp;

    public MwSite(WikiPage wp) {
        this.wp = wp;
    }

    @Override
    public String name() {
        return "mw.site";
    }

    @Override
    public LuaTable getInterface() {
        LuaTable table = new LuaTable();
        table.set("getNsIndex", getNsIndex());
        table.set("pagesInCategory", pagesInCategory());
        table.set("pagesInNamespace", pagesInNamespace());
        table.set("usersInGroup", usersInGroup());
        table.set("interwikiMap", interwikiMap());
        return table;
    }

    private LuaValue interwikiMap() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                if (luaValue.isnil() || "string".equals(luaValue.typename())) {
                    return new LuaTable();
                } else {
                    throw new LuaError("bad argument #1 to 'interwikiMap' (string expected, got " + luaValue.typename() + ")");
                }
            }
        };
    }

    private LuaValue usersInGroup() {
        return new OneArgFunction() {
            @Override public LuaValue call(LuaValue group) {
                return LuaValue.valueOf(0);
            }
        };
    }

    private LuaValue pagesInNamespace() {
        return new OneArgFunction() {
            @Override public LuaValue call(LuaValue ns) {
                return LuaValue.valueOf(0);
            }
        };
    }

    private LuaValue pagesInCategory() {
        return new TwoArgFunction() {
            @Override public LuaValue call(LuaValue category, LuaValue which) {
                return LuaValue.valueOf(0);
            }
        };
    }

    private LuaValue getNsIndex() {
        return new OneArgFunction() {
            @Override
            /**
             * Get a namespace key by value, case insensitive.  Canonical namespace
             * names override custom ones defined for the current language.
             *
             * @param name String
             * @return mixed An integer if $text is a valid value otherwise false
             */
            public LuaValue call(LuaValue name) {
                Integer ns = getNameSpaceNumber(name.tojstring());
                if (ns != null) {
                    return LuaValue.valueOf(ns);
                } else {
                    return FALSE;
                }
            }
        };
    }

    @Override
    public LuaValue getSetupOptions() {
        LuaTable table = new LuaTable();
        table.set("siteName", "test");      // $GLOBALS['wgSitename'],
        table.set("server", "server");      // $GLOBALS['wgServer'],
        table.set("scriptPath", "");        // $GLOBALS['wgScriptPath'],
        table.set("stylePath",  "");        // $GLOBALS['wgStylePath'],
        table.set("currentVersion", "1.0"); // SpecialVersion::getVersion(),
        table.set("stats", stats());
        table.set("namespaces", namespaces());
        return table;
    }

    private LuaTable stats() {
        LuaTable stats = new LuaTable();

        stats.set("pages", 0);        // (int)SiteStats::pages(),
        stats.set("articles", 0);     // (int)SiteStats::articles(),
        stats.set("files", 0);        // (int)SiteStats::images(),
        stats.set("edits", 0);        // (int)SiteStats::edits(),
        stats.set("views", NIL);      // $wgDisableCounters ? null : (int)SiteStats::views(),
        stats.set("users", 0);        // (int)SiteStats::users(),
        stats.set("activeUsers", 0);  // (int)SiteStats::activeUsers(),
        stats.set("admins", 0);       // (int)SiteStats::numberingroup( 'sysop' ),
        return stats;
    }

    private LuaTable namespaces() {
        LuaTable table = new LuaTable();
		HashMap<Integer, NameSpace> namespaces = getNameSpaces();
		for (Entry<Integer, NameSpace> entry : namespaces.entrySet()) {
			table.set(entry.getKey(), luaDataForNamespace(entry.getValue()));
		}
        return table;
    }

    private LuaTable luaDataForNamespace(NameSpace namespaceValue) {
        LuaTable ns = new LuaTable();
        ns.set("id", namespaceValue.get_id());
        ns.set("name", namespaceValue.get_name().replace('_', ' '));
        ns.set("canonicalName", namespaceValue.get_canonicalName().replace('_', ' '));
        ns.set("hasSubpages", LuaValue.valueOf(namespaceValue.get_hasSubpages()));
        ns.set("hasGenderDistinction", LuaValue.valueOf(namespaceValue.get_hasGenderDistinction()));
        ns.set("isCapitalized", LuaValue.valueOf(namespaceValue.get_isCapitalized()));
        ns.set("isContent", LuaValue.valueOf(namespaceValue.get_isContent()));
        ns.set("isIncludable", LuaValue.valueOf(namespaceValue.get_isIncludable()));
        ns.set("isMovable", LuaValue.valueOf(namespaceValue.get_isMovable()));
        ns.set("isSubject", LuaValue.valueOf(namespaceValue.get_isSubject()));
        ns.set("isTalk", LuaValue.valueOf(namespaceValue.get_isTalk()));

        LuaValue[] aliases = new LuaValue[namespaceValue.get_aliases() == null ? 0 : namespaceValue.get_aliases().size()];
		if (namespaceValue.get_aliases() != null)
			for (int i = 0; i < namespaceValue.get_aliases().size(); i++) {
				aliases[i] = toLuaString(namespaceValue.get_aliases().get(i));
			}
        ns.set("aliases", LuaValue.listOf(aliases));
		ns.set("subject", namespaceValue.get_subject());
		if (namespaceValue.get_talk() != null)
			ns.set("talk", namespaceValue.get_talk());
		if (namespaceValue.get_associated() != null)
			ns.set("associated", namespaceValue.get_associated());
		else ns.set("associated", NIL);

		ns.set("defaultContentModel", NIL);//<--a che serve ?

        return ns;
    }

}
