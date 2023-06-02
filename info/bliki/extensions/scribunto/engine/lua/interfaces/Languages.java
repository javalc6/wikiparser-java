package info.bliki.extensions.scribunto.engine.lua.interfaces;


import java.util.HashMap;
import java.util.Map;

final class Languages {
    private final Map<String,String> codes = new HashMap<>();
    {
        codes.put("de", "Deutsch");
        codes.put("en", "English");
        codes.put("es", "Español");
        codes.put("fr", "Français");
        codes.put("it", "Italiano");
        codes.put("pt", "Português");
    }

    /**
     * @param code string: The code of the language for which to get the name
     * @return string: Language name or empty
     */
    public String getName(String code) {
        return codes.get(code);
    }
}
