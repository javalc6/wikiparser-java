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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;

/*
  This class contains miscellaneous utility functions
*/
final public class Utilities {
	public final static String PROPERTY_TEMPLATE = "template";
	public final static String PROPERTY_MODULE = "module";
	public final static String PROPERTY_REDIRECT = "redirect";
	public final static String PROPERTY_PARSER_IF = "parser.if";
	public final static String PROPERTY_PARSER_IFERROR = "parser.iferror";
	public final static String PROPERTY_PARSER_IFEXIST = "parser.ifexist";
	public final static String PROPERTY_PARSER_IFEXPR = "parser.ifexpr";
	public final static String PROPERTY_PARSER_IFEQ = "parser.ifeq";


	private static HashMap<String, String> code2language;

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

    public static ResourceBundle getResourceBundle(Locale locale) {
        try {
			File file = new File("wiki");
			ClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()});
			return ResourceBundle.getBundle("wiktionary", locale, loader);
        } catch (MissingResourceException e) {
			return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getResourceString(ResourceBundle bundle, String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    public static HashMap<String, String> getLanguageNames() {
		return code2language;
	}

	public static int findValidEqualSign(String str) {//find valid position of = sign
		if (str == null || str.isEmpty()) return -1;
		int len = str.length();
		int noc = 2; //number of open curls
		int i = 0;
		while (i < len) {
			char ch = str.charAt(i);
			switch (ch) {
				case '{':
					boolean found = false;
					while (++i < len && ((ch = str.charAt(i)) == '{')) {
						noc++; found = true;
					}
					if (found)
						noc++;
					continue;
				case '}':
					boolean found2 = false;
					while (noc > 0 && (++i < len) && ((ch = str.charAt(i)) == '}')) {
						noc--; found2 = true;
					}
					if (found2) {
						if (noc > 0)
							noc--;
						if (noc == 0)
							return -1;
						continue;
					}
					if (noc > 0) continue;
					break;
				case '<':
					if (str.startsWith("<nowiki>", i)) {
						int end = str.indexOf("</nowiki>", i + 8);
						if (end != -1) {
							i = end + 9;//go after tag "</nowiki>"
							continue;
						}
					} else if (str.startsWith("<code>", i)) {
						int end = str.indexOf("</code>", i + 6);
						if (end != -1) {
							i = end + 7;//go after tag "</code>"
							continue;
						}
					} else if (str.startsWith("<math>", i)) {
						int end = str.indexOf("</math>", i + 6);
						if (end != -1) {
							i = end + 7;//go after tag "</math>"
							continue;
						}
					}
					int end = str.indexOf(">", i);
					if (end != -1) {
						i = end + 1;// go after ">"		
						continue;
					}
					break;
				case '|':
					if (noc < 3)
						return -1;
					break;
				case '=':
					if (noc < 3)
						return i;
					break;
			}
			i++;
		}
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

	public static void delete_comments(StringBuilder text) { // delete html comments <!-- -->
		int comment;
		while ((comment = text.indexOf("<!--")) != -1) {// find html comments <!-- -->
			int eoc = text.indexOf("-->", comment + 3);
			if (eoc != -1) {
				text.delete(comment, eoc + 3);
				if ((comment > 0) && (text.charAt(comment - 1)  == '\n') && (comment < text.length()) && (text.charAt(comment)  == '\n')) {//rimuove l'eventuale riga vuota in caso di eliminazione del commento
					text.deleteCharAt(comment);
				}
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

//reference: https://gerrit.wikimedia.org/g/mediawiki/core/%2B/HEAD/includes/languages/data/Names.php
	static {
		code2language = new HashMap<>();
		code2language.put("aa", "Qafár af"); // Afar
		code2language.put("ab", "аԥсшәа"); // Abkhaz
		code2language.put("abs", "bahasa ambon"); // Ambonese Malay, T193566
		code2language.put("ace", "Acèh"); // Aceh
		code2language.put("acm", "عراقي"); // Iraqi (Mesopotamian) Arabic
		code2language.put("ady", "адыгабзэ"); // Adyghe
		code2language.put("ady-cyrl", "адыгабзэ"); // Adyghe
		code2language.put("aeb", "تونسي / Tûnsî"); // Tunisian Arabic (multiple scripts - defaults to Arabic)
		code2language.put("aeb-arab", "تونسي"); // Tunisian Arabic (Arabic Script)
		code2language.put("aeb-latn", "Tûnsî"); // Tunisian Arabic (Latin Script)
		code2language.put("af", "Afrikaans"); // Afrikaans
		code2language.put("ak", "Akan"); // Akan
		code2language.put("aln", "Gegë"); // Gheg Albanian
		code2language.put("als", "Alemannisch"); // Alemannic -- not a valid code, for compatibility. See gsw.
		code2language.put("alt", "алтай тил"); // Altai, T254854
		code2language.put("am", "አማርኛ"); // Amharic
		code2language.put("ami", "Pangcah"); // Amis
		code2language.put("an", "aragonés"); // Aragonese
		code2language.put("ang", "Ænglisc"); // Old English, T25283
		code2language.put("ann", "Obolo"); // Obolo
		code2language.put("anp", "अंगिका"); // Angika
		code2language.put("ar", "العربية"); // Arabic
		code2language.put("arc", "ܐܪܡܝܐ"); // Aramaic
		code2language.put("arn", "mapudungun"); // Mapuche, Mapudungu, Araucanian (Araucano)
		code2language.put("arq", "جازايرية"); // Algerian Spoken Arabic
		code2language.put("ary", "الدارجة"); // Moroccan Spoken Arabic
		code2language.put("arz", "مصرى"); // Egyptian Spoken Arabic
		code2language.put("as", "অসমীয়া"); // Assamese
		code2language.put("ase", "American sign language"); // American sign language
		code2language.put("ast", "asturianu"); // Asturian
		code2language.put("atj", "Atikamekw"); // Atikamekw
		code2language.put("av", "авар"); // Avar
		code2language.put("avk", "Kotava"); // Kotava
		code2language.put("awa", "अवधी"); // Awadhi
		code2language.put("ay", "Aymar aru"); // Aymara
		code2language.put("az", "azərbaycanca"); // Azerbaijani
		code2language.put("azb", "تۆرکجه"); // South Azerbaijani
		code2language.put("ba", "башҡортса"); // Bashkir
		code2language.put("ban", "Basa Bali"); // Balinese (Latin script)
		code2language.put("ban-bali", "ᬩᬲᬩᬮᬶ"); // Balinese (Balinese script)
		code2language.put("bar", "Boarisch"); // Bavarian (Austro-Bavarian and South Tyrolean)
		code2language.put("bat-smg", "žemaitėška"); // Samogitian (deprecated code, "sgs" in ISO 639-3 since 2010-06-30 )
		code2language.put("bbc", "Batak Toba"); // Batak Toba (falls back to bbc-latn)
		code2language.put("bbc-latn", "Batak Toba"); // Batak Toba
		code2language.put("bcc", "جهلسری بلوچی"); // Southern Balochi
		code2language.put("bci", "wawle"); // Baoulé
		code2language.put("bcl", "Bikol Central"); // Bikol: Central Bicolano language
		code2language.put("be", "беларуская"); // Belarusian normative
		code2language.put("be-tarask", "беларуская (тарашкевіца)"); // Belarusian in Taraskievica orthography
		code2language.put("be-x-old", "беларуская (тарашкевіца)"); // (be-tarask compat)
		code2language.put("bew", "Betawi"); // Betawi
		code2language.put("bg", "български"); // Bulgarian
		code2language.put("bgn", "روچ کپتین بلوچی"); // Western Balochi
		code2language.put("bh", "भोजपुरी"); // Bihari macro language. Falls back to Bhojpuri (bho)
		code2language.put("bho", "भोजपुरी"); // Bhojpuri
		code2language.put("bi", "Bislama"); // Bislama
		code2language.put("bjn", "Banjar"); // Banjarese
		code2language.put("blk", "ပအိုဝ်ႏဘာႏသာႏ"); // Pa"O
		code2language.put("bm", "bamanankan"); // Bambara
		code2language.put("bn", "বাংলা"); // Bengali
		code2language.put("bo", "བོད་ཡིག"); // Tibetan
		code2language.put("bpy", "বিষ্ণুপ্রিয়া মণিপুরী"); // Bishnupriya Manipuri
		code2language.put("bqi", "بختیاری"); // Bakthiari
		code2language.put("br", "brezhoneg"); // Breton
		code2language.put("brh", "Bráhuí"); // Brahui
		code2language.put("bs", "bosanski"); // Bosnian
		code2language.put("btm", "Batak Mandailing"); // Batak Mandailing
		code2language.put("bto", "Iriga Bicolano"); // Rinconada Bikol
		code2language.put("bug", "ᨅᨔ ᨕᨘᨁᨗ"); // Buginese
		code2language.put("bxr", "буряад"); // Buryat (Russia)
		code2language.put("ca", "català"); // Catalan
		code2language.put("cbk-zam", "Chavacano de Zamboanga"); // Zamboanga Chavacano, T124657
		code2language.put("cdo", "閩東語 / Mìng-dĕ̤ng-ngṳ̄"); // Min-dong (multiple scripts - defaults to Latin)
		code2language.put("ce", "нохчийн"); // Chechen
		code2language.put("ceb", "Cebuano"); // Cebuano
		code2language.put("ch", "Chamoru"); // Chamorro
		code2language.put("cho", "Chahta anumpa"); // Choctaw
		code2language.put("chr", "ᏣᎳᎩ"); // Cherokee
		code2language.put("chy", "Tsetsêhestâhese"); // Cheyenne
		code2language.put("ckb", "کوردی"); // Central Kurdish
		code2language.put("co", "corsu"); // Corsican
		code2language.put("cps", "Capiceño"); // Capiznon
		code2language.put("cr", "Nēhiyawēwin / ᓀᐦᐃᔭᐍᐏᐣ"); // Cree
		code2language.put("crh", "qırımtatarca"); // Crimean Tatar (multiple scripts - defaults to Latin)
		code2language.put("crh-cyrl", "къырымтатарджа (Кирилл)"); // Crimean Tatar (Cyrillic)
		code2language.put("crh-latn", "qırımtatarca (Latin)"); // Crimean Tatar (Latin)
		code2language.put("crh-ro", "tatarşa"); // Crimean Tatar (Romania)
		code2language.put("cs", "čeština"); // Czech
		code2language.put("csb", "kaszëbsczi"); // Cassubian
		code2language.put("cu", "словѣньскъ / ⰔⰎⰑⰂⰡⰐⰠⰔⰍⰟ"); // Old Church Slavonic (ancient language)
		code2language.put("cv", "чӑвашла"); // Chuvash
		code2language.put("cy", "Cymraeg"); // Welsh
		code2language.put("da", "dansk"); // Danish
		code2language.put("dag", "dagbanli"); // Dagbani
		code2language.put("de", "Deutsch"); // German ("Du")
		code2language.put("de-at", "Österreichisches Deutsch"); // Austrian German
		code2language.put("de-ch", "Schweizer Hochdeutsch"); // Swiss Standard German
		code2language.put("de-formal", "Deutsch (Sie-Form)"); // German - formal address ("Sie")
		code2language.put("dga", "Dagaare"); // Southern Dagaare
		code2language.put("din", "Thuɔŋjäŋ"); // Dinka
		code2language.put("diq", "Zazaki"); // Zazaki
		code2language.put("dsb", "dolnoserbski"); // Lower Sorbian
		code2language.put("dtp", "Dusun Bundu-liwan"); // Central Dusun
		code2language.put("dty", "डोटेली"); // Doteli
		code2language.put("dv", "ދިވެހިބަސް"); // Dhivehi
		code2language.put("dz", "ཇོང་ཁ"); // Dzongkha (Bhutan)
		code2language.put("ee", "eʋegbe"); // Éwé
		code2language.put("egl", "Emiliàn"); // Emilian
		code2language.put("el", "Ελληνικά"); // Greek
		code2language.put("eml", "emiliàn e rumagnòl"); // Emiliano-Romagnolo / Sammarinese
		code2language.put("en", "English"); // English
		code2language.put("en-ca", "Canadian English"); // Canadian English
		code2language.put("en-gb", "British English"); // British English
		code2language.put("en-x-piglatin", "Igpay Atinlay"); // Pig Latin, for variant development
		code2language.put("eo", "Esperanto"); // Esperanto
		code2language.put("es", "español"); // Spanish
		code2language.put("es-419", "español de América Latina"); // Spanish for the Latin America and Caribbean region
		code2language.put("es-formal", "español (formal)"); // Spanish formal address
		code2language.put("et", "eesti"); // Estonian
		code2language.put("eu", "euskara"); // Basque
		code2language.put("ext", "estremeñu"); // Extremaduran
		code2language.put("fa", "فارسی"); // Persian
		code2language.put("fat", "mfantse"); // Fante
		code2language.put("ff", "Fulfulde"); // Fulfulde, Maasina
		code2language.put("fi", "suomi"); // Finnish
		code2language.put("fit", "meänkieli"); // Tornedalen Finnish
		code2language.put("fiu-vro", "võro"); // Võro (deprecated code, "vro" in ISO 639-3 since 2009-01-16)
		code2language.put("fj", "Na Vosa Vakaviti"); // Fijian
		code2language.put("fo", "føroyskt"); // Faroese
		code2language.put("fon", "fɔ̀ngbè"); // Fon
		code2language.put("fr", "français"); // French
		code2language.put("frc", "français cadien"); // Cajun French
		code2language.put("frp", "arpetan"); // Franco-Provençal/Arpitan
		code2language.put("frr", "Nordfriisk"); // North Frisian
		code2language.put("fur", "furlan"); // Friulian
		code2language.put("fy", "Frysk"); // Frisian
		code2language.put("ga", "Gaeilge"); // Irish
		code2language.put("gaa", "Ga"); // Ga
		code2language.put("gag", "Gagauz"); // Gagauz
		code2language.put("gan", "贛語"); // Gan (multiple scripts - defaults to Traditional Han)
		code2language.put("gan-hans", "赣语（简体）"); // Gan (Simplified Han)
		code2language.put("gan-hant", "贛語（繁體）"); // Gan (Traditional Han)
		code2language.put("gcr", "kriyòl gwiyannen"); // Guianan Creole
		code2language.put("gd", "Gàidhlig"); // Scots Gaelic
		code2language.put("gl", "galego"); // Galician
		code2language.put("gld", "на̄ни"); // Nanai
		code2language.put("glk", "گیلکی"); // Gilaki
		code2language.put("gn", "Avañe\"ẽ"); // Guaraní, Paraguayan
		code2language.put("gom", "गोंयची कोंकणी / Gõychi Konknni"); // Goan Konkani
		code2language.put("gom-deva", "गोंयची कोंकणी"); // Goan Konkani (Devanagari script)
		code2language.put("gom-latn", "Gõychi Konknni"); // Goan Konkani (Latin script)
		code2language.put("gor", "Bahasa Hulontalo"); // Gorontalo
		code2language.put("got", "𐌲𐌿𐍄𐌹𐍃𐌺"); // Gothic
		code2language.put("gpe", "Ghanaian Pidgin"); // Ghanaian Pidgin
		code2language.put("grc", "Ἀρχαία ἑλληνικὴ"); // Ancient Greek
		code2language.put("gsw", "Alemannisch"); // Alemannic
		code2language.put("gu", "ગુજરાતી"); // Gujarati
		code2language.put("guc", "wayuunaiki"); // Wayuu
		code2language.put("gur", "farefare"); // Farefare
		code2language.put("guw", "gungbe"); // Gun
		code2language.put("gv", "Gaelg"); // Manx
		code2language.put("ha", "Hausa"); // Hausa
		code2language.put("hak", "客家語/Hak-kâ-ngî"); // Hakka
		code2language.put("haw", "Hawaiʻi"); // Hawaiian
		code2language.put("he", "עברית"); // Hebrew
		code2language.put("hi", "हिन्दी"); // Hindi
		code2language.put("hif", "Fiji Hindi"); // Fijian Hindi (multiple scripts - defaults to Latin)
		code2language.put("hif-latn", "Fiji Hindi"); // Fiji Hindi (Latin script)
		code2language.put("hil", "Ilonggo"); // Hiligaynon
		code2language.put("hno", "ہندکو"); // Hindko
		code2language.put("ho", "Hiri Motu"); // Hiri Motu
		code2language.put("hr", "hrvatski"); // Croatian
		code2language.put("hrx", "Hunsrik"); // Riograndenser Hunsrückisch
		code2language.put("hsb", "hornjoserbsce"); // Upper Sorbian
		code2language.put("hsn", "湘语"); // Xiang Chinese
		code2language.put("ht", "Kreyòl ayisyen"); // Haitian Creole French
		code2language.put("hu", "magyar"); // Hungarian
		code2language.put("hu-formal", "magyar (formal)"); // Hungarian formal address
		code2language.put("hy", "հայերեն"); // Armenian, T202611
		code2language.put("hyw", "Արեւմտահայերէն"); // Western Armenian, T201276, T219975
		code2language.put("hz", "Otsiherero"); // Herero
		code2language.put("ia", "interlingua"); // Interlingua (IALA)
		code2language.put("id", "Bahasa Indonesia"); // Indonesian
		code2language.put("ie", "Interlingue"); // Interlingue (Occidental)
		code2language.put("ig", "Igbo"); // Igbo
		code2language.put("igl", "Igala"); // Igala
		code2language.put("ii", "ꆇꉙ"); // Sichuan Yi
		code2language.put("ik", "Iñupiatun"); // Inupiaq
		code2language.put("ike-cans", "ᐃᓄᒃᑎᑐᑦ"); // Inuktitut, Eastern Canadian (Unified Canadian Aboriginal Syllabics)
		code2language.put("ike-latn", "inuktitut"); // Inuktitut, Eastern Canadian (Latin script)
		code2language.put("ilo", "Ilokano"); // Ilokano
		code2language.put("inh", "гӀалгӀай"); // Ingush
		code2language.put("io", "Ido"); // Ido
		code2language.put("is", "íslenska"); // Icelandic
		code2language.put("it", "italiano"); // Italian
		code2language.put("iu", "ᐃᓄᒃᑎᑐᑦ / inuktitut"); // Inuktitut (macro language, see ike/ikt, falls back to ike-cans)
		code2language.put("ja", "日本語"); // Japanese
		code2language.put("jam", "Patois"); // Jamaican Creole English
		code2language.put("jbo", "la .lojban."); // Lojban
		code2language.put("jut", "jysk"); // Jutish / Jutlandic
		code2language.put("jv", "Jawa"); // Javanese
		code2language.put("ka", "ქართული"); // Georgian
		code2language.put("kaa", "Qaraqalpaqsha"); // Karakalpak
		code2language.put("kab", "Taqbaylit"); // Kabyle
		code2language.put("kbd", "адыгэбзэ"); // Kabardian
		code2language.put("kbd-cyrl", "адыгэбзэ"); // Kabardian (Cyrillic)
		code2language.put("kbp", "Kabɩyɛ"); // Kabiyè
		code2language.put("kcg", "Tyap"); // Tyap
		code2language.put("kea", "kabuverdianu"); // Cape Verdean Creole
		code2language.put("kg", "Kongo"); // Kongo, (FIXME!) should probably be KiKongo or KiKoongo
		code2language.put("khw", "کھوار"); // Khowar
		code2language.put("ki", "Gĩkũyũ"); // Gikuyu
		code2language.put("kiu", "Kırmancki"); // Kirmanjki
		code2language.put("kj", "Kwanyama"); // Kwanyama
		code2language.put("kjh", "хакас"); // Khakas
		code2language.put("kjp", "ဖၠုံလိက်"); // Eastern Pwo (multiple scripts - defaults to Burmese script)
		code2language.put("kk", "қазақша"); // Kazakh (multiple scripts - defaults to Cyrillic)
		code2language.put("kk-arab", "قازاقشا (تٴوتە)"); // Kazakh Arabic
		code2language.put("kk-cn", "قازاقشا (جۇنگو)"); // Kazakh (China)
		code2language.put("kk-cyrl", "қазақша (кирил)"); // Kazakh Cyrillic
		code2language.put("kk-kz", "қазақша (Қазақстан)"); // Kazakh (Kazakhstan)
		code2language.put("kk-latn", "qazaqşa (latın)"); // Kazakh Latin
		code2language.put("kk-tr", "qazaqşa (Türkïya)"); // Kazakh (Turkey)
		code2language.put("kl", "kalaallisut"); // Inuktitut, Greenlandic/Greenlandic/Kalaallisut (kal)
		code2language.put("km", "ភាសាខ្មែរ"); // Khmer, Central
		code2language.put("kn", "ಕನ್ನಡ"); // Kannada
		code2language.put("ko", "한국어"); // Korean
		code2language.put("ko-kp", "조선말"); // Korean (DPRK), T190324
		code2language.put("koi", "перем коми"); // Komi-Permyak
		code2language.put("kr", "kanuri"); // Kanuri
		code2language.put("krc", "къарачай-малкъар"); // Karachay-Balkar
		code2language.put("kri", "Krio"); // Krio
		code2language.put("krj", "Kinaray-a"); // Kinaray-a
		code2language.put("krl", "karjal"); // Karelian
		code2language.put("ks", "कॉशुर / کٲشُر"); // Kashmiri (multiple scripts - defaults to Perso-Arabic)
		code2language.put("ks-arab", "کٲشُر"); // Kashmiri (Perso-Arabic script)
		code2language.put("ks-deva", "कॉशुर"); // Kashmiri (Devanagari script)
		code2language.put("ksh", "Ripoarisch"); // Ripuarian
		code2language.put("ksw", "စှီၤ"); // S"gaw Karen
		code2language.put("ku", "kurdî"); // Kurdish (multiple scripts - defaults to Latin)
		code2language.put("ku-arab", "كوردي (عەرەبی)"); // Northern Kurdish (Arabic script) (falls back to ckb)
		code2language.put("ku-latn", "kurdî (latînî)"); // Northern Kurdish (Latin script)
		code2language.put("kum", "къумукъ"); // Kumyk (Cyrillic, "kum-latn" for Latin script)
		code2language.put("kus", "Kʋsaal"); // Kusaal
		code2language.put("kv", "коми"); // Komi-Zyrian (Cyrillic is common script but also written in Latin script)
		code2language.put("kw", "kernowek"); // Cornish
		code2language.put("ky", "кыргызча"); // Kirghiz
		code2language.put("la", "Latina"); // Latin
		code2language.put("lad", "Ladino"); // Ladino
		code2language.put("lb", "Lëtzebuergesch"); // Luxembourgish
		code2language.put("lbe", "лакку"); // Lak
		code2language.put("lez", "лезги"); // Lezgi
		code2language.put("lfn", "Lingua Franca Nova"); // Lingua Franca Nova
		code2language.put("lg", "Luganda"); // Ganda
		code2language.put("li", "Limburgs"); // Limburgian
		code2language.put("lij", "Ligure"); // Ligurian
		code2language.put("liv", "Līvõ kēļ"); // Livonian
		code2language.put("lki", "لەکی"); // Laki
		code2language.put("lld", "Ladin"); // Ladin
		code2language.put("lmo", "lombard"); // Lombard - T283423
		code2language.put("ln", "lingála"); // Lingala
		code2language.put("lo", "ລາວ"); // Laotian
		code2language.put("loz", "Silozi"); // Lozi
		code2language.put("lrc", "لۊری شومالی"); // Northern Luri
		code2language.put("lt", "lietuvių"); // Lithuanian
		code2language.put("ltg", "latgaļu"); // Latgalian
		code2language.put("lus", "Mizo ţawng"); // Mizo/Lushai
		code2language.put("luz", "لئری دوٙمینی"); // Southern Luri
		code2language.put("lv", "latviešu"); // Latvian
		code2language.put("lzh", "文言"); // Literary Chinese, T10217
		code2language.put("lzz", "Lazuri"); // Laz
		code2language.put("mad", "Madhurâ"); // Madurese, T264582
		code2language.put("mag", "मगही"); // Magahi
		code2language.put("mai", "मैथिली"); // Maithili
		code2language.put("map-bms", "Basa Banyumasan"); // Banyumasan ("jv-x-bms")
		code2language.put("mdf", "мокшень"); // Moksha
		code2language.put("mg", "Malagasy"); // Malagasy
		code2language.put("mh", "Ebon"); // Marshallese
		code2language.put("mhr", "олык марий"); // Eastern Mari
		code2language.put("mi", "Māori"); // Maori
		code2language.put("min", "Minangkabau"); // Minangkabau
		code2language.put("mk", "македонски"); // Macedonian
		code2language.put("ml", "മലയാളം"); // Malayalam
		code2language.put("mn", "монгол"); // Halh Mongolian (Cyrillic) (ISO 639-3: khk)
		code2language.put("mni", "ꯃꯤꯇꯩ ꯂꯣꯟ"); // Manipuri/Meitei
		code2language.put("mnw", "ဘာသာ မန်"); // Mon, T201583
		code2language.put("mo", "молдовеняскэ"); // Moldovan, deprecated (ISO 639-2: ro-Cyrl-MD)
		code2language.put("mos", "moore"); // Mooré
		code2language.put("mr", "मराठी"); // Marathi
		code2language.put("mrh", "Mara"); // Mara
		code2language.put("mrj", "кырык мары"); // Hill Mari
		code2language.put("ms", "Bahasa Melayu"); // Malay
		code2language.put("ms-arab", "بهاس ملايو"); // Malay (Arabic Jawi script)
		code2language.put("mt", "Malti"); // Maltese
		code2language.put("mus", "Mvskoke"); // Muskogee/Creek
		code2language.put("mwl", "Mirandés"); // Mirandese
		code2language.put("my", "မြန်မာဘာသာ"); // Burmese
		code2language.put("myv", "эрзянь"); // Erzya
		code2language.put("mzn", "مازِرونی"); // Mazanderani
		code2language.put("na", "Dorerin Naoero"); // Nauruan
		code2language.put("nah", "Nāhuatl"); // Nahuatl (added to ISO 639-3 on 2006-10-31)
		code2language.put("nan", "Bân-lâm-gú"); // Min-nan, T10217
		code2language.put("nap", "Napulitano"); // Neapolitan, T45793
		code2language.put("nb", "norsk bokmål"); // Norwegian (Bokmal)
		code2language.put("nds", "Plattdüütsch"); // Low German ""or"" Low Saxon
		code2language.put("nds-nl", "Nedersaksies"); // aka Nedersaksisch: Dutch Low Saxon
		code2language.put("ne", "नेपाली"); // Nepali
		code2language.put("new", "नेपाल भाषा"); // Newar / Nepal Bhasha
		code2language.put("ng", "Oshiwambo"); // Ndonga
		code2language.put("nia", "Li Niha"); // Nias, T263968
		code2language.put("niu", "Niuē"); // Niuean
		code2language.put("nl", "Nederlands"); // Dutch
		code2language.put("nl-informal", "Nederlands (informeel)"); // Dutch (informal address ("je"))
		code2language.put("nmz", "nawdm"); // Nawdm
		code2language.put("nn", "norsk nynorsk"); // Norwegian (Nynorsk)
		code2language.put("no", "norsk"); // Norwegian macro language (falls back to nb).
		code2language.put("nod", "ᨣᩤᩴᨾᩮᩬᩥᨦ"); // Northern Thai
		code2language.put("nog", "ногайша"); // Nogai
		code2language.put("nov", "Novial"); // Novial
		code2language.put("nqo", "ߒߞߏ"); // N"Ko
		code2language.put("nrm", "Nouormand"); // Norman (invalid code; "nrf" in ISO 639 since 2014)
		code2language.put("nso", "Sesotho sa Leboa"); // Northern Sotho
		code2language.put("nv", "Diné bizaad"); // Navajo
		code2language.put("ny", "Chi-Chewa"); // Chichewa
		code2language.put("nyn", "runyankore"); // Nkore
		code2language.put("nys", "Nyunga"); // Nyungar
		code2language.put("oc", "occitan"); // Occitan
		code2language.put("ojb", "Ojibwemowin"); // Ojibwe
		code2language.put("olo", "livvinkarjala"); // Livvi-Karelian
		code2language.put("om", "Oromoo"); // Oromo
		code2language.put("or", "ଓଡ଼ିଆ"); // Oriya
		code2language.put("os", "ирон"); // Ossetic, T31091
		code2language.put("pa", "ਪੰਜਾਬੀ"); // Eastern Punjabi (Gurmukhi script) (pan)
		code2language.put("pag", "Pangasinan"); // Pangasinan
		code2language.put("pam", "Kapampangan"); // Pampanga
		code2language.put("pap", "Papiamentu"); // Papiamentu
		code2language.put("pcd", "Picard"); // Picard
		code2language.put("pcm", "Naijá"); // Nigerian Pidgin
		code2language.put("pdc", "Deitsch"); // Pennsylvania German
		code2language.put("pdt", "Plautdietsch"); // Plautdietsch/Mennonite Low German
		code2language.put("pfl", "Pälzisch"); // Palatinate German
		code2language.put("pi", "पालि"); // Pali
		code2language.put("pih", "Norfuk / Pitkern"); // Norfuk/Pitcairn/Norfolk
		code2language.put("pl", "polski"); // Polish
		code2language.put("pms", "Piemontèis"); // Piedmontese
		code2language.put("pnb", "پنجابی"); // Western Punjabi
		code2language.put("pnt", "Ποντιακά"); // Pontic/Pontic Greek
		code2language.put("prg", "prūsiskan"); // Prussian
		code2language.put("ps", "پښتو"); // Pashto
		code2language.put("pt", "português"); // Portuguese
		code2language.put("pt-br", "português do Brasil"); // Brazilian Portuguese
		code2language.put("pwn", "pinayuanan"); // Paiwan
		code2language.put("qu", "Runa Simi"); // Southern Quechua
		code2language.put("qug", "Runa shimi"); // Kichwa/Northern Quechua (temporarily used until Kichwa has its own)
		code2language.put("rgn", "Rumagnôl"); // Romagnol
		code2language.put("rif", "Tarifit"); // Tarifit
		code2language.put("rki", "ရခိုင်"); // Arakanese
		code2language.put("rm", "rumantsch"); // Raeto-Romance
		code2language.put("rmc", "romaňi čhib"); // Carpathian Romany
		code2language.put("rmy", "romani čhib"); // Vlax Romany
		code2language.put("rn", "ikirundi"); // Rundi (Kirundi)
		code2language.put("ro", "română"); // Romanian
		code2language.put("roa-rup", "armãneashti"); // Aromanian (deprecated code, "rup" exists in ISO 639-3)
		code2language.put("roa-tara", "tarandíne"); // Tarantino ("nap-x-tara")
		code2language.put("rsk", "руски"); // Pannonian Rusyn
		code2language.put("ru", "русский"); // Russian
		code2language.put("rue", "русиньскый"); // Rusyn
		code2language.put("rup", "armãneashti"); // Aromanian
		code2language.put("ruq", "Vlăheşte"); // Megleno-Romanian (multiple scripts - defaults to Latin)
		code2language.put("ruq-cyrl", "Влахесте"); // Megleno-Romanian (Cyrillic script)
		code2language.put("ruq-grek", "Βλαεστε"); // Megleno-Romanian (Greek script)
		code2language.put("ruq-latn", "Vlăheşte"); // Megleno-Romanian (Latin script)
		code2language.put("rw", "Ikinyarwanda"); // Kinyarwanda
		code2language.put("ryu", "うちなーぐち"); // Okinawan
		code2language.put("sa", "संस्कृतम्"); // Sanskrit
		code2language.put("sah", "саха тыла"); // Sakha
		code2language.put("sat", "ᱥᱟᱱᱛᱟᱲᱤ"); // Santali
		code2language.put("sc", "sardu"); // Sardinian
		code2language.put("scn", "sicilianu"); // Sicilian
		code2language.put("sco", "Scots"); // Scots
		code2language.put("sd", "سنڌي"); // Sindhi
		code2language.put("sdc", "Sassaresu"); // Sassarese
		code2language.put("sdh", "کوردی خوارگ"); // Southern Kurdish
		code2language.put("se", "davvisámegiella"); // Northern Sami
		code2language.put("se-fi", "davvisámegiella (Suoma bealde)"); // Northern Sami (Finland)
		code2language.put("se-no", "davvisámegiella (Norgga bealde)"); // Northern Sami (Norway)
		code2language.put("se-se", "davvisámegiella (Ruoŧa bealde)"); // Northern Sami (Sweden)
		code2language.put("sei", "Cmique Itom"); // Seri
		code2language.put("ses", "Koyraboro Senni"); // Koyraboro Senni
		code2language.put("sg", "Sängö"); // Sango/Sangho
		code2language.put("sgs", "žemaitėška"); // Samogitian
		code2language.put("sh", "srpskohrvatski / српскохрватски"); // Serbo-Croatian (multiple scripts - defaults to Latin)
		code2language.put("sh-cyrl", "српскохрватски (ћирилица)"); // Serbo-Croatian (Cyrillic script)
		code2language.put("sh-latn", "srpskohrvatski (latinica)"); // Serbo-Croatian (Latin script) (default)
		code2language.put("shi", "Taclḥit"); // Tachelhit, Shilha (multiple scripts - defaults to Latin)
		code2language.put("shi-latn", "Taclḥit"); // Tachelhit (Latin script)
		code2language.put("shi-tfng", "ⵜⴰⵛⵍⵃⵉⵜ"); // Tachelhit (Tifinagh script)
		code2language.put("shn", "ၽႃႇသႃႇတႆး "); // Shan
		code2language.put("shy", "tacawit"); // Shawiya (Multiple scripts - defaults to Latin)
		code2language.put("shy-latn", "tacawit"); // Shawiya (Latin script) - T194047
		code2language.put("si", "සිංහල"); // Sinhalese
		code2language.put("simple", "Simple English"); // Simple English
		code2language.put("sjd", "кӣллт са̄мь кӣлл"); // Kildin Sami
		code2language.put("sje", "bidumsámegiella"); // Pite Sami
		code2language.put("sk", "slovenčina"); // Slovak
		code2language.put("skr", "سرائیکی"); // Saraiki (multiple scripts - defaults to Arabic)
		code2language.put("skr-arab", "سرائیکی"); // Saraiki (Arabic script)
		code2language.put("sl", "slovenščina"); // Slovenian
		code2language.put("sli", "Schläsch"); // Lower Selisian
		code2language.put("sm", "Gagana Samoa"); // Samoan
		code2language.put("sma", "åarjelsaemien"); // Southern Sami
		code2language.put("smn", "anarâškielâ"); // Inari Sami
		code2language.put("sms", "nuõrttsääʹmǩiõll"); // Skolt Sami
		code2language.put("sn", "chiShona"); // Shona
		code2language.put("so", "Soomaaliga"); // Somali
		code2language.put("sq", "shqip"); // Albanian
		code2language.put("sr", "српски / srpski"); // Serbian (multiple scripts - defaults to Cyrillic)
		code2language.put("sr-ec", "српски (ћирилица)"); // Serbian Cyrillic ekavian
		code2language.put("sr-el", "srpski (latinica)"); // Serbian Latin ekavian
		code2language.put("srn", "Sranantongo"); // Sranan Tongo
		code2language.put("sro", "sardu campidanesu"); // Campidanese Sardinian
		code2language.put("ss", "SiSwati"); // Swati
		code2language.put("st", "Sesotho"); // Southern Sotho
		code2language.put("stq", "Seeltersk"); // Saterland Frisian
		code2language.put("sty", "себертатар"); // Siberian Tatar
		code2language.put("su", "Sunda"); // Sundanese
		code2language.put("sv", "svenska"); // Swedish
		code2language.put("sw", "Kiswahili"); // Swahili
		code2language.put("syl", "ꠍꠤꠟꠐꠤ"); // Sylheti
		code2language.put("szl", "ślůnski"); // Silesian
		code2language.put("szy", "Sakizaya"); // Sakizaya - T174601
		code2language.put("ta", "தமிழ்"); // Tamil
		code2language.put("tay", "Tayal"); // Atayal
		code2language.put("tcy", "ತುಳು"); // Tulu
		code2language.put("tdd", "ᥖᥭᥰᥖᥬᥳᥑᥨᥒᥰ"); // Tai Nüa
		code2language.put("te", "తెలుగు"); // Telugu
		code2language.put("tet", "tetun"); // Tetun
		code2language.put("tg", "тоҷикӣ"); // Tajiki (falls back to tg-cyrl)
		code2language.put("tg-cyrl", "тоҷикӣ"); // Tajiki (Cyrllic script) (default)
		code2language.put("tg-latn", "tojikī"); // Tajiki (Latin script)
		code2language.put("th", "ไทย"); // Thai
		code2language.put("ti", "ትግርኛ"); // Tigrinya
		code2language.put("tk", "Türkmençe"); // Turkmen
		code2language.put("tl", "Tagalog"); // Tagalog
		code2language.put("tly", "tolışi"); // Talysh
		code2language.put("tly-cyrl", "толыши"); // Talysh (Cyrillic)
		code2language.put("tn", "Setswana"); // Setswana
		code2language.put("to", "lea faka-Tonga"); // Tonga (Tonga Islands)
		code2language.put("tok", "toki pona"); // Toki Pona
		code2language.put("tpi", "Tok Pisin"); // Tok Pisin
		code2language.put("tr", "Türkçe"); // Turkish
		code2language.put("tru", "Ṫuroyo"); // Turoyo
		code2language.put("trv", "Seediq"); // Taroko
		code2language.put("ts", "Xitsonga"); // Tsonga
		code2language.put("tt", "татарча / tatarça"); // Tatar (multiple scripts - defaults to Cyrillic)
		code2language.put("tt-cyrl", "татарча"); // Tatar (Cyrillic script) (default)
		code2language.put("tt-latn", "tatarça"); // Tatar (Latin script)
		code2language.put("tum", "chiTumbuka"); // Tumbuka
		code2language.put("tw", "Twi"); // Twi
		code2language.put("ty", "reo tahiti"); // Tahitian
		code2language.put("tyv", "тыва дыл"); // Tyvan
		code2language.put("tzm", "ⵜⴰⵎⴰⵣⵉⵖⵜ"); // Tamazight
		code2language.put("udm", "удмурт"); // Udmurt
		code2language.put("ug", "ئۇيغۇرچە / Uyghurche"); // Uyghur (multiple scripts - defaults to Arabic)
		code2language.put("ug-arab", "ئۇيغۇرچە"); // Uyghur (Arabic script) (default)
		code2language.put("ug-latn", "Uyghurche"); // Uyghur (Latin script)
		code2language.put("uk", "українська"); // Ukrainian
		code2language.put("ur", "اردو"); // Urdu
		code2language.put("uz", "oʻzbekcha / ўзбекча"); // Uzbek (multiple scripts - defaults to Latin)
		code2language.put("uz-cyrl", "ўзбекча"); // Uzbek Cyrillic
		code2language.put("uz-latn", "oʻzbekcha"); // Uzbek Latin (default)
		code2language.put("ve", "Tshivenda"); // Venda
		code2language.put("vec", "vèneto"); // Venetian
		code2language.put("vep", "vepsän kel’"); // Veps
		code2language.put("vi", "Tiếng Việt"); // Vietnamese
		code2language.put("vls", "West-Vlams"); // West Flemish
		code2language.put("vmf", "Mainfränkisch"); // Upper Franconian, Main-Franconian
		code2language.put("vmw", "emakhuwa"); // Makhuwa
		code2language.put("vo", "Volapük"); // Volapük
		code2language.put("vot", "Vaďďa"); // Vod/Votian
		code2language.put("vro", "võro"); // Võro
		code2language.put("wa", "walon"); // Walloon
		code2language.put("wal", "wolaytta"); // Wolaytta
		code2language.put("war", "Winaray"); // Waray-Waray
		code2language.put("wls", "Fakaʻuvea"); // Wallisian
		code2language.put("wo", "Wolof"); // Wolof
		code2language.put("wuu", "吴语"); // Wu (multiple scripts - defaults to Simplified Han)
		code2language.put("wuu-hans", "吴语（简体）"); // Wu (Simplified Han)
		code2language.put("wuu-hant", "吳語（正體）"); // Wu (Traditional Han)
		code2language.put("xal", "хальмг"); // Kalmyk-Oirat
		code2language.put("xh", "isiXhosa"); // Xhosan
		code2language.put("xmf", "მარგალური"); // Mingrelian
		code2language.put("xsy", "saisiyat"); // SaiSiyat - T216479
		code2language.put("yi", "ייִדיש"); // Yiddish
		code2language.put("yo", "Yorùbá"); // Yoruba
		code2language.put("yrl", "Nhẽẽgatú"); // Nheengatu
		code2language.put("yue", "粵語"); // Cantonese (multiple scripts - defaults to Traditional Han)
		code2language.put("yue-hans", "粵语（简体）"); // Cantonese (Simplified Han)
		code2language.put("yue-hant", "粵語（繁體）"); // Cantonese (Traditional Han)
		code2language.put("za", "Vahcuengh"); // Zhuang
		code2language.put("zea", "Zeêuws"); // Zeeuws / Zeaws
		code2language.put("zgh", "ⵜⴰⵎⴰⵣⵉⵖⵜ ⵜⴰⵏⴰⵡⴰⵢⵜ"); // Moroccan Amazigh (multiple scripts - defaults to Neo-Tifinagh)
		code2language.put("zh", "中文"); // (Zhōng Wén) - Chinese
		code2language.put("zh-classical", "文言"); // Classical Chinese/Literary Chinese -- (see T10217)
		code2language.put("zh-cn", "中文（中国大陆）"); // Chinese (PRC)
		code2language.put("zh-hans", "中文（简体）"); // Mandarin Chinese (Simplified Chinese script) (cmn-hans)
		code2language.put("zh-hant", "中文（繁體）"); // Mandarin Chinese (Traditional Chinese script) (cmn-hant)
		code2language.put("zh-hk", "中文（香港）"); // Chinese (Hong Kong)
		code2language.put("zh-min-nan", "Bân-lâm-gú"); // Min-nan -- (see T10217)
		code2language.put("zh-mo", "中文（澳門）"); // Chinese (Macau)
		code2language.put("zh-my", "中文（马来西亚）"); // Chinese (Malaysia)
		code2language.put("zh-sg", "中文（新加坡）"); // Chinese (Singapore)
		code2language.put("zh-tw", "中文（臺灣）"); // Chinese (Taiwan)
		code2language.put("zh-yue", "粵語"); // Cantonese -- (see T10217)
		code2language.put("zu", "isiZulu"); // Zulu
	}


}
