/*
License Information, 2023 Livio (javalc6)

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.io.*;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import bzip2.BZip2CompressorInputStream;
import java.nio.file.Paths;
import java.nio.file.Files; 

/*
Wiktionary splitter: this standalone tool splits the xml file wiktionary downloaded from https://dumps.wikimedia.org/enwiktionary/latest/

The xml file wiktionary is splitted in the following files for further processing:
- wiki.dat: line oriented text file with definitions
- templates.dat: line oriented text file with templates
- modules.dat: line oriented text file with modules

The following files are generated for information purposes:
- index.txt: list of words extracted in wiki.dat
- excluded.txt: list of words excluded

compile: javac -encoding UTF-8 wiki\WikiSplitter.java

usage:  java wiki.WikiSplitter <filename>

Note: The value of constant FilterOtherLanguages is useful to select only the wanted language before generating wiki.dat

This software incorporates bzip2 decompressor from in https://commons.apache.org/proper/commons-compress, under Apache License version 2.0
*/
public class WikiSplitter {
	private final static boolean FilterOtherLanguages = true;//filter out sections with other languages using patterns 'thislanguage' and 'language_pattern'

	private final static Pattern thislanguage = Pattern.compile("==english=="); // detect correct language ==english (note: use lowercase)
	private final static Pattern language_pattern = Pattern.compile("==[^=]*\\p{L}[^=]*==");

	private final static Pattern keywords = Pattern.compile("[\\p{L}\\-][\\p{L}'/.\\-\\s]*");// Pattern to check that keyworks start with a Letter and contains letters, spaces, - and '
	private final static String EOL = "\r\n";

	private final static int max_word_length = 127; // max length of a word in the index (value must be lower than 128)

	private final static String template_label = "Template:";
	private final static String module_label = "Module:";

	private final TreeMap<String, String> name2module = new TreeMap<>();
	private final TreeMap<String, String> name2template = new TreeMap<>();

	public static void main(String[] args) {
		if (args.length == 1) {
			String fn = args[0];
			long t0 = System.nanoTime();

			new WikiSplitter().doSplit(fn);

			System.out.println();
			System.out.println((System.nanoTime() - t0)/1e9+" s");
		} else {
			System.out.println("Usage: java WikiSplitter <filename>");
		}
	}

    public void doSplit(String fn) {
		if (fn == null)
			fn = "enwiktionary-latest-pages-articles.xml";
		try {
			TreeMap<String, String> dict = new TreeMap<>();
			String[] language = new String[1];
			parseXMLFile(fn, language, dict);

// write templates file
			PrintWriter template_file = new PrintWriter("templates.dat", "UTF-8");
			name2template.forEach((name, template) -> {
				String[] lines = template.split(EOL);
				template_file.println(name + "|" + lines.length);
				for (String line: lines)
					template_file.println(line);
			});
			template_file.close();

// write modules file
			PrintWriter module_file = new PrintWriter("modules.dat", "UTF-8");
			name2module.forEach((name, module) -> {
				String[] lines = module.split(EOL);
				module_file.println(name + "|" + lines.length);
				for (String line: lines)
					module_file.println(line);
			});
			module_file.close();
			
			write_dat(dict, fn, language[0]);
		} catch(Exception e) {
			e.printStackTrace();
        }
    }

	private void parseXMLFile(String fn, String[] language, TreeMap<String, String> dict) throws IOException, ParserConfigurationException {
			ArrayList<String> excluded = new ArrayList<>();

			InputSource is;
			if (fn.endsWith("bz2"))	{
				InputStream fin = Files.newInputStream(Paths.get(fn));
				BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(new BufferedInputStream(fin));
				is = new InputSource(bzIn);
				is.setEncoding("UTF-8"); 
			} else {
				Reader reader = new InputStreamReader(new FileInputStream(fn), StandardCharsets.UTF_8);
				is = new InputSource(reader);
				is.setEncoding("UTF-8"); 
			}

		    try {
				SAXParserFactory.newInstance().newSAXParser().parse(is, 
					new WikiHandler(dict, excluded, language));
			} catch (SAXException e) {
				e.printStackTrace();
			}

// write excluded words file
			PrintWriter excludedf = new PrintWriter("excluded.txt", "UTF-8");
			for (String e: excluded) {
				excludedf.println(e);
			}
			excludedf.close();

			System.out.println();
			System.out.println("Number of words found: "+dict.size());
			System.out.println("Number of excluded words: "+excluded.size());
	}

	private void write_dat(TreeMap<String, String> dict, String fn, String language) throws IOException {
// write definition file
			PrintWriter output = new PrintWriter("wiki.dat", "UTF-8");
			output.println(language + "|" + fn); //header
			StringBuilder definition = new StringBuilder();
			for (Map.Entry<String, String> entry : dict.entrySet()) {
				String word = entry.getKey();
				int n_def_lines = 0;
                String[] lines = entry.getValue().split(EOL);
                for (String line : lines) {
					definition.append(line).append(EOL);
                    n_def_lines++;
                }
				if (n_def_lines != 0) {
					output.println(word+"|"+n_def_lines);
					output.print(definition);
					definition.setLength(0);n_def_lines = 0;
				} else System.out.println(word+" has been dropped");
			}
			output.close();

// write index file
			output = new PrintWriter("index.txt", "UTF-8");
			for (String word: dict.keySet()) {
				output.println(word);
			}
			output.close();
	}

	class WikiHandler extends DefaultHandler {
		boolean isTitle = false;
		boolean isText = false;
		boolean isPage = false;
		boolean isLemma = false;
		boolean isTemplate = false;
		boolean isModule = false;
		final StringBuilder buf = new StringBuilder();
		int counter;
		String title;
		final TreeMap <String, String> dict;
		final ArrayList<String> excluded;
		final String[] language;

		WikiHandler(TreeMap<String, String> dict, ArrayList<String> excluded, String[] language) {
			super();
			this.dict = dict;
			this.excluded = excluded;
			this.language = language;

			counter = 0;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException { 
			if (qName.equalsIgnoreCase("mediawiki")) {
				for (int i = 0; i < attributes.getLength(); i++) {
					if (attributes.getQName(i).equals("xml:lang")) {
						language[0] = attributes.getValue(i); 
						Locale locale = new Locale(language[0]); 
						String str = locale.getDisplayLanguage(locale);
						String language_name = str.substring(0, 1).toUpperCase(locale) + str.substring(1);//capitalize language_name
						Locale.setDefault(locale);

						System.out.println();
						System.out.println("Language: "+language[0]+" ("+language_name+")");
						break;
					}
				}
			} else if (qName.equalsIgnoreCase("page")) {
				isPage = true;
				isLemma = false;
				isTemplate = false;
				isModule = false;
			} else if (isPage) {
				if (qName.equalsIgnoreCase("title")) {
					isTitle = true;
				} else if (qName.equalsIgnoreCase("text")) {
					isText = true;
				} else if (isText && qName.equalsIgnoreCase("math")) {
					buf.append("<math>"); // echo tag
				}
			}
		}
	 
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equalsIgnoreCase("page")) {
				isPage = false;
			} else if (qName.equalsIgnoreCase("title")) {
				isTitle = false; 
				title = buf.toString().trim();
				if (title.startsWith(template_label)) {
					isTemplate = true;
				} else if (title.startsWith(module_label)) {
					isModule = true;
				} else if (title.length() <= max_word_length && keywords.matcher(title).matches()) {
					isLemma = true;
				} else {
					excluded.add(title);
				}
				buf.setLength(0);
			} else if (qName.equalsIgnoreCase("text")) {
				isText = false; 
				if (isLemma) {
					if (dict.containsKey(title)) { // keyword collision, shall never happen!
						System.out.print("Unexpected collision for: " + title);
					}
					String definition = FilterOtherLanguages ? doFilterOtherLanguages(buf) : buf.toString();
					if (!definition.isEmpty()) {
						dict.put(title, definition.replace("\n", EOL).trim());
						counter++;
						if (counter % 1000 == 0)
							System.out.print(".");
					} else excluded.add(title);

					isLemma = false;
				} else if (isModule) {
					String module_title = title.substring(module_label.length());//delete label from module
					name2module.put(module_title, buf.toString().replace("\n", EOL).trim());
					isModule = false;
				} else if (isTemplate) {
					String template_title = title.substring(template_label.length());//delete label from template
					name2template.put(template_title, buf.toString().replace("\n", EOL).trim());
					isTemplate = false;
				}
				buf.setLength(0);
			} else if (isText && qName.equalsIgnoreCase("math")) {
				buf.append("</math>");// echo tag
			}
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			if (isTitle||isText)
				for (int i=start; i<start+length; i++)
					buf.append(ch[i]);
		}	 
	}

	private String doFilterOtherLanguages(StringBuilder buf) {
		StringBuilder result = new StringBuilder();
		boolean correct_language = false;
		SegmentReader sr = new SegmentReader(buf);
		String segment;
		while ((segment = sr.getSegment(false)) != null) {
			String low_segment = segment.toLowerCase();
			if (thislanguage.matcher(low_segment).matches())
				correct_language = true;
			else if (language_pattern.matcher(low_segment).matches())
				correct_language = false;
			if (correct_language) {
				result.append(segment);
				result.append("\n");
			}
		}
		return result.toString();
	}

	class SegmentReader {
		private final StringBuilder buffer;
		private int pointer = 0;
		private int math; //helper for <math>...</math> blocks, defined here to improve performance

		public SegmentReader(StringBuilder buf) {
			buffer = buf;
		}

		private int findMatchBrace(int start) {//start on first {{
			int lvl = 0;
			int end = buffer.length();
			for (int i = start; i < end - 1; i++) {
				if (buffer.charAt(i) == '{' && buffer.charAt(i + 1) == '{') {//{{
					lvl++; i++;
				} else if (buffer.charAt(i) == '}' && buffer.charAt(i + 1) == '}') {//}}
					lvl--; i++;
					if (lvl == 0)
						return i + 1;//match found, return position after }}
				}
			}
			return -1;//no match found
		}

		private final int[] opening_pos = new int[256];
		private int findMatchingBraces(int start, int next_nl) {//returns matching braces with condition that next_nl < match[1] is true, otherwise it returns null
			while (start != -1) {
				int lvl = 0; int close_lvl = -1;//undef
				int close_pos = -1;//undef
				int end = buffer.length();
				for (int i = start; i < end - 1; i++) {
	//skip {{ or }} if found inside <math>...</math> block
					int emath = -1;
					if ((math != -1) && ((emath = buffer.indexOf("</math>", math)) != -1) && (math < i) && (i < emath)) {
						i = emath;
						math = buffer.indexOf("<math", emath); // nota: a volte il tag math ha dei parametri, es: <math style="blabla">
						continue;//skip <math>...</math> block
					}			
					if ((math != -1) && (emath == -1))
						break;

					if (buffer.charAt(i) == '{' && buffer.charAt(i + 1) == '{') {//{{
						opening_pos[lvl] = i;
						lvl++; i++;
					} else if ((lvl > 0) && buffer.charAt(i) == '}' && buffer.charAt(i + 1) == '}') {//}}
						lvl--; i++;
						close_lvl = lvl;
						close_pos = i + 1;
						if (lvl == 0)
							break;
					}
				}
				if (close_lvl == -1)
					return -1;//no match found
				if ((opening_pos[close_lvl] < next_nl) && (next_nl < close_pos))//found!
					return close_pos;
				if (close_lvl > 0)
					return -1;//no match found				
				start = buffer.indexOf("{{", close_pos);//move ahead of current match
			}
			return -1;//no match found
		}

		public String getSegment(boolean debug) {//nota: nella stringa ritornata da getSegment() ci possono essere contenere dei caratteri '\n' (senza alcun \r)
			if (pointer == -1)
				return null;
			if (pointer >= buffer.length()) {//pointer not valid any more
				pointer = -1;
				return null;
			}
			while (buffer.charAt(pointer) == '\n') {//skip useless newlines
				pointer++;
				if (pointer >= buffer.length()) {
					pointer = -1;
					return null;
				}
			}
			int next_nl;
			String result;
			if ((next_nl = buffer.indexOf("\n", pointer)) != -1) {
				int start = buffer.indexOf("{{", pointer);
				if (start != -1) {
					math = buffer.indexOf("<math", start);
					int pp = findMatchingBraces(start, next_nl);
					if (pp != -1) {
						result = buffer.substring(pointer, pp);// }}
						pointer = pp;// }}
						if (debug)
							System.out.println("getSegment.4");
						return result;
					}
				}
				math = buffer.indexOf("<math", pointer);
				if ((math != -1) && (next_nl > math)) {
					int emath = buffer.indexOf("</math>", math);
					if ((emath != -1) && (next_nl < emath)) {
						next_nl = buffer.indexOf("\n", emath + 7); // 7: "</math>".length()
					}
				}
				if (next_nl != -1) {
					result = buffer.substring(pointer, next_nl);
					pointer = next_nl + 1;//skip \n
					if (debug)
						System.out.println("getSegment.2");
					return result;
				}
			}
			result = buffer.substring(pointer);
			pointer = -1;
			if (debug)
				System.out.println("getSegment.3");
			return result;
		}
	}
}