/*
License Information, 2024 Livio (javalc6)

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
package wiki;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import wiki.TemplateParser;
import static wiki.tools.Utilities.process_include;
import wiki.tools.WikiPage;

import static wiki.tools.Utilities.getLocale;

import info.bliki.extensions.scribunto.ScribuntoException;
import info.bliki.extensions.scribunto.engine.lua.ScribuntoLuaEngine;
/* 
TestSuite for TemplateParser to perform automatic tests of wiki parser only (html formatter is not included)

compile: javac -encoding UTF-8 -cp .;lib\luaj-jse-3.0.2p.jar wiki\TestSuite.java

run: java -cp .;lib\luaj-jse-3.0.2p.jar wiki.TestSuite [test number from 0 to 4] [template to test]

meaning of test number:
0: miscellaneous tests to check features
1: debug test
2: test template expansion, in this case specify also the template to test in the command line, note: requires files templates.dat and modules.dat
3: smoke test, note: requires files wiki.dat, templates.dat and modules.dat
4: parser robustness test

template to test: this parameter is required for test number 2
*/

final public class TestSuite {
	final static HashMap<String, String> name2template = new HashMap<>();
	final static HashMap<String, String> name2module = new HashMap<>();

	public static void main(String[] args) throws ParseException {
		int test_number = 0;
		if (args.length > 0) {
			try {
				test_number = Integer.parseInt(args[0]);
			} catch (Exception ex) {
				System.out.println("usage: java -cp .;lib\\luaj-jse-3.0.2p.jar wiki.TestSuite [test number from 0 to 4] [template to test]");
				System.out.println("0: miscellaneous tests to check features");
				System.out.println("1: debug test");
				System.out.println("2: test template expansion, in this case specify also the template to test in the command line");
				System.out.println("3: smoke test");
				System.out.println("4: parser robustness test");
				System.exit(1);
			}
			if (test_number > 4) {
				System.out.println("Incorrect test number, shall be from 0 to 4, assuming 0");
				test_number = 0;
			} else if (test_number == 2 && args.length < 2) {
				System.out.println("Please write the template to test in the command line, e.g. \"{{der|en|fr|Marne}}\"");
				System.exit(1);
			}
		}
		TestSuite ts = new TestSuite();
		TemplateParser tp = new TemplateParser();
		switch (test_number) {
			case 0:  ts.do_parser_eval_tests(tp); break;
			case 1:  ts.do_debug_test(tp); break;
			case 2:  ts.do_template_test(tp, args[1]); break;
			case 3:  ts.do_smoke_test(tp); break;//NOTE: this test requires that failsafe_parser is set to false in TemplateParser.java
			case 4:  ts.do_mixed_parser_tests(tp); break;
		}
	}

	private void do_parser_eval_tests(TemplateParser tp) throws ParseException {//automatic tests of parser 
		putItem(name2template, "asitis", "asitis");
		putItem(name2template, "one", "template_one");
		putItem(name2template, "loop", "{{loop2}}");
		putItem(name2template, "Loop2", "{{loop}}");
		putItem(name2template, "echo", "{{{1|default}}}");
		putItem(name2template, "compose3", "{{{1}}}{{{2}}}{{{3}}}");
		putItem(name2template, "TEx3", "{{{1}}}{{{2}}}{{{3}}} ({{{x}}})");
		putItem(name2template, "nested", "{{echo|{{echo|{{echo|{{{p}}}}}{{{q}}}}}{{{r}}}}}");
		putItem(name2template, "checkparam", "{{#if:{{{1|}}}|Text in param|No text in param}}");
		putItem(name2template, "checknamedparam", "{{#if:{{{lang|}}}|check deprecated lang param usage|no deprecated lang param usage}}");
		putItem(name2template, "coord/dms2dec", "{{#expr:{{#switch:{{{1}}}|N|E=1|S|O|W=-1}}*({{{2|0}}}+({{{3|0}}}+{{{4|0}}}/60)/60) round {{{precdec|{{#if:{{{4|}}}|5|{{#if:{{{3|}}}|3|0}}}}}}}}}");

		putItem(name2module, "testlua", "local export={}\nfunction export.osdate(frame)\nreturn os.date(\"!*t\", 906000490).month\nend\nreturn export");
		putItem(name2module, "testmodule", "local export={}\nfunction export.echo(frame)\nreturn frame.args[\"text\"]\nend\nreturn export");
		putItem(name2module, "domath", "local export={}\nlocal f=load\"return math.sqrt(3^2+4^2)\"\nfunction export.pitagora(frame)\nreturn f()\nend\nreturn export");

		HashMap<String, String> name2content = new HashMap<>();
		putItem(name2content, "textbook", "just a test for #ifexist");

		WikiPage wp = new WikiPage("textbook",  new SimpleDateFormat("dd-MM-yyyy hh:mm").parse("01-01-2020 15:30"),
			getLocale("en"), tp, name2template, name2module, false, name2content, true);

		System.out.println();
		System.out.print("Running tests...");
		System.out.println(tp.parse("{{CURRENTTIME}} {{CURRENTDAY}} {{CURRENTMONTHNAME}} {{CURRENTYEAR}}", wp));
		System.out.println(tp.parse("{{REVISIONTIMESTAMP}}", wp));
		testEvaluate(tp, "{{REVISIONTIMESTAMP}}", wp, "20200101143000");
		testEvaluate(tp, "text{{PAGENAME:book}}", wp, "textbook");
		testEvaluate(tp, "{{PAGENAMEE:book}}", wp, "book");
		testEvaluate(tp, "{{NAMESPACE:template:book}}", wp, "Template");
		testEvaluate(tp, "{{BASEPAGENAME:Help:Title/Foo/Bar}}", wp, "Title/Foo");
		testEvaluate(tp, "{{FULLPAGENAME:Help:Title/Foo/Bar}}", wp, "Help:Title/Foo/Bar");
		testEvaluate(tp, "{{SUBPAGENAME:Help:Title/Foo/Bar}}", wp, "Bar");
		testEvaluate(tp, "{{ROOTPAGENAME:Help:Title/Foo/Bar}}", wp, "Title");
		testEvaluate(tp, "{{!}}", wp, "|");
		testEvaluate(tp, "{{=}}", wp, "=");
		testEvaluate(tp, "{{{1|prova}}}", wp, "prova");
		testEvaluate(tp, "{{anchorencode:x y z á é}}", wp, "x_y_z_á_é");
		testEvaluate(tp, "{{formatnum:987,654,321.654321| R}}", wp, "9.87654321654321E8");
		testEvaluate(tp, "{{formatnum:987654321.654}}", wp, "987,654,321.654");
		testEvaluate(tp, "{{lc:blaBLA}}", wp, "blabla");
		testEvaluate(tp, "{{lcfirst:BLABLA}}", wp, "bLABLA");
		testEvaluate(tp, "{{uc:blaBLA}}", wp, "BLABLA");
		testEvaluate(tp, "{{ucfirst:blaBLA}}", wp, "BlaBLA");
		testEvaluate(tp, "{{ns:10}}", wp, "Template");
		testEvaluate(tp, "{{ns:T}}", wp, "Template");
		testEvaluate(tp, "{{ns:temPlate}}", wp, "Template");
		testEvaluate(tp, "{{nse:help talk}}", wp, "Help_talk");
		testEvaluate(tp, "{{padleft:| 1|xyz}}", wp, "x");
		testEvaluate(tp, "{{padleft:xyz| 5|_}}", wp, "__xyz");
		testEvaluate(tp, "{{padright:xyz|5|_}}", wp, "xyz__");
		testEvaluate(tp, "{{padright:xyz|5 }}", wp, "xyz00");
		testEvaluate(tp, "{{plural:1|is |are}}", wp, "is");
		testEvaluate(tp, "{{plural:3|is|are }}", wp, "are");
		testEvaluate(tp, "{{localurl:MediaWiki}}", wp, "/wiki/MediaWiki");
//		testEvaluate(tp, "{{localurl:MediaWiki|printable=yes}}", wp, "/w/index.php?title=MediaWiki&printable=yes");
		testEvaluate(tp, "{{fullurl:Category:Top level}}", wp, "//en.wiktionary.org/wiki/Category:Top_level");
		testEvaluate(tp, "{{urlencode:x:y/z~w}}", wp, "x%3Ay%2Fz%7Ew");
		testEvaluate(tp, "{{#formatdate:dec 25,2009| dmy}}", wp, "dec 25,2009");
		testEvaluate(tp, "{{#ifeq: foo | bar | equal | not equal}}", wp, "not equal");
		testEvaluate(tp, "{{#ifeq: 1233.00 | 1233 |equal | not}}", wp, "equal");
//		testEvaluate(tp, "{{#time: Y-m-d }}", wp, "2024-01-01");
		testEvaluate(tp, "{{#time:d F Y|1988-02-28 | nl}}", wp, "28 februari 1988");
		testEvaluate(tp, "{{#time: r|Oct 26, 1981}}", wp, "Mon, 26 Oct 1981 00:00:00 +0000");
		testEvaluate(tp, "{{#time: r|26 Oct 1981}}", wp, "Mon, 26 Oct 1981 00:00:00 +0000");
		testEvaluate(tp, "{{#time: r|Oct. 26, 1981}}", wp, "Mon, 26 Oct 1981 00:00:00 +0000");
//		testEvaluate(tp, "{{#time: r|Tue October 26, 1981}}", wp, "Tue, 27 Oct 1981 00:00:00 +0000");
		testEvaluate(tp, "{{#time: r|26. Oct 81}}", wp, "Mon, 26 Oct 1981 00:00:00 +0000");
		testEvaluate(tp, "{{#time: r|26 Oct 1981, 12:49:16}}", wp, "Mon, 26 Oct 1981 12:49:16 +0000");
		testEvaluate(tp, "{{#time: r| Oct 26th 1981 }}", wp, "Mon, 26 Oct 1981 00:00:00 +0000");
		testEvaluate(tp, "{{#time: r|October 26 1981}}", wp, "Mon, 26 Oct 1981 00:00:00 +0000");
		testEvaluate(tp, "{{#time: r|26.10.1981}}", wp, "Mon, 26 Oct 1981 00:00:00 +0000");
		testEvaluate(tp, "{{#time: r|2000 December 20}}", wp, "<strong class=\"error\">textbook:2000 December 20</strong>");
//		testEvaluate(tp, "{{#time: r|October 26}}", wp, "Mon, 26 Oct 2023 00:00:00 +0000");	
		testEvaluate(tp, "{{#iferror: {{#time: Y-m-d }} | error | correct }}", wp, "correct");
		testEvaluate(tp, "{{#iferror: {{#time: r|2000 December 20}} | error | correct }}", wp, "error");
		testEvaluate(tp, "{{#titleparts: Talk:Foo/bar/baz/quok | 2 | 2 }}", wp, "bar/baz");
		testEvaluate(tp, "{{#titleparts: Talk:Foo/bar/baz/quok | -1 }}", wp, "Talk:Foo/bar/baz");
//		testEvaluate(tp, "{{#ifexist: textbook | exists | doesn't exist }}", wp, "exists"); 30-09-2024: disabled because getContent() in WikiPage returns always null
		testEvaluate(tp, "{{#tag:img | image | src=images/a.png }}", wp, "<img src=\"images/a.png\">image</img>");
		testEvaluate(tp, "{{#switch: baz | foo = Foo | baz = Baz | Bar }}", wp, "Baz");
		testEvaluate(tp, "{{#switch: foo | foo| baz = Baz | Bar }}", wp, "Baz");
		testEvaluate(tp, "{{#switch: boh | foo = Foo | #default = Bar | baz = Baz }}", wp, "Bar");
		testEvaluate(tp, "{{#invoke:testmodule|echo |text=ciao}}", wp, "ciao");
		testEvaluate(tp, "{{safesubst:#invoke:domath|pitagora }}", wp, "5");
		testEvaluate(tp, "{{#expr:2*sin(pi/6)}}", wp, "0.9999999999999999");
		testEvaluate(tp, "{{#expr:{{{sub|0}}}+1}}", wp, "1");
		testEvaluate(tp, "{{#ifexpr: 1 > 0 | yes }}", wp, "yes");
		testEvaluate(tp, "{{#ifexpr: 2 > 0 and 1 = 0 | yes | no}}", wp, "no");
		testEvaluate(tp, "{{#iferror: {{#expr: 1 + 2 }} | error | correct }}", wp, "correct");
		testEvaluate(tp, "{{#iferror: {{#expr: 1 + X }} | error | correct }}", wp, "error");
		testEvaluate(tp, "{{asitis}}", wp, "asitis");
		testEvaluate(tp, "{{loop}}", wp, "Template loop detected: [[Template:loop]]");
		testEvaluate(tp, "{{echo|alfa}}", wp, "alfa");
		testEvaluate(tp, "{{echo| alfa }}", wp, " alfa ");
		testEvaluate(tp, "{{echo}}", wp, "default");
		testEvaluate(tp, "{{echo|a{{=}}b}}", wp, "a=b");
		testEvaluate(tp, "{{echo|a<nowiki>=</nowiki>b}}", wp, "a=b");
		testEvaluate(tp, "{{echo|a<!--=-->b}}", wp, "ab");
		testEvaluate(tp, "{{echo|[[ab|cd]]}}", wp, "[[ab|cd]]");
		testEvaluate(tp, "a{{ compose3 | alfa | beta | gamma }}b", wp, "a alfa  beta  gamma b");
//		testEvaluate(tp, "{{ TEx3 | A | B | C | x = C }}", wp, "A B C (C)");
		testEvaluate(tp, "{{subst:temPLAte:echo|alfa}}", wp, "alfa");
		testEvaluate(tp, "{{nested|r={{echo|c}}|q={{echo|b}}|p={{subst:temPLAte:echo|a}}}}", wp, "abc");
		testEvaluate(tp, "{{checkparam}}", wp, "No text in param");
		testEvaluate(tp, "{{checknamedparam}}", wp, "no deprecated lang param usage");
		testEvaluate(tp, "{{o{{#if: string | ne | two}}}}", wp, "template_one");
		testEvaluate(tp, "{{int:friday}}", wp, "Friday");//this test requires file en.json stored in folder wiki
		testEvaluate(tp, "{{#language}}", wp, "English");
		testEvaluate(tp, "{{#language:it}}", wp, "Italian");
		testEvaluate(tp, "{{#language:it|fr}}", wp, "italien");
		testEvaluate(tp, "{{#invoke:testlua|osdate}}", wp, "9");

		System.out.println("End of tests");
    }

	private void do_debug_test(TemplateParser tp) throws ParseException {//debug test for troubleshooting specific problem
		WikiPage wp = new WikiPage("textbook",  new SimpleDateFormat("dd-MM-yyyy hh:mm").parse("01-01-2020 15:30"),
				getLocale("en"), tp, name2template, name2module, true, null, true);
/*example of a standalone test
		putItem(name2template, "accent", "here we are");
		putItem(name2template, "a", "#REDIRECT [[Template:accent]]");
		String result = tp.parse("{{a|UK}}", wp);
*/
//example of test with templates.dat and modules.dat
		readfile(name2template, "templates.dat", false);
		readfile(name2module, "modules.dat", false);
		String result = tp.parse("{{en-verb}}", wp);

		System.out.println("evaluate: " + result);
	}

	private void do_template_test(TemplateParser tp, String template) throws ParseException {//test of template expansion using files templates.dat and modules.dat
		readfile(name2template, "templates.dat", false);
		readfile(name2module, "modules.dat", false);
		System.out.println("Warning: this test uses locale en (english), in case of other languages must be changed inside do_template_test() call"); System.out.println();
		WikiPage wp = new WikiPage("textbook",  new SimpleDateFormat("dd-MM-yyyy hh:mm").parse("01-01-2020 15:30"),
				getLocale("en"), tp, name2template, name2module, true, null, true);
		String result = tp.parse(template, wp);
        System.out.println(template + "--> " + result);
	}

	private void do_smoke_test(TemplateParser tp) throws ParseException {//performs smoke tests on files wiki.dat, templates.dat and modules.dat
		readfile(name2template, "templates.dat", false);
		readfile(name2module, "modules.dat", false);
		HashMap<String, String> name2content = new HashMap<>();
		String firstline = readfile(name2content, "wiki.dat", true);
		int idx = firstline.indexOf("|");
		String language = firstline.substring(0, idx);

		System.out.println("Number of templates: " + name2template.size());
		System.out.println("Number of modules: " + name2module.size());
		System.out.println("Number of definitions: " + name2content.size());
		System.out.println("Performing smoke test");

		name2content.forEach((keyword, definition) -> {
			System.out.println("Testing: " + keyword);
			try	{
				WikiPage wp = new WikiPage(keyword,  new SimpleDateFormat("dd-MM-yyyy hh:mm").parse("01-01-2020 15:30"),
						getLocale(language), tp, name2template, name2module, false, name2content, true);
				tp.parse(definition, wp);				
			} catch (ParseException ex) {
			}
		});
	}

	private void do_mixed_parser_tests(TemplateParser tp) throws ParseException {//mixed tests of parser to check handling of misplaced characters
		WikiPage wp = new WikiPage("textbook",  new SimpleDateFormat("dd-MM-yyyy hh:mm").parse("01-01-2020 15:30"),
				getLocale("en"), tp, name2template, name2module, false, null, true);
		putItem(name2template, "echo", "{{{1|default}}}");
		System.out.println();
		System.out.print("Running tests...");
		testParser(tp, "alfa}}", wp);
		testParser(tp, "a{{lfa}}", wp);
		testParser(tp, "a{{{echo|b}}c", wp);
		testParser(tp, "a{{echo|b}}}c", wp);
		testParser(tp, "a{{{echo|b}}}c", wp);
		testParser(tp, "{{d|{{e}}}}f", wp);
		testParser(tp, "{{d|name={{e}}|3=f|g|ref=|=as}}q", wp);
		testParser(tp, "{{{e|f}}}q", wp);
		testParser(tp, "{{{e|}}}q", wp);
		testParser(tp, "{{{e|f}}q", wp);
		testParser(tp, "{{e|f}}}q", wp);
		testParser(tp, "{{{e|{{f}}}}}q", wp);
		testParser(tp, "{{{e|{{{f}}}}}}q", wp);
		testParser(tp, "{{a|{{{b}}}}}", wp);
		testParser(tp, "a{{CurRENTTIMESTAMP}}b", wp);
		testParser(tp, "{{alfa|{{b}}c}}", wp);
		testParser(tp, "{{alfa|{{{b}}}c}}", wp);
		testParser(tp, "{{anchorencode:{{{b|}}}c}}", wp);
		testParser(tp, "{{#if:{{{2|}}}|{{ordinalbox|eo|{{#expr:{{{2}}}-1}}-a|{{{2}}}-a|{{#expr:{{{2}}}+1}}-a|{{{3}}}a|{{{5}}}a|card={{{4}}}}}}}", wp);
		testParser(tp, "a{{b{{z", wp);
//		testParser(tp, "a{{b{{c{{", wp);
		testParser(tp, "a{{b|{{c}}|d{{e}}|{{{{f}}}}|g}}h", wp);
		testParser(tp, "a{{b_*_x|{{_c_}}|d{{e+g}}|{{{{f:m:n}}}}|g}}h", wp);
		testParser(tp, "a{{b{{c{{d}}e}}z", wp);
		testParser(tp, "{{d|{{e}}}}", wp);
		testParser(tp, "b}}a{{c|}}{{d|{{e|}}}}f", wp);
		testParser(tp, "{{e}}f", wp);
		testParser(tp, "{{{{e|}}", wp);//in questo caso il primo {{ va interpretato come string e non come tree!
		testParser(tp, "a{{b|{{c|x}}d|{{e}}|{{{{f|y}}}}|g}}h", wp);
		testParser(tp, "a{{d|{{e}}|{{{{f}}}}|g}}h", wp);
		System.out.println("End of tests");
	}

	private void testParser(TemplateParser tp, String wiki, WikiPage wp) {
		String root = tp.parse(wiki, wp);
		System.out.println(wiki + " --> " + root);
	}


	private void testEvaluate(TemplateParser tp, String wiki, WikiPage wp, String expected) {
		String root = tp.parse(wiki, wp);
		if (!root.equals(expected))
			System.err.println("Error: no match for " + root + " <--> " + expected);
	}

	public static void putItem(HashMap<String, String> name2page, String identifier, String item) {
		name2page.put(identifier.trim(), item);
	}

	public static String readfile(HashMap<String, String> name2page, String fn, boolean isWikiDat) {
		String firstline = null;
		try (LineNumberReader in = new LineNumberReader(new InputStreamReader(new FileInputStream(fn), StandardCharsets.UTF_8))) {
			StringBuilder definition = new StringBuilder();
			String st, identifier = "";
			String [] result;
			if (isWikiDat)
				firstline = in.readLine(); // read first line in wiki.dat
			int skiplines = 0;
			while((st = in.readLine()) != null) {
				if (skiplines > 0) {
					definition.append(st);
					skiplines--;
					if (skiplines == 0)	{
						assert !identifier.isEmpty();
						name2page.put(identifier, definition.toString());
						definition.setLength(0);
					} else definition.append("\n");
					continue;
				}
				if ((!st.contains("|"))) {
					System.out.println("Error1 Parsing in "+fn+": "+st);
					System.exit(1);
				}
				result = st.split("\\|");
				identifier =result[0].trim();
				try {
					skiplines = Integer.parseInt(result[1]);
				} catch (Exception ex) {
					System.out.println("Error2 Parsing in "+fn+": "+st);
					System.exit(1);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return firstline;//firstline is returned only in case of isWikiDat, otherwise null is returned
	}

}
