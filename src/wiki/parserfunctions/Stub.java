package wiki.parserfunctions;

import java.util.ArrayList;
import wiki.tools.WikiPage;
import info.bliki.extensions.scribunto.template.Frame;

//stub for not implemented parser functions, return empty string

public final class Stub extends ParserFunction {

    public final static ParserFunction Instance = new Stub();

    @Override
	public String evaluate(WikiPage wp, ArrayList<String> parameters, Frame parent) {
        return ""; 
	}

}