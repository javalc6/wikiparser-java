/* Scribunto interface with Luaj based on Axel Kramer Scribunto engine: https://github.com/axkr/info.bliki.wikipedia_parser/tree/master/bliki-core/src/main/java/info/bliki/extensions/scribunto

references:
https://www.mediawiki.org/wiki/Extension:Scribunto
https://www.mediawiki.org/wiki/Extension:Scribunto/Lua_reference_manual

php & lua code:
https://github.com/wikimedia/mediawiki-extensions-Scribunto/tree/master/includes/Engines/LuaCommon

*/
package info.bliki.extensions.scribunto.engine.lua;

import info.bliki.extensions.scribunto.ScribuntoException;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwHtml;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwInit;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwInterface;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwLanguage;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwMessage;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwSite;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwText;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwTitle;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwUri;
import info.bliki.extensions.scribunto.engine.lua.interfaces.MwUstring;
import info.bliki.extensions.scribunto.template.Frame;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ResourceFinder;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import wiki.parserfunctions.ParserFunction;
import wiki.parserfunctions.ParserFunctions;
import wiki.MagicWords;
import wiki.tools.WikiPage;
import static wiki.NameSpaces.getNameSpaceNumber;
import static wiki.NameSpaces.getNameSpaceByNumber;

public final class ScribuntoLuaEngine implements MwInterface {
    private static final int MAX_EXPENSIVE_CALLS = 10;
    private static final boolean ENABLE_LUA_DEBUG_LIBRARY = true;//required to execute module table/getUnprotectedMetatable in English wiktionary
    private final Globals globals;
    private Frame currentFrame;
    private final Map<String,Frame> childFrames = new HashMap<>();
    private int expensiveFunctionCount;

	private final Map<String, Prototype> compileCache = new HashMap<>();
    private final MwInterface[] interfaces;

    private final WikiPage wp;

	private final boolean debug = false;

	public ScribuntoLuaEngine(WikiPage wp) {
		this.wp = wp;
		globals = ENABLE_LUA_DEBUG_LIBRARY ? JsePlatform.debugGlobals() : JsePlatform.standardGlobals();
        globals.finder = new LuaResourceFinder(globals.finder);

		extendGlobals(globals);

        this.interfaces = new MwInterface[] {
            new MwSite(wp),
            new MwUstring(),
            new MwTitle(wp),
            new MwText(),
            new MwUri(),
            new MwMessage(),
            new MwHtml(),
            new MwLanguage(wp),
        };

        try {
            load();
        } catch (IOException e) {
            throw new RuntimeException(e);
		}
	}

    public String invoke(String moduleName, String functionName, Frame parent, Map<String, String> params, boolean isSubst, boolean trace_calls) throws ScribuntoException {
		if (debug || trace_calls) {
			System.out.println("invoke, moduleName="+moduleName+", functionName="+functionName);
			for(Map.Entry<String,String> entry : params.entrySet()) {
				System.out.println(entry.getKey() + " => " + entry.getValue());
			}
		}
        Prototype prototype = compileCache.get(moduleName);
        if (prototype == null) {
            try {
				prototype = globals.compilePrototype(new ByteArrayInputStream(getRawWikiContent(moduleName).getBytes(StandardCharsets.UTF_8)), moduleName);
				compileCache.put(moduleName, prototype);
            } catch (LuaError | IOException e) {
                throw new ScribuntoException(e);
            }
        }
        final Frame frame = new Frame(getNameSpaceByNumber(828) + ":" + moduleName, params, parent, isSubst);
        final LuaValue function = loadFunction(functionName, prototype, frame);

        return executeFunctionChunk(function, frame);
    }


    @Override
    public String name() {
        return "mw";
    }

    protected Globals getGlobals() {
        return globals;
    }

    LuaValue loadFunction(String functionName, Prototype prototype, Frame frame) throws ScribuntoException {
        try {
            currentFrame = frame;
            LuaValue function =  new LuaClosure(prototype, globals).checkfunction().call().get(functionName);
            if (function.isnil()) {
                throw new ScribuntoException("no such function '"+functionName+"'");
            }
            return function;
        } catch (LuaError e) {
            throw new ScribuntoException(e);
        } finally {
            currentFrame = null;
        }
    }

	String executeFunctionChunk(LuaValue luaFunction, Frame frame) {
        try {
            currentFrame = frame;
            LuaValue executeFunction = globals.get("mw").get("executeFunction");

            final LuaString result = executeFunction.call(luaFunction).checkstring();
			return new String(result.m_bytes, result.m_offset, result.m_length, StandardCharsets.UTF_8);
        } finally {
            currentFrame = null;
        }
    }

    private void load() throws IOException {
        load(new MwInit());
        load(this);
        for (MwInterface iface : interfaces) {
            load(iface);
        }

        stubTitleBlacklist();
        stubExecuteModule();
        stubWikiBase();
    }

    private void stubTitleBlacklist() {
        // TODO move to separate file
        final LuaValue mw = globals.get("mw");
        LuaValue ext = mw.get("ext");
        if (ext.isnil()) {
            ext = new LuaTable();
            mw.set("ext", ext);
        }
        LuaTable blacklist = new LuaTable();
        blacklist.set("test", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue action, LuaValue title) {
                return NIL;
            }
        });
        ext.set("TitleBlacklist", blacklist);
    }


    private void stubExecuteModule() {
        // don't need module isolation
        final LuaValue mw = globals.get("mw");
        mw.set("executeModule", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                LuaFunction chunk = args.arg(1).checkfunction();
                LuaValue name     = args.arg(2);

                final LuaValue res = chunk.call();

                if (name.isnil()) {
                    return LuaValue.varargsOf(new LuaValue[]{LuaValue.TRUE, res});
                } else {
                    if (!res.istable()) {
                        return LuaValue.varargsOf(new LuaValue[]{FALSE, toLuaString(res.typename())});
                    } else {
                        return LuaValue.varargsOf(new LuaValue[]{LuaValue.TRUE, res.checktable().get(name)});
                    }
                }
            }
        });
    }

    private void stubWikiBase() {
        // fake https://www.mediawiki.org/wiki/Extension:Wikibase
		// https://github.com/wikimedia/mediawiki-extensions-Wikibase/blob/master/client/includes/DataAccess/Scribunto/mw.wikibase.lua
        final LuaValue mw = globals.get("mw");
        final LuaTable wikibase = new LuaTable();
        wikibase.set("getEntity", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return NIL;
            }
        });
        wikibase.set("getEntityObject", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return NIL;
            }
        });
        wikibase.set("getBestStatements", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue id, LuaValue property) {
                return new LuaTable();
            }
        });
        wikibase.set("sitelink", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemId, LuaValue siteId) {
                return NIL;
            }
        });
		wikibase.set("getEntityIdForCurrentPage", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return NIL;
            }
        });
        wikibase.set("getDescription", new OneArgFunction() {
            @Override public LuaValue call(LuaValue id) {
                return id;//fake
            }
        });
        wikibase.set("getLabel", new OneArgFunction() {
            @Override public LuaValue call(LuaValue id) {
                return id;//fake
            }
        });
        wikibase.set("getGlobalSiteId", new OneArgFunction() {
            @Override public LuaValue call(LuaValue id) {
                return NIL;//fake
            }
        });
        wikibase.set("isValidEntityId", new OneArgFunction() {
            @Override public LuaValue call(LuaValue id) {
                return FALSE;//fake
            }
        });
        mw.set("wikibase", wikibase);
    }

    private void load(MwInterface luaInterface) throws IOException {
        final String filename = fileNameForInterface(luaInterface);

        try (InputStream is = globals.finder.findResource(filename)) {
            if (is == null) {
                throw new FileNotFoundException("could not find '"+filename+"'. Make sure it is on the classpath.");
            }
            final LuaValue pkg = globals.load(is, "@"+filename, "bt", globals).call();
            final LuaValue setupInterface = pkg.get("setupInterface");

            if (!setupInterface.isnil()) {
                globals.set("mw_interface", luaInterface.getInterface());
                setupInterface.call(luaInterface.getSetupOptions());
            }
        }
    }

    @Override
    public LuaTable getInterface() {
        final LuaTable table = new LuaTable();
        table.set("loadPackage", loadPackage());
        table.set("loadPHPLibrary", loadPHPLibrary());
        table.set("frameExists", frameExists());
        table.set("newChildFrame", newChildFrame());
        table.set("getExpandedArgument", getExpandedArgument());
        table.set("getAllExpandedArguments", getAllExpandedArguments());
        table.set("getFrameTitle", getFrameTitle());//reference: ???
        table.set("expandTemplate", expandTemplate());
        table.set("callParserFunction", callParserFunction());
        table.set("preprocess", preprocess());
        table.set("incrementExpensiveFunctionCount", incrementExpensiveFunctionCount());
        table.set("isSubsting", isSubsting());
        table.set("addWarning", addWarning());
        return table;
    }

    private LuaValue callParserFunction() {
        return new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue frameId, LuaValue function, LuaValue args) {
                final String functionName = function.checkjstring();
				int idx = functionName.indexOf(":");
				String name = idx != -1 ? functionName.substring(0, idx) : functionName;
				String param0 = idx != -1 ? functionName.substring(idx + 1) : null;

				ParserFunction pf = ParserFunctions.get(name);
				if (debug)
					System.out.println("callParserFunction: "+name);

                if (pf != null) {
					ArrayList<String> parameters = new ArrayList<>();
					if (param0 != null)
						parameters.add(param0.trim());
					LuaTable arguments = args.checktable();
					for (int i = 1; i <= arguments.length(); i++) {
//System.out.println("arguments("+i+"):" + arguments.get(i).checkjstring());
						parameters.add(arguments.get(i).checkjstring());
					}

					Frame parent = null;
					final String ret = pf.evaluate(wp, parameters, parent);
					return ret == null ? NIL : LuaString.valueOf(ret);

                } else {
					MagicWords.MagicWord mw = MagicWords.get(name);
                    if (mw != null) {
                        final LuaTable arguments = args.checktable();
                        final String argument = arguments.get(1).checkjstring();
						final String processed = MagicWords.evaluate(mw, argument, wp);
                        return processed == null ? NIL : toLuaString(processed);
                    } else {
                        System.out.println("unknown name:" + name);
                    }
                }
				return NIL;
            }
        };
    }

    private LuaValue isSubsting() {
        return new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(getFrameById(toLuaString("current")).isSubsting());
            }
        };
    }

    private LuaValue incrementExpensiveFunctionCount() {
        return new ZeroArgFunction() {
            @Override public LuaValue call() {
                if (++expensiveFunctionCount > MAX_EXPENSIVE_CALLS) {
                    error("too many expensive function calls");
                }
                return NIL;
            }
        };
    }

    private OneArgFunction addWarning() {
        return new OneArgFunction() {
            @Override public LuaValue call(LuaValue packageName) {
                return NIL;
            }
        };
    }

    private LuaValue preprocess() {
        return new TwoArgFunction() {
            @Override public LuaValue call(LuaValue frameId, LuaValue text) {
				if (debug)
					System.out.println("preprocess: " + text);
				return toLuaString(wp.getTemplateParser().parse(text.checkjstring(), wp));
            }
        };
    }

    private LuaValue expandTemplate() {
        return new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue frameId, LuaValue title, LuaValue args) {
				if (debug)
					System.out.println("expandTemplate: " + title);
				Map<String, String> parameterMap = luaParams(args);

				if (debug)
					parameterMap.forEach((name, value) -> System.out.print(name + (value.isEmpty() ? "" : " = " + value) + ", "));

				Frame parent = null;
				return toLuaString(wp.getTemplateParser().getParsedTemplate(title.tojstring(), wp, parameterMap, parent));				
            }
        };
    }

    private LuaValue getExpandedArgument() {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue frameId, LuaValue name) {
                return getFrameById(frameId).getArgument(name.tojstring());
            }
        };
    }

    private Frame getFrameById(LuaValue luaFrameId) {
        final String frameId = luaFrameId.checkjstring();
        Frame frame = null;
        if (frameId.equals("parent")) {
            frame = currentFrame.getParent();
        } else if (frameId.equals("current")) {
            frame = currentFrame;
        } else if (childFrames.containsKey(frameId)) {
            frame = childFrames.get(frameId);
        }
        if (frame == null) {
            throw new AssertionError("No frame set: "+ luaFrameId);
        }
        return frame;
    }

    private LuaValue getFrameTitle() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return toLuaString(getFrameById(arg).getTitle());
            }
        };
    }

    private LuaValue getAllExpandedArguments() {
        return new OneArgFunction() {
            @Override public LuaValue call(LuaValue frameId) {
                return getFrameById(frameId).getAllArguments();
            }
        };
    }

    private LuaValue newChildFrame() {
        return new ThreeArgFunction() {
            /**
             * Creates a new frame
             * @return the new frame id
             */
            @Override
            public LuaValue call(LuaValue frameId, LuaValue title, LuaValue args) {
				String ns = currentFrame.getNamespace();
                final Frame childFrame =
                        currentFrame.newChild(
                            (ns.isEmpty() ? "" : ns + ":") + title.checkjstring(),
                            luaParams(args),
                            currentFrame.isSubsting());

                childFrames.put(childFrame.getFrameId(), childFrame);
                return toLuaString(childFrame.getFrameId());
            }
        };
    }

    private LuaValue frameExists() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return TRUE;
            }
        };
    }

    private OneArgFunction loadPackage() {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue packageName) {
                return loadModule(packageName.tojstring());
            }
        };
    }

    private LuaValue loadModule(String chunkName) throws LuaError {
        Prototype prototype = compileCache.get(chunkName);
        if (prototype != null) {
            return new LuaClosure(prototype, globals);
        } else {
            try (InputStream is = findPackage(chunkName)) {
				if (is != null)
	                return new LuaClosure(
		                loadAndCache(is, chunkName),
			            globals);
				else return LuaValue.FALSE;//02-09-2024: return FALSE when package not found
            } catch (ScribuntoException | IOException e) {
                throw new LuaError(e);
            }
        }
    }

    private Prototype loadAndCache(InputStream code, String chunkName) throws ScribuntoException {
        try {
            Prototype prototype = globals.compilePrototype(code, chunkName);
            compileCache.put(chunkName, prototype);

            return prototype;
        } catch (LuaError | IOException e) {
            throw new ScribuntoException(e);
        }
    }

    private OneArgFunction loadPHPLibrary() {
        return new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                return LuaValue.NIL;
            }
        };
    }

    private String fileNameForInterface(MwInterface luaInterface) {
        return luaInterface.name() + (luaInterface.name().endsWith(".lua") ? "" : ".lua");
    }

    private InputStream findPackage(String name) throws IOException {
		boolean isModule = false;
		int idx = name.indexOf(":");
		if (idx != -1) {
			String ns = name.substring(0, idx);
			Integer ns_id = getNameSpaceNumber(ns);
			if (ns_id != null && ns_id == 828)
				isModule = true;
		}

		if (isModule) {
            return findModule(name.substring(idx + 1));
        } else {
            InputStream is = globals.finder.findResource(name+".lua");
            if (is != null) {
                return is;
            } else {
                throw new IOException("package "+name+" not found");
            }
        }
    }

    private InputStream findModule(final String moduleName) throws IOException {
//System.out.println("findModule, name: " + moduleName);
        try {
            return new ByteArrayInputStream(getRawWikiContent(moduleName).getBytes(StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            // fall back to local files
            final String name = moduleName.replaceAll("[/:]", "_");
            InputStream is = globals.finder.findResource(name+".lua");
            if (is != null) {
                return is;
            } else {
				System.out.println("Warning: file not found: " + moduleName);//02-09-2024: print warning instead of throw e;
                return null;//02-09-2024: return null instead of throw e;
            }
        }
    }

    protected String getRawWikiContent(String pageName) throws FileNotFoundException {
        String content = wp.getModule(pageName);
		if (content == null) {
			throw new FileNotFoundException("could not find module \"" + pageName + "\"");
		}
		//02-09-2024: patch to handle variable arguments ... as in old versions of LUA
		int idx = 0;
		while ((idx = content.indexOf("arg", idx)) != -1) {
			if (content.charAt(idx - 1) == ' ' && content.charAt(idx + 3) == '.' && content.charAt(idx + 4) == 'n') {// arg.n
				char ch = content.charAt(idx + 5);
				if (ch == ' ' || ch == '\n') {
					content = content.substring(0, idx) + "#{...}" + content.substring(idx + 5);
				}
			} else idx += 5;
		}
		idx = 0;
		while ((idx = content.indexOf("function ", idx)) != -1) {
			char ch = content.charAt(idx - 1);
			idx += 9; //length of "function "
			if (ch == ' ' || ch == '\n') {
				int vararg = content.indexOf("(...)", idx);
				if (vararg != -1 && vararg < idx + 50 && content.substring(idx, vararg).matches("[a-zA-Z0-9: ]*")) {//evitiamo di considerare (...) lontani da 'function'				
					content = content.substring(0, vararg + 5) + " local arg = table.pack(...) " + content.substring(vararg + 5);//length of "(...)"
					idx = vararg;
//					System.out.println("-------------------");
//					System.out.println(content);
//					System.out.println("-------------------");
				}
			}
		}
		return content;
    }

    @Override
    public LuaValue getSetupOptions() {
        return new LuaTable();
    }

    private void extendGlobals(final Globals globals) {
        globals.set("setfenv", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue f, LuaValue env) {
                return f;
            }
        });
        globals.set("gefenv", new OneArgFunction() {
            public LuaValue call(LuaValue f) {
                return globals;
            }
        });
        globals.set("unpack", new unpack());

        // math.log10 got removed in 5.2
        LuaValue math = globals.get("math");
        math.set("log10", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                return valueOf(Math.log10(luaValue.checkdouble()));
            }
        });

        // math.mod was renamed to fmod
        math.set("mod", math.get("modf"));

        // table.maxn got removed in 5.2
        LuaValue table = globals.get("table");
        table.set("maxn", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                // TODO: is this correct?
                return arg.checktable().len();
            }
        });

        // table.getn got removed in 5.2
        table.set("getn", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                if (arg.isnil()) {
                    return LuaValue.error("bad argument #1 to 'getn' (table expected, got nil)");
                } else {
                    return arg.checktable().len();
                }
            }
        });

        // string.gfind was renamed to gmatch in 5.2
		LuaValue string = globals.get("string");
        string.set("gfind", string.get("gmatch"));

    }

    public static LuaString toLuaString(String string) {
        return LuaString.valueOf(string.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String,String> luaParams(LuaValue args) {
        Map<String,String> parameters = new HashMap<>();
        final LuaTable table = args.checktable();
        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs next = table.next(key);
            if ((key = next.arg1()).isnil())
                break;

            LuaValue value = next.arg(2);
            parameters.put(key.checkjstring(), value.checkjstring());
        }
        return parameters;
    }

    private static class unpack extends VarArgFunction {
        public Varargs invoke(Varargs args) {
            LuaTable t = args.checktable(1);
            switch (args.narg()) {
                case 1: return t.unpack();
                case 2: return t.unpack(args.checkint(2));
                default: return t.unpack(args.checkint(2), args.checkint(3));
            }
        }
    }

	public void resetEngine() {
		globals.setmetatable(new LuaTable());//reset globals metatable
//unload wiki modules
		final LuaTable packagee = globals.get("package").checktable();
        final LuaTable loaded = packagee.get("loaded").checktable();
        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs next = loaded.next(key);
            if ((key = next.arg1()).isnil())
                break;
            LuaValue value = next.arg(2);
			if (key.checkjstring().startsWith("Module:"))
				loaded.set(key, LuaValue.NIL);//unload module
        }		
		
		childFrames.clear();
//		compileCache.clear();
	}

    static class LuaResourceFinder implements ResourceFinder {
        private static final String[] LIBRARY_PATH = new String[] {
            "",
            "luabit",
            "lualib",
            "ustring",
        };

        private final ResourceFinder delegate;

        LuaResourceFinder(ResourceFinder delegate) {
            this.delegate = delegate;
        }

        @Override
        public InputStream findResource(String filename) {
            for (String path : LIBRARY_PATH) {
                InputStream is = delegate.findResource(path + "/" + filename);
                if (is != null) {
                    return is;
                }
            }
            return null;
        }
    }

}
