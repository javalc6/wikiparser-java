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
package wiki.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
/*
  This class contains miscellaneous utility functions
*/
final public class Utilities {

    public static Locale getLocale(String lang_code) {
        if (lang_code.startsWith("en"))
            return Locale.ENGLISH;
        else if (lang_code.startsWith("fr"))
            return Locale.FRENCH;
        else if (lang_code.startsWith("it"))
            return Locale.ITALIAN;
        else if (lang_code.startsWith("de"))
            return Locale.GERMAN;
        else {//other languages
            int split = lang_code.indexOf('_');
            if (split != -1) { // split language country
                return new Locale(lang_code.substring(0, split), lang_code.substring(split + 1));
            } else return new Locale(lang_code);
        }
    }

	public static int findValidEqualSign(String parameter) {//find position of = sign outside html tag
		if (parameter == null || parameter.isEmpty()) return -1;
		int idx = parameter.indexOf("=");
		if (idx == -1)
			return idx;
		int idx2 = parameter.indexOf("<");
		if ((idx2 == -1) || (idx < idx2))
			return idx;
		return -1;
	}

    public static boolean checkInteger(String str) {//check that string str is an unsigned integer number
        for (char c : str.trim().toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public static String flipTemplateName(String s) {//change the initial letter from upper to lower or viceversa
        if (s.isEmpty()) return s;
		if (Character.isLowerCase(s.charAt(0)))
			return s.substring(0, 1).toUpperCase() + s.substring(1);
		else return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

	public static String normaliseTitle(String value, boolean firstCharacterAsUpperCase) {
        int len = value.length();
        StringBuilder sb = new StringBuilder(len);
        boolean whiteSpace = true;
        boolean first = firstCharacterAsUpperCase;
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || (c == '_')) {
                if (!whiteSpace) {
                    whiteSpace = true;
                    sb.append(' ');
                }
            } else {
                if (first) {
                    c = Character.toUpperCase(c);
                    first = false;
                }
                sb.append(c);
                whiteSpace = false;
            }
        }
        return sb.toString().trim();
    }


    private static SimpleDateFormat formatter = null;
	public static String dateFormatter(Date date, String pattern) {//note: date is nullable
		if (date == null) {
			date = new Date(System.currentTimeMillis());
		}
        if (formatter == null) {
			formatter = new SimpleDateFormat();
			TimeZone utc = TimeZone.getTimeZone("GMT+00");
			formatter.setTimeZone(utc);
        }
		formatter.applyPattern(pattern);
		return formatter.format(date);
	}

	private static void delete_comments(StringBuilder text) { // delete html comments <!-- -->
		int comment = text.indexOf("<!--");// find html comments <!-- -->
		while (comment != -1) {
			int eoc = text.indexOf("-->", comment + 3);
			if (eoc != -1) {
				text.delete(comment, eoc + 3);
				if ((comment > 0) && (text.charAt(comment - 1)  == '\n') && (comment < text.length()) && (text.charAt(comment)  == '\n')) {//rimuove l'eventuale riga vuota in caso di eliminazione del commento
					text.deleteCharAt(comment);
				}
				comment = text.indexOf("<!--");
			} else {
				text.setLength(comment);
				break;
			}
		}
	}

/*
This method processes include tags according to https://en.wikipedia.org/wiki/Help:Template#Noinclude,_includeonly,_and_onlyinclude
Note:
- issue with template w in english wiktionary
  [[wikipedia:{{<includeonly>safesubst:</includeonly>#if:{{{lang|}}}|{{{lang}}}:}}{{{1|}}}|{{<includeonly>safesubst:</includeonly>#if:{{{2|}}}|{{{2}}}|{{{1|}}}}}]]<noinclude>{{documentation}}</noinclude>
- unable to handle border cases, like template "templates" in english wiktionary:
<no<includeonly>include>[[Category:{{ucfirst:{{{1|}}} templates}}|{{PAGENAME}}]]</no</includeonly>include><noinclude>{{documentation}}</noinclude>
*/
	public static String process_include(String input, boolean delete_comments) {
		StringBuilder sb = new StringBuilder(input);
		deleteAll(sb, "<includeonly>");
		deleteAll(sb, "</includeonly>");
		deleteInsideAB(sb, "<noinclude>", "</noinclude>");
		int idx = sb.indexOf("<onlyinclude>");
		if (idx != -1) {
			int idx2 = sb.indexOf("</onlyinclude>", idx);
			if (idx2 != -1)
				return sb.substring(idx + "<onlyinclude>".length(), idx2);
		}
		idx = sb.indexOf("<noinclude>");
		if (idx != -1) {//orphan <noinclude>, it happens if there is no end tag </noinclude>
			sb.delete(idx, sb.length());//delete everything from <noinclude>
		}
		if (delete_comments)
			delete_comments(sb);
		return sb.toString();
	}

    public static void replaceAll(StringBuilder sb, String what) {
        int idx = 0;
        while (idx < sb.length()) {
            idx = sb.indexOf(what, idx);
            if (idx != -1)	{
                sb.replace(idx, idx+what.length(), "");
                "".length();
            } else break;
        }
    }

    public static void replaceAll(StringBuilder sb, String what, String replacement) {
        int idx = 0;
        while (idx < sb.length()) {
            idx = sb.indexOf(what, idx);
            if (idx != -1)	{
                sb.replace(idx, idx+what.length(), replacement);
                idx += replacement.length();
            } else break;
        }
    }

    public static void deleteAll(StringBuilder sb, String what) {
        int idx = 0;
		int what_length = what.length();
        while ((idx = sb.indexOf(what, idx)) != -1) {
			sb.delete(idx, idx + what_length);
        }
    }

    private static void deleteInsideAB(StringBuilder sb, String a, String b) {
        int idx = 0;
		int b_length = b.length();
        while ((idx = sb.indexOf(a, idx)) != -1) {
			int idxb = sb.indexOf(b, idx);
			if (idxb != -1)
				sb.delete(idx, idxb + b_length);
			else break;
        }
    }

    public static String encodeUrl(String s) {
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
			int ch = s.charAt(i);
			if ('A' <= ch && ch <= 'Z') { // 'A'..'Z'
				sb.append((char) ch);
			} else if ('a' <= ch && ch <= 'z') { // 'a'..'z'
				sb.append((char) ch);
			} else if ('0' <= ch && ch <= '9') { // '0'..'9'
				sb.append((char) ch);
			} else if (ch == ' ') { // space
				sb.append('_'); 
			} else if ("!$()*,-./:;@_".indexOf(ch) != -1) {
				sb.append((char) ch);
			} else if (ch <= 0x007F) { // other ASCII
				toHex(ch, sb);
			} else if (ch <= 0x07FF) { // 0x007F .. 0x07FF
				toHex(0xc0 | (ch >> 6), sb);
				toHex(0x80 | (ch & 0x3F), sb);
			} else { // 0x800 .. 0xFFFF
				toHex(0xe0 | (ch >> 12), sb);
				toHex(0x80 | ((ch >> 6) & 0x3F), sb);
				toHex(0x80 | (ch & 0x3F), sb);
			}
        }
		return sb.toString();
    }

	private static void toHex(int n, StringBuilder sb) {
		sb.append("%");
		if (n < 0x10)
			sb.append("0");
		sb.append(Integer.toHexString(n).toUpperCase());
	}

}
