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
/* The class WikiFormatter can be used to format wiki content by handling wikicode not related to templates and modules.
   Pre-requisite: templates and modules shall be processed before calling this class.

*/
package wiki.tools;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import static wiki.tools.Utilities.delete_comments;
import static wiki.tools.Utilities.getLanguageNames;

final public class WikiFormatter {
	final static String category_label = "Category:";
	final static List<String> not_allowed_media = Arrays.asList(new String[]{"file", "image", "audio"});
	final static HashMap<String, String> code2language = getLanguageNames();

    public static String formatWikiText(StringBuilder lemma, StringBuilder wikitext, String linkBaseURL, String language) {
		delete_comments(wikitext);
		StringBuilder result = new StringBuilder(wikitext.length());
		StringBuilder buf = new StringBuilder(128);
		StringBuilder old_list = new StringBuilder();
		boolean open_p = false;
		int table_open = 0;//zztable
		boolean tr_open = false;//zztable
		boolean td_open = false;//zztable
		boolean th_open = false;//zztable

		boolean open_tag_div = false;

		int idx = 0;
		int blen = wikitext.length(); boolean check_math = true;
		while (idx < blen) {
			int eol = wikitext.indexOf("\n", idx);
			if (check_math)	{
				int p1 = wikitext.indexOf("<math", idx);
				if (p1 != -1) {
					int p2 = wikitext.indexOf("</math", p1);
					while ((eol != -1) && (p2 != -1) && (p1 < eol)) {
						if (p2 > eol) {
							wikitext.replace(eol, eol + 1, " ");
							eol = wikitext.indexOf("\n", eol + 1);
						} else {
							p1 = wikitext.indexOf("<math", p2 + 1);
							if ((p1 == -1) || (p1 > eol))
								break;
							p2 = wikitext.indexOf("</math", p1);
						}
					}
				} else check_math = false;
			}

			String st;
			if (eol == -1) {
				st = wikitext.substring(idx).trim();
				idx = blen;
			} else {
				st = wikitext.substring(idx, eol).trim();
				idx = eol + 1;
			}
			if (!st.isEmpty()) {
				char first_char = st.charAt(0);
				if (first_char == '#' || first_char == ':' || first_char == ';' || first_char == '*') { // "#" or ":" or ";" or "*"
					if (buf.length() > 0) {
// Add bold and italic formatting
						applyBoldItalic(buf, 0);
						if (open_p)	{
							buf.append("</p>");
							open_p = false;
						}
						applyRules(buf, result, linkBaseURL, language);
						buf.setLength(0);
					}
					int j = 0;
					int length = Math.min(st.length(), old_list.length());
					for (; j < length; j++) {
						char ch = st.charAt(j);  char old = old_list.charAt(j);
						if ((ch != old) && (ch != ';' || (st.indexOf(':', j) == -1) || (old != ':')))
							if (((old != '+') || ((ch != ';') && (ch != ':'))))
								break;
					}
					int common = j;
					for (j = old_list.length() - 1; j >= common; j--) {
						char ch = old_list.charAt(j);
						if (ch == '#') {
							buf.append("</li></ol>");
						} else if (ch == '*') {
							buf.append("</li></ul>");
						} else if ((ch == ':') || (ch == '+')) {
							buf.append("</dd></dl>");
						} else if (ch == ';') {
							buf.append("</dt></dl>");
						}
					}
					old_list.setLength(common);
					boolean a = true;
					for (j = common; j < st.length(); j++) {
						char ch = st.charAt(j); 
						if (ch == '#') {
							buf.append("<ol><li>");
						} else if (ch == '*') {
							buf.append("<ul><li>");
						} else if (ch == ':') {
							buf.append("<dl><dd>");
						} else if (ch == ';') {
							buf.append("<dl><dt>");
							int idy = st.indexOf(':', j);
							if (idy != -1) {
								buf.append(st, j + 1, idy);
								buf.append("</dt><dd>"); ch = '+'; //+ has special meaning, it includes ; followed by :
								buf.append(st, idy + 1, st.length());
								j = st.length();
							}
						} else break;
						old_list.append(ch);
						a = false;
					}
					if (a) {
						char ch = st.charAt(j - 1); 
						if (ch == '#') {
							buf.append("</li><li>");
						} else if (ch == '*')
							buf.append("</li><li>");
						else if (ch == ':')
							buf.append("</dd><dd>");
						else if (ch == ';') {
							int idy = st.indexOf(':', j);
							if (idy != -1) {
								buf.append("</dd><dt>");
								buf.append(st, j, idy);
								buf.append("</dt><dd>");
								j = idy + 1;
							} else buf.append("</dt><dt>");
						}
					}
					if (j < st.length()) {
						if ((j < st.length() - 1) && (st.charAt(j) == '{') && (st.charAt(j + 1) == '|')) {// {| begin table
							buf.append("<table");//fill start of table
							if (st.length() > j + 7) {
								if (st.length() > 4) {
									buf.append(" ").append(st.substring(j + 2));
								}
							}
							buf.append(">");
							table_open++;
						} else buf.append(st, j, st.length());
					}

				} else if (first_char == '|') { //"|" Table handling
					int ptr = 1;//skip first char
					if (st.startsWith("|+")) {//|+ Table caption
						ptr++;
						if (!tr_open) {
							buf.append("<caption>"); buf.append(st, ptr, st.length());
							buf.append("</caption>");
						}
					} else if (st.startsWith("|-")) {//|- Table row
						ptr++;
						if (!tr_open) {
							buf.append("<tr>");
							tr_open = true;
						} else {
							if (td_open) {
								buf.append("</td>");
								td_open = false;
							} else if (th_open) {
								buf.append("</th>");
								th_open = false;
							}
							buf.append("</tr><tr>");
						}
					} else if (st.startsWith("|}")) {//|} Table end
						if (td_open) {
							buf.append("</td>");
							td_open = false;
						}
						if (tr_open) {
							buf.append("</tr>");
							tr_open = false;
						}
						if (table_open > 0) {
							buf.append("</table>");//fill end of table
							table_open--;
                        }
						if (st.length() > 2) {
							String trailer = st.substring(2).trim();
							if (!trailer.isEmpty())
								buf.append(trailer);
						}
					} else {//Table data cell
						if ((ptr < st.length()) && (st.charAt(ptr) == '|')) {
							ptr++;//ignore extra |
						}
						if (!tr_open) {
							buf.append("<tr>");
							tr_open = true;
						}
						if (td_open) {
							buf.append("</td><td");
						} else {
							buf.append("<td");
							td_open = true;
						}
						int double_bar;
						while ((double_bar = st.indexOf("||", ptr)) != -1) {
							int bar = st.indexOf("|", ptr);
							if ((bar != -1) && (bar < double_bar)) {
								int sq = st.indexOf("[", ptr);
								if ((sq == -1) || (sq > bar)) {
									buf.append(" ");
									buf.append(st.substring(ptr, bar));
									ptr = bar + 1;
								}
							}
							buf.append(">");
							buf.append(st, ptr, double_bar); ptr = double_bar + 2;
							buf.append("</td><td"); 
						}
						int bar = st.indexOf("|", ptr);
						if (bar != -1) {
							int sq = st.indexOf("[", ptr);
							if ((sq == -1) || (sq > bar)) {
								buf.append(" ");
								buf.append(st.substring(ptr, bar));
								ptr = bar + 1;
							}
						}
						buf.append(">");
						buf.append(st, ptr, st.length());
					}				
				} else if (first_char == '!') { //! Table header cell
					int ptr = 1;//skip first char
					if ((ptr < st.length()) && (st.charAt(ptr) == '!')) {//ignore extra !
						ptr++;
					}
					if (!tr_open) {
						buf.append("<tr>");
						tr_open = true;
					}
					if (th_open) {
						buf.append("</th><th");
					} else {
						buf.append("<th");
						th_open = true;
					}
					int double_bar;
					while ((double_bar = st.indexOf("!!", ptr)) != -1) {
						int bar = st.indexOf("|", ptr);
						if ((bar != -1) && (bar < double_bar)) {
							int sq = st.indexOf("[", ptr);
							if ((sq == -1) || (sq > bar)) {
								buf.append(" ");
								buf.append(st, ptr, bar); ptr = bar + 1;
							}
						}
						buf.append(">");
						buf.append(st, ptr, double_bar); ptr = double_bar + 2;
						buf.append("</th><th"); 
					}
					int bar = st.indexOf("|", ptr);
					if (bar != -1) {
						int sq = st.indexOf("[", ptr);
						if ((sq == -1) || (sq > bar)) {
							buf.append(" ");
							buf.append(st.substring(ptr, bar));
							ptr = bar + 1;
						}
					}
					buf.append(">");
					buf.append(st, ptr, st.length());
				} else if (st.startsWith("</")) {//in case of closing tag, rendering shall be transparent
					if (open_p) {
						buf.append("</p>");
						open_p = false;
					}					
					buf.append(st);
					if (st.startsWith("</table")) {
						table_open--;
                    }
				} else if (first_char == '/' || first_char == '>') { 
					buf.append(st);
				} else { 
					if (buf.length() > 0) {// Add bold and italic formatting
						applyBoldItalic(buf, 0);
						if ((first_char == '=') && open_p)	{
							buf.append("</p>");
							open_p = false;
						}
						applyRules(buf, result, linkBaseURL, language);
						buf.setLength(0);
					}
					for (int j = old_list.length() - 1; j >= 0; j--) {
						char ch = old_list.charAt(j);
						if (ch == '#') {
							result.append("</li></ol>");
						} else if (ch == '*') {
							result.append("</li></ul>");
						} else if ((ch == ':') || (ch == '+')) {
							result.append("</dd></dl>");
						} else if (ch == ';') {
							result.append("</dt></dl>");
						}
					}
					old_list.setLength(0);
					if (st.startsWith("{|")) {//{| begin table
						buf.append("<table");
						if (st.length() > 7) {
                            st.length();
                            buf.append(" ").append(st.substring(2));
                        }
						buf.append(">");
						table_open++;
					} else if (first_char == '=') { // = heading
						int len = st.length();
						int k = len - 1;
						while (k > 0 && st.charAt(k) == '=')
							k--;
						if (k > 0 && k < len - 1) {
							int j = 1;
							while (j < len && st.charAt(j) == '=')
								j++;
							int level = Math.min(j, len - 1 - k); 
							if (level > 6) j = level;
							buf.append("<h").append(level).append(">").append(st, j, k + 1).append("</h").append(level).append(">");
						} else buf.append(st);
					} else {

						if (open_p) {
							buf.append("</p>");
							open_p = false;
						}
						if (st.startsWith("<table")) {
							table_open++;
							buf.append(st);
						} else {
							boolean p_allowed = !st.startsWith("<div") && !st.startsWith("<ol") && !st.startsWith("<ul") && !st.startsWith("<dl");
							if (p_allowed) {
                                buf.append("<p>");
                                open_p = true;
                            }
							buf.append(st);
						}

					}
				}
			}
		}
		if (buf.length() > 0) { // Add bold and italic formatting
			applyBoldItalic(buf, 0);
			if (open_p)	{
				buf.append("</p>");
				open_p = false;
			}
			applyRules(buf, result, linkBaseURL, language);
			buf.setLength(0);
		}

		for (int j = old_list.length() - 1; j >= 0; j--) {
			char ch = old_list.charAt(j);
			if (ch == '#') {
				result.append("</li></ol>");
			} else if (ch == '*') {
				result.append("</li></ul>");
			} else if ((ch == ':') || (ch == '+')) {
				result.append("</dd></dl>");
			} else if (ch == ';') {
				result.append("</dt></dl>");
			}
		}
		old_list.setLength(0);

		if (td_open) {
			result.append("</td>");
			td_open = false;
		}
		if (tr_open) {
			result.append("</tr>");
			tr_open = false;
		}
		if (table_open > 0) {
			result.append("</table>");//fill end of table
			table_open--;
		}

        replaceAll(result, "<wbr>", "");

        deleteAll(result, "<p></p>");
        replaceAll(result, "<li> <li", "<li"); //in case of expanded lines like: # <li ...> blabla...
		return result.toString();
    }

    private static void replaceAll(StringBuilder sb, String what, String replacement) {
        int idx = 0;
		int what_length = what.length();
		int replacement_length = replacement.length();
        while ((idx = sb.indexOf(what, idx)) != -1) {
			sb.replace(idx, idx + what_length, replacement);
			idx += replacement_length;
        }
    }

    private static void deleteAll(StringBuilder sb, String what) {
        int idx = 0;
		int what_length = what.length();
        while ((idx = sb.indexOf(what, idx)) != -1) {
			sb.delete(idx, idx + what_length);
        }
    }

    private static void applyRules(StringBuilder sb, StringBuilder result, String linkBaseURL, String language) {
		int ids = 0, ids2; int last = 0;
		int len = sb.length();
        while ((ids < len) && ((ids = sb.indexOf("[", ids)) != -1)) {
			int idx = sb.indexOf("[[", ids), idx2;
			if ((idx == -1) || (idx > ids))	{ //Process [http.... label]
				if (ids + 6 < len) {
					String prot = sb.substring(ids+1, ids+5).toLowerCase();
					ids2 = sb.indexOf("]", ids + 2);
					if ((ids2 != -1) && prot.startsWith("http")) {
						result.append(sb, last, ids);
						for (int i = ids + 1; i < ids2; i++) {
							char ch = sb.charAt(i);
							if (ch == ' ' || ch == '<') {
								if (ch == ' ')
									i++;//skip blank
								result.append(sb, i, ids2);
								break;
							}
						}
						last = ids2 + 1;
						ids = last;
					} else ids++;
				} else ids++;
			} else {//Process [[.. |.. ]]		Process [[.. :.. ]]	
				if ((idx2 = sb.indexOf("]]", idx + 2)) != -1) {//idx2==-1 <--> [[.... without closing ]]
                    int idxk, idxc, idxd;
                    //check idxk [[..[[...]]
                    while (((idxk = sb.indexOf("[[", idx + 2)) != -1) && (idxk < idx2)) {
                        idx = idxk;
                    }
					int idxb = sb.indexOf("|", idx + 2);
					String baseURL = null, display_text = null, internal_link;
					if ((idxb != -1)  && (idxb < idx2)) {
						internal_link = sb.substring(idx + 2, idxb).trim(); // +2 == "[[".length()
						display_text = sb.substring(idxb + 1, idx2);
					} else internal_link = sb.substring(idx + 2, idx2).trim();
					String path = internal_link;
					if (internal_link.startsWith(category_label)) {//suppress category, example: [[Category:English abbreviations|CROSS]]
						sb.delete(idx, idx2 + 2);
						len = sb.length();
						continue;
					}
					idxc = internal_link.indexOf(":", 1);
					if (idxc != -1)	{
						String media = internal_link.substring(0, idxc).toLowerCase();
						if (media.startsWith(":"))
							media = media.substring(1);//remove initial :
						if (not_allowed_media.contains(media)) {
							sb.delete(idx, idx2 + 2);
							len = sb.length();
							continue;
						}
						if (code2language.containsKey(media)) {
							baseURL = String.format(linkBaseURL, media);
							path = internal_link.substring(idxc + 1);
						} else if (media.equals("w")) {
							baseURL = "https://" + language + ".wikipedia.org/wiki/";
							path = internal_link.substring(idxc + 1);
/*						} else {
							baseURL = internal_link.substring(0, idxc + 1);
							path = internal_link.substring(idxc + 1);*/
						}
					}
					if (baseURL == null)
						baseURL = String.format(linkBaseURL, language);
					result.append(sb, last, idx);
					result.append("<a href=\"").append(baseURL);
					try {
						result.append(URLEncoder.encode(path, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						result.append(path); //fallback old way...
					}
					result.append("\">").append(display_text == null ? internal_link : display_text).append("</a>");

					last = idx2 + 2;
					ids = last;
				} else ids++;
			}
        }
		if (last < len)
			result.append(sb, last, len);
	}

    private static void applyBoldItalic(StringBuilder sb, int start) {
		int b = -1, i = -1;
		int idx = start, len;
        while ((idx < (len = sb.length())) && ((idx = sb.indexOf("''", idx)) != -1)) {
			boolean quote3 = (idx + 2 < len) && (sb.charAt(idx + 2) == '\'');// equivalent to sb.startsWith("\'\'\'", idx)
			boolean flag = !quote3 || ((idx + 4 < len) && (sb.charAt(idx + 3) == '\'') && (sb.charAt(idx + 4) == '\''));// equivalent to sb.startsWith("\'\'\'\'\'", idx)
			if (quote3) { // equivalent to sb.startsWith("\'\'\'", idx)
				if (b == idx) {//retract
					sb.replace(idx - 3, idx + 3, ""); idx -= 3; // 3 <-- lenght("<b>")	
					b = -1; quote3 = false; 
				} else if (b != -1) {
					sb.replace(idx, idx + 3, "</b>"); idx += 4; // 4 <-- lenght("</b>")
					b = -1; quote3 = false; 
				}
			}
			if (flag) {
				if (i == idx) {//retract
					sb.replace(idx - 3, idx + 2, ""); i = -1; idx -= 3; // 3 <-- lenght("<i>")
				} else if (i != -1) {
					if ((b == -1) || (b < i)) {
						sb.replace(idx, idx + 2, "</i>"); i = -1; idx += 4; // 4 <-- lenght("</i>")
					} else if (b != i + 3) {
						sb.replace(b - 3, b, "'</i>"); idx += 2;// 3 <-- lenght("<b>")
						b = -1; quote3 = false; 
						sb.replace(idx, idx + 2, "<i>"); idx += 3; // 3 <-- lenght("<i>")
						i = idx;
					} else {//swap "<i><b>" with "<b><i>"
						sb.replace(b - 2, b - 1, "i");
						sb.replace(i - 2, i - 1, "b");
						sb.replace(idx, idx + 2, "</i>"); i = -1; idx += 4; // 4 <-- lenght("</i>")
					}
				} else {
					sb.replace(idx, idx + 2, "<i>"); idx += 3; // 3 <-- lenght("<i>")
					i = idx;
				}
			}
			if (quote3) {
				sb.replace(idx, idx + 3, "<b>"); idx += 3; // 3 <-- lenght("<b>")
				b = idx;
			}
//			System.out.println("**"+sb+"**");
        }
		if ((b != -1) && (i != -1) && (b != i + 3))	{
			if (b < i) {
				sb.replace(b - 3, b, "'<i>");// 3 <-- lenght("<b>")
				sb.replace(i - 3, i, "</i>");// 3 <-- lenght("<i>")
			} else {
				sb.replace(b - 3, b, "'</i>");// 3 <-- lenght("<b>")
			}
		} else {
			if (b == sb.length()) {//retract
				sb.setLength(b - 3);// 3 <-- lenght("<b>")
				b = -1;
			} else if (i == sb.length()) {//retract
				sb.setLength(i - 3);// 3 <-- lenght("<i>")
				i = -1;
			}
			if (b != -1) 
				sb.append("</b>");
			if (i != -1) 
				sb.append("</i>");
		}
    }
} // end of WikiFormatter