package info.bliki.extensions.scribunto.engine.lua.interfaces;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import static info.bliki.extensions.scribunto.template.Frame.toLuaString;


public class MwUri implements MwInterface {
    private final String wgServer;
    private final String wgCanonicalServer;
    private final String wgScript;
    private final String wgArticlePath;

    public MwUri() {
        wgServer = "//wiki.local";
        wgCanonicalServer = "http://wiki.local";
        boolean wgUsePathInfo = true;
        wgScript = "/w/index.php";
        String wgScriptPath = "/w";
        wgArticlePath = "/wiki/$1";
    }

    @Override
    public String name() {
        return "mw.uri";
    }

    @Override
    public LuaTable getInterface() {
        LuaTable iface = new LuaTable();
        iface.set("anchorEncode", anchorEncode());
        iface.set("localUrl", localUrl());
        iface.set("fullUrl", fullUrl());
        iface.set("canonicalUrl", canonicalUrl());
        return iface;
    }

    private LuaValue anchorEncode() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.EMPTYSTRING;
            }
        };
    }

    private LuaValue canonicalUrl() {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue page, LuaValue query) {
                if (query.isnil()) {
                    return toLuaString(wgCanonicalServer + pagePath(page));
                } else {
                    return toLuaString(wgCanonicalServer + formatQuery(page, query));
                }
            }
        };
    }

    private LuaValue fullUrl() {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue page, LuaValue query) {
                if (query.isnil()) {
                    return toLuaString(wgServer + pagePath(page));
                } else {
                    return toLuaString(wgServer + formatQuery(page, query));
                }
            }
        };
    }

    private LuaValue localUrl() {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue page, LuaValue query) {
                if (query.isnil()) {
                    return toLuaString(pagePath(page));
                } else {
                    return toLuaString(formatQuery(page, query));
                }
            }
        };
    }

    @Override
    public LuaValue getSetupOptions() {
        return new LuaTable();
    }

    private String pagePath(LuaValue page) {
        return wgArticlePath.replace("$1", page.tojstring());
    }

    private String formatQuery(LuaValue page, LuaValue query) {
        if (query.isstring()) {
            return wgScript + "?title="+page.checkstring()+"&"+query.checkjstring();
        } else if (query.istable()) {
            LuaTable params = query.checktable();

            StringBuilder base = new StringBuilder(wgScript + "?title=" + page.checkstring() + "&");
            for (LuaValue key : params.keys()) {
                base.append(key.tojstring()).append("=").append(params.get(key).tojstring());
            }
            return base.toString();
        } else {
            throw new AssertionError("unexpected type: "+query);
        }
    }
}
