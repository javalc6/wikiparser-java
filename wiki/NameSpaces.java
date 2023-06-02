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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
/* The class NameSpaces is an holder for Wikipedia namespaces
*/
final public class NameSpaces {

	public static class NameSpace {
		public final int id;
		private final String name;
		private final String canonicalName;
		private final boolean hasSubpages;
		private final boolean hasGenderDistinction;
		private final boolean isCapitalized;
		private final boolean isContent;
		private final boolean isIncludable;
		private final boolean isMovable;
		private final boolean isSubject;
		private final boolean isTalk;
		private final List<String> aliases;
		private final int subject;
		private final Integer talk;
		private final Integer associated;

		NameSpace(int id, String name, String canonicalName, boolean hasSubpages, boolean hasGenderDistinction, 
			boolean isCapitalized, boolean isContent, boolean isIncludable, boolean isMovable, boolean isSubject, 
			boolean isTalk, List<String> aliases, int subject, Integer talk, Integer associated) {
			this.id = id;
			this.name = name;
			this.canonicalName = canonicalName;
			this.aliases = aliases;
			this.hasSubpages = hasSubpages;
			this.hasGenderDistinction = hasGenderDistinction;
			this.isCapitalized = isCapitalized;
			this.isContent = isContent;
			this.isIncludable = isIncludable;
			this.isMovable = isMovable;
			this.isSubject = isSubject;
			this.isTalk = isTalk;
			this.subject = subject;
			this.talk = talk;
			this.associated = associated;
		}

		public int get_id() {
			return id;
		}

		public String get_name() {
			return name;
		}

		public String get_canonicalName() {
			return canonicalName;
		}

		public boolean get_hasSubpages() {
			return hasSubpages;
		}

		public boolean get_hasGenderDistinction() {
			return hasGenderDistinction;
		}

		public boolean get_isCapitalized() {
			return isCapitalized;
		}

		public boolean get_isContent() {
			return isContent;
		}

		public boolean get_isIncludable() {
			return isIncludable;
		}

		public boolean get_isMovable() {
			return isMovable;
		}

		public boolean get_isSubject() {
			return isSubject;
		}

		public boolean get_isTalk() {
			return isTalk;
		}

		public List<String> get_aliases() {
			return aliases;
		}

		public int get_subject() {
			return subject;
		}

		public Integer get_talk() {
			return talk;
		}

		public Integer get_associated() {
			return associated;
		}

	}

	private final static HashMap<Integer, NameSpace> namespaces = new HashMap<>();
	static {
		namespaces.put(-2, new NameSpace(-2, "Media", "Media", false, false, true, false, true, false, false, true, null, -2, null, null));
		namespaces.put(-1, new NameSpace(-1, "Special", "Special", false, false, true, false, true, false, false, true, null, -1, null, null));
		namespaces.put(0, new NameSpace(0, "", "", false, false, true, true, true, false, true, false, null, 0, 1, 1));
		namespaces.put(1, new NameSpace(1, "Talk", "Talk", true, false, true, false, true, false, false, true, null, 0, 1, 0));
		namespaces.put(2, new NameSpace(2, "User", "User", true, true, true, false, true, false, true, false, null, 2, 3, 3));
		namespaces.put(3, new NameSpace(3, "User talk", "User talk", true, true, true, false, true, false, false, true, null, 2, 3, 2));
		namespaces.put(4, new NameSpace(4, "Project", "Project", true, false, true, false, true, false, true, false, Arrays.asList("Meta", "WP", "Wiktionary"), 4, 5, 5));
		namespaces.put(5, new NameSpace(5, "Project talk", "Project talk", true, false, true, false, true, false, false, true, Arrays.asList("Meta_talk", "WT", "Wiktionary_talk"), 4, 5, 4));
		namespaces.put(6, new NameSpace(6, "File", "File", false, false, true, false, true, false, true, false, Collections.singletonList("Image"), 6, 7, 7));
		namespaces.put(7, new NameSpace(7, "File talk", "File talk", true, false, true, false, true, false, false, true, Collections.singletonList("Image_talk"), 6, 7, 6));
		namespaces.put(8, new NameSpace(8, "MediaWiki", "MediaWiki", true, false, true, false, true, false, true, false, null, 8, 9, 9));
		namespaces.put(9, new NameSpace(9, "MediaWiki talk", "MediaWiki talk", true, false, true, false, true, false, false, true, null, 8, 9, 8));
		namespaces.put(10, new NameSpace(10, "Template", "Template", false, false, true, false, true, false, true, false, Collections.singletonList("T"), 10, 11, 11));
		namespaces.put(11, new NameSpace(11, "Template talk", "Template talk", true, false, true, false, true, false, false, true, null, 10, 11, 10));
		namespaces.put(12, new NameSpace(12, "Help", "Help", true, false, true, false, true, false, true, false, null, 12, 13, 13));
		namespaces.put(13, new NameSpace(13, "Help talk", "Help talk", true, false, true, false, true, false, false, true, null, 12, 13, 12));
		namespaces.put(14, new NameSpace(14, "Category", "Category", false, false, true, false, true, false, true, false, Collections.singletonList("CAT"), 14, 15, 15));
		namespaces.put(15, new NameSpace(15, "Category talk", "Category talk", true, false, true, false, true, false, false, true, null, 14, 15, 14));

		namespaces.put(100, new NameSpace(100, "Portal", "Portal", false, false, true, false, true, false, true, false, Arrays.asList("AP", "Appendix"), 100, 101, 101));
		namespaces.put(101, new NameSpace(101, "Portal talk", "Portal talk", false, false, true, false, true, false, true, false, Collections.singletonList("Appendix_talk"), 100, 101, 100));

		namespaces.put(828, new NameSpace(10, "Module", "Module", false, false, true, false, true, false, true, false, Collections.singletonList("MOD"), 828, 829, 829));
		namespaces.put(829, new NameSpace(829, "Module talk", "Module talk", false, false, true, false, true, false, false, true, null, 828, 829, 828));
	}

	public static HashMap<Integer, NameSpace> getNameSpaces() {
		return namespaces;
	}

	public static boolean isNameSpace(String namespace) {
		return getNameSpaceNumber(namespace) != null;
	}

	public static Integer getNameSpaceNumber(String namespace) {
		namespace = normaliseNameSpace(namespace);
		for (Entry<Integer, NameSpace> entry : namespaces.entrySet()) {
			NameSpace ns = entry.getValue();
			if (ns.canonicalName.equals(namespace))
				return entry.getKey();
			else if (ns.name.equals(namespace))
				return entry.getKey();
			if (ns.aliases != null) {
				for (String alias: ns.aliases) {
					if (alias.equals(namespace))
						return entry.getKey();
				}
			}
		}
		return null;
	}

	public static String getNameSpaceByNumber(Integer numnamespace) {
		NameSpace ns = namespaces.get(numnamespace);
		if (ns != null)
			return namespaces.get(numnamespace).canonicalName;
		else return null;
	}

	public static String normaliseNameSpace(String name) {
		if (name == null || name.isEmpty()) return name;
		return (Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase());
	}

}
