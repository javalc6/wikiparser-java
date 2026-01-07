package info.bliki.extensions.scribunto.engine.lua.interfaces;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import wiki.tools.WikiPage;
import static wiki.NameSpaces.getNameSpaceByNumber;
import static wiki.NameSpaces.getNameSpaceNumber;

import static info.bliki.extensions.scribunto.template.Frame.toLuaString;

import static org.luaj.vm2.LuaValue.EMPTYSTRING;

// https://github.com/wikimedia/mediawiki-extensions-Scribunto/blob/master/includes/Engines/LuaCommon/TitleLibrary.php
// title structure: [interwiki:][namespace:]pagename[#fragment]
public class MwTitle implements MwInterface {
    private final WikiPage wp;

    public MwTitle(WikiPage wp) {
        this.wp = wp;
    }

    @Override
    public String name() {
        return "mw.title";
    }

    @Override
    public LuaTable getInterface() {
        LuaTable table = new LuaTable();
        table.set("newTitle", newTitle());
        table.set("makeTitle", makeTitle());
        table.set("getExpensiveData", getExpensiveData());
        table.set("getUrl", getUrl());
        table.set("getContent", getContent());
        table.set("fileExists", fileExists());
        table.set("getFileInfo", getFileInfo());
        table.set("protectionLevels", protectionLevels());
        table.set("cascadingProtection", cascadingProtection());
        table.set("redirectTarget", redirectTarget());//07-04-2025: added dummy method to handle issue @mw.title.lua attempt to call a nil value
        return table;
    }

    @Override
    public LuaValue getSetupOptions() {
        LuaTable table = new LuaTable();
        table.set("thisTitle", title(LuaValue.valueOf(0), toLuaString(wp.getPagename()), LuaValue.NIL, LuaValue.NIL));
        table.set("NS_MEDIA", -2);
        return table;
    }

    private LuaValue getFileInfo() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.NIL;
            }
        };
    }

    private LuaValue getExpensiveData() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return new LuaTable();
            }
        };
    }

    private LuaValue getUrl() {
        return new LibFunction() {
            /**
             *  $text, $which, $query = null, $proto = null
             */
            @Override public Varargs invoke(Varargs args) {
                return LuaValue.EMPTYSTRING;
            }
        };
    }

    private LuaValue fileExists() {
        return new OneArgFunction() {
            /**
             * @param page
             * @return Whether the file exists. For File- and Media-namespace titles, this is
             * expensive. It will also be recorded as an image usage for File- and Media-namespace titles.
             */
            @Override public LuaValue call(LuaValue page) {
                return NIL;
            }
        };
    }

    private LuaValue protectionLevels() {
        return new OneArgFunction() {
            @Override public LuaValue call(LuaValue action) {
                return new LuaTable();
            }
        };
    }

    private LuaValue cascadingProtection() {
        return new OneArgFunction() {
            @Override public LuaValue call(LuaValue action) {
                LuaTable table = new LuaTable();
                table.set("restrictions", new LuaTable());
                return table;
            }
        };
    }

    private LuaValue redirectTarget() {//07-04-2025: added dummy method to handle issue @mw.title.lua attempt to call a nil value
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.NIL;
            }
        };
    }

    private LuaValue getContent() {
        return new OneArgFunction() {
            /**
             * @param page the title of the page to fetch
             * @return the (unparsed) content of the page, or nil if there is no page.
             * The page will be recorded as a transclusion.
             */
            @Override public LuaValue call(LuaValue page) {
				String content = wp.getContent(page.tojstring());
                return content == null ? EMPTYSTRING : toLuaString(content);
            }
        };
    }

    private LuaValue newTitle() {
        return new TwoArgFunction() {
            /**
             * Handler for title.new
             *
             * @param text_or_id       string|int Title or page_id to fetch
             * @param defaultNamespace string|int Namespace name or number to use if $text_or_id doesn't override
             * @return array Lua data
             */
            @Override
            public LuaValue call(LuaValue text_or_id, LuaValue defaultNamespace) {
                if (text_or_id.isnumber()) {
                    // no database lookup
                    return NIL;
                } else if (text_or_id.isstring()) {
                    if (isValidTitle(text_or_id, defaultNamespace)) {
						String text = text_or_id.checkjstring();
						String interwiki = null;
						Integer ns_id = null;
						int idx = text.indexOf(":");
						if (idx != -1) {
							String prefix = text.substring(0, idx);
							if (prefix.equals("w") || prefix.equals("wikipedia") || prefix.equals("wikt") || prefix.equals("wiktionary"))//to be extended with more interwikis
								interwiki = prefix;
							else ns_id = getNameSpaceNumber(prefix);//ns_id != null in case prefix is namespace
						}
						int fragment_idx = text.indexOf("#");
                        return title(
                            ns_id == null ? defaultNamespace : LuaInteger.valueOf(ns_id),
                            ns_id == null ? text_or_id : toLuaString(text.substring(idx + 1)),
                            fragment_idx == -1 ? NIL : toLuaString(text.substring(fragment_idx + 1)),
                            interwiki == null ? NIL : toLuaString(interwiki));
                    } else {
                        return NIL;
                    }
                } else {
//					System.out.println("text_or_id is not a number/string: "+text_or_id);
					return NIL;
                }
            }
        };
    }

    private boolean isValidTitle(LuaValue title, LuaValue defaultNamespace) {
        // To be complete, this method would have to replicate the logic in
        // MediaWikiTitleCodec.php#splitTitleString
        //
        // https://github.com/wikimedia/mediawiki/blob/c13fee87d42bdd6fdf6764edb6f6475c14c27749/includes/title/MediaWikiTitleCodec.php#L252
        return !title.checkjstring().trim().isEmpty();
    }

    /**
     * Creates a title object with title title in namespace namespace, optionally with the
     * specified fragment and interwiki prefix. namespace may be any key found in mw.site.namespaces.
     *
     * @param $ns           string|int Namespace
     * @param $text         string Title text
     * @param $fragment     string URI fragment
     * @param $interwiki    string Interwiki code
     *
     * @return if the resulting title is not valid, returns nil.
     */
    private LuaValue makeTitle() {
        return new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaValue ns    = args.arg(1);
                LuaValue title = args.arg(2);
                LuaValue fragment = args.arg(3);
                LuaValue interwiki = args.arg(4);

                if (isValidTitle(title, ns)) {
                    return title(ns, title, fragment, interwiki);
                } else {
                    return NIL;
                }
            }
        };
    }

    private LuaValue title(LuaValue ns, LuaValue title, LuaValue fragment, LuaValue interwiki) {
		if (ns.isnil() || !ns.isint())
			ns = LuaValue.valueOf(0);
        LuaTable table = new LuaTable();
        table.set("isLocal", EMPTYSTRING);
        table.set("isRedirect", EMPTYSTRING);
        table.set("subjectNsText", EMPTYSTRING);
        table.set("interwiki", interwiki.isnil() ? EMPTYSTRING : interwiki);
        table.set("namespace", ns);
        table.set("nsText", toLuaString(getNameSpaceByNumber(ns.checkint())));
        table.set("text", title);
        table.set("id", title);
        table.set("fragment", fragment.isnil() ? EMPTYSTRING : fragment);
        table.set("contentModel", EMPTYSTRING);
        table.set("thePartialUrl", EMPTYSTRING);
        table.set("exists", LuaValue.TRUE);
		
        return table;
    }
}
