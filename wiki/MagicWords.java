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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;

import static wiki.NameSpaces.getNameSpaceNumber;
import static wiki.NameSpaces.getNameSpaceByNumber;
import static wiki.tools.Utilities.dateFormatter;
import static wiki.tools.Utilities.normaliseTitle;
import static wiki.tools.Utilities.encodeUrl;

/* references:
https://www.mediawiki.org/wiki/Help:Magic_word
https://www.mediawiki.org/wiki/Manual:Namespace#Built-in_namespaces
 */
public final class MagicWords {

    public enum MagicWord {
		_e_q_u_a_l_,
        _p_i_p_e_,
        anchorencode,
		articlepagename,
        articlepagenamee,
        articlespace,
        articlespacee,
        basepagename,
        basepagenamee,
        cascadingsources,
        contentlang,
        contentlanguage,
        currentday,
        currentday2,
        currentdayname,
        currentdayofweek,
        currenthour,
        currentmonth,
        currentmonthabbr,
        currentmonthname,
        currenttime,
        currenttimestamp,
        currentversion,
        currentweek,
        currentyear,
        defaultcategorysort,
        defaultsort,
        defaultsortkey,
        displaytitle,
        filepath,
        fullpagename,
        fullpagenamee,
		_int_,
		lc,
		lcfirst,
        localday,
        localday2,
        localdayname,
        localdayofweek,
        localhour,
        localmonth,
        localmonthabbr,
        localmonthname,
        localtime,
        localtimestamp,
        localweek,
        localyear,
        namespace,
        namespacee,
        namespacenumber,
		ns,
		nse,
        numberadmins,
        numberarticles,
        numberfiles,
        numberpages,
        numberusers,
        pageid,
        pagename,
        pagenamee,
        pagesincat,
        pagesincategory,
        pagesinnamespace,
        pagesinnamespacens,
        pagesize,
        protectionexpiry,
        protectionlevel,
        revisionday,
        revisionday2,
        revisionid,
        revisionmonth,
        revisionmonth1,
        revisiontimestamp,
        revisionuser,
        revisionyear,
		rootpagename,
		rootpagenamee,
        scriptpath,
        server,
        servername,
        sitename,
        stylepath,
        subjectpagename,
        subjectpagenamee,
        subjectspace,
        subjectspacee,
        subpagename,
        subpagenamee,
        talkpagename,
        talkpagenamee,
        talkspace,
        talkspacee,
		uc,
		ucfirst,
		urlencode
    }

	public static boolean contains(String name) {
		if (name.equals("!") || name.equals("="))
			return true;
		try {
			MagicWord.valueOf(name.toLowerCase());
	        return true;			
		} catch (IllegalArgumentException ex) {
	        return false;
		}
    }

    public static MagicWord get(String name) {
		if (name.equals("!"))
			return MagicWord._p_i_p_e_;
		if (name.equals("="))
			return MagicWord._e_q_u_a_l_;
		if (name.equals("int"))
			return MagicWord._int_;
		try {
	        return MagicWord.valueOf(name.toLowerCase());			
		} catch (IllegalArgumentException ex) {
	        return null;
		}
    }

    public static String evaluate(MagicWord magicWord, String parameter, String title, Date revision) {//note: revision is nullable
        switch (magicWord) {
            case anchorencode:
	            if (parameter != null) {
					return parameter.replace(' ', '_');
				} else return null;
            case articlepagename:
            case subjectpagename:
            case talkpagename:
                return getFullpagename(parameter, title);//dummy value
            case articlepagenamee:
            case subjectpagenamee:
            case talkpagenamee:
                return encodeUrl(normaliseTitle(getFullpagename(parameter, title), false));//dummy value
			case articlespace:
            case subjectspace:
            case talkspace:
                return "";//dummy value
            case articlespacee:
            case subjectspacee:
            case talkspacee:
                return "";//dummy value
            case basepagename:
                return getBasePageName(parameter, title);
            case basepagenamee:
                return encodeUrl(normaliseTitle(getBasePageName(parameter, title), false));
            case currentday:
            case localday:
				return dateFormatter(null, "d");
            case currentday2:
            case localday2:
				return dateFormatter(null, "dd");
            case currentdayname:
            case localdayname:
				return dateFormatter(null, "EEEE");
            case currentdayofweek:
            case localdayofweek:
				return dateFormatter(null, "F");
            case currentmonth:
            case localmonth:
				return dateFormatter(null, "MM");
            case currentmonthabbr:
            case localmonthabbr:
				return dateFormatter(null, "MMM");
            case currentmonthname:
            case localmonthname:
				return dateFormatter(null, "MMMM");
            case currenttime:
            case localtime:
				return dateFormatter(null, "HH:mm");
            case currenthour:
            case localhour:
				return dateFormatter(null, "HH");
            case currentweek:
            case localweek:
				return dateFormatter(null, "w");
            case localyear:
            case currentyear:
				return dateFormatter(null, "yyyy");
            case currenttimestamp:
            case localtimestamp:
				return dateFormatter(null, "yyyyMMddHHmmss");
            case fullpagename:
                return getFullpagename(parameter, title);
            case fullpagenamee:
                return encodeUrl(normaliseTitle(getFullpagename(parameter, title), false));
            case lc:
	            if (parameter != null)
					return parameter.toLowerCase();
				else return null;
            case lcfirst:
	            if (parameter != null) {
					return parameter.isEmpty() ? "" : Character.toLowerCase(parameter.charAt(0)) + parameter.substring(1);
				} else return null;
            case namespace:
                return getNamespace(getFullpagename(parameter, title));
            case namespacee:
                return encodeUrl(normaliseTitle(getNamespace(getFullpagename(parameter, title)), false));
            case namespacenumber:
				String namespace = getNamespace(getFullpagename(parameter, title));
				Integer nsn = getNameSpaceNumber(namespace);
				if (nsn != null)
					return Integer.toString(nsn);
				else return null;
			case ns:
				nsn = null;
				if (parameter != null)
					try {
						nsn = Integer.parseInt(parameter);
					} catch (NumberFormatException nfe) {
						nsn = getNameSpaceNumber(parameter);
					}
				if (nsn == null)
					return null;
				return getNameSpaceByNumber(nsn);
			case nse:
				nsn = null;
				if (parameter != null)
					try {
						nsn = Integer.parseInt(parameter);
					} catch (NumberFormatException nfe) {
						nsn = getNameSpaceNumber(parameter);
					}
				if (nsn == null)
					return null;
				return getNameSpaceByNumber(nsn).replace(' ', '_');
            case pagename:
                return getPagenameHelper(parameter, title);
            case pagenamee:
				return encodeUrl(normaliseTitle(getPagenameHelper(parameter, title), false));
            case revisionyear:
				return dateFormatter(revision, "yyyy");
            case revisionday:
				return dateFormatter(revision, "d");
            case revisionday2:
				return dateFormatter(revision, "dd");
            case revisionmonth:
				return dateFormatter(revision, "MM");
            case revisionmonth1:
				return dateFormatter(revision, "M");
            case revisiontimestamp:
				return dateFormatter(revision, "yyyyMMddHHmmss");
            case revisionuser:
                return "";
            case rootpagename:
                return getRootPageName(parameter, title);
            case rootpagenamee:
                return encodeUrl(normaliseTitle(getRootPageName(parameter, title), false));
            case subpagename:
                return getSubPageName(parameter, title);
            case subpagenamee:
                return encodeUrl(normaliseTitle(getSubPageName(parameter, title), false));
            case uc:
	            if (parameter != null)
					return parameter.toUpperCase();
				else return null;
            case ucfirst:
	            if (parameter != null) {
					return parameter.isEmpty() ? "" : Character.toUpperCase(parameter.charAt(0)) + parameter.substring(1);
				} else return null;
            case urlencode:
				if (parameter != null) {
					try	{
						return URLEncoder.encode(parameter, "UTF-8");
					} catch (IOException ex) {
//ignore
					}
				}
				return null;
            case _p_i_p_e_:
                return "|";
            case _e_q_u_a_l_:
                return "=";
            case _int_:
				return parameter;//todo: implement logic for intFunction as in CoreParserFunctions.php
            default:
                break;
        }

        return magicWord.name();
    }

    private static String getSubPageName(String parameter, String title) {
        String pagename = getPagenameHelper(parameter, title);
		int idx = pagename.lastIndexOf("/");
		if (idx != -1)
			return pagename.substring(idx + 1);
		else return pagename;
    }

    private static String getBasePageName(String parameter, String title) {
        String pagename = getPagenameHelper(parameter, title);
		int idx = pagename.lastIndexOf("/");
		if (idx != -1)
			return pagename.substring(0, idx);
		else return pagename;
    }

    private static String getRootPageName(String parameter, String title) {
        String pagename = getPagenameHelper(parameter, title);
		int idx = pagename.indexOf("/");
		if (idx != -1) {
			return pagename.substring(0, idx);
		} else return pagename;
    }

    private static String getFullpagename(String parameter, String title) {
		if (parameter != null && !parameter.isEmpty())
			return parameter;
		else return title;
    }

    private static String getPagenameHelper(String parameter, String title) {
        if (parameter != null && !parameter.isEmpty())
			title = parameter;
        int idx = title.indexOf(':');
        if (idx != -1) {
			return title.substring(idx + 1);
		} else return title;
    }

    private static String getNamespace(String title) {
        int idx = title.indexOf(':');
        if (idx != -1) {
            String namespace = normaliseTitle(title.substring(0, idx), true);
            if (namespace.length() > 0)
                return namespace;
        }
        return "";
    }

}