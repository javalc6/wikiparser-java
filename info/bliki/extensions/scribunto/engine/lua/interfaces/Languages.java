package info.bliki.extensions.scribunto.engine.lua.interfaces;


import static wiki.tools.Utilities.getLanguageNames;

final class Languages {

    /**
     * @param code string: The code of the language for which to get the name
     * @return string: Language name or empty
     */
    public String getName(String code) {
        return getLanguageNames().get(code);
    }
}
