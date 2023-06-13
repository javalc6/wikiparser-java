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

import java.lang.RuntimeException;
/*
  This class is an helper for parsing wiki text
*/
final public class WikiScanner {
	protected final String str;//immutable
	protected int pointer;

	public WikiScanner(String str) {
		this.str = str;
		pointer = 0;
	}

	public Character getCharInCharSet(String charset) {//returns next char only if it is in charset
		if (pointer < str.length()) {
			char ch = str.charAt(pointer);
			if (charset.indexOf(ch) != -1) {
				pointer++;
				return ch;
			}
		}
		return null;
	}

	public boolean getChar(char ch) {//returns true only if sequence is found at current position; in this case pointer is moved forward
		if (pointer >= str.length() || str.charAt(pointer) != ch)
			return false;
		pointer++;
		return true;
	}

	public boolean getSequence(String sequence) {//returns true only if sequence is found at current position; in this case pointer is moved forward
		if (pointer + sequence.length() > str.length() || !str.startsWith(sequence, pointer))
			return false;
		pointer += sequence.length();
		return true;
	}

	public String getString() {//returns remaining substring
		if (pointer < str.length()) {
			String ret = str.substring(pointer);
			pointer = str.length();
			return ret;
		}
		return null;			
	}


	public String getSubstring(int end) {//returns substring upto <end> not included
		if (pointer < end) {
			String ret = str.substring(pointer, end);
			pointer = end;
			return ret;
		}
		return null;			
	}

	public String getStringWithoutOpening() {//returns substring before {{
		if (pointer < str.length()) {
			int idx = findTarget("{{", pointer);
			if (idx == -1)
				idx = str.length();
			if (idx > pointer) {
				String ret = str.substring(pointer, idx);
				pointer = idx;
				return ret;
			}
		}
		return null;			
	}

	private int findTarget(String target, int i) {
		int len = str.length();
		while (i < len) {
			if (str.startsWith(target, i))
				return i;
			if (str.startsWith("<nowiki>", i)) {
				int end = str.indexOf("</nowiki>", i + 8);
				if (end == -1)
					return -1;
				i = end + 9; // 9 == "</nowiki>".length()
				continue;
			}
			if (str.startsWith("<code>", i)) {
				int end = str.indexOf("</code>", i + 6);
				if (end == -1)
					return -1;
				i = end + 7; // 7 == "</code>".length()
				continue;
			}
			i++;
		}
		return -1;
	}

	public String getStringParameter(int[] equalPos) {//returns parameter, if any
		if (pointer < str.length()) {
			int i = pointer;
			int len = str.length();
			int noc = 2; //number of open curls
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
							if (noc == 0) {
								String ret = str.substring(pointer, i - 2);
								pointer = i - 2;
								return ret;
							}
							continue;
						}
						if (noc > 0) continue;
						break;
					case '[':
						if (++i < len && ((ch = str.charAt(i)) == '[')) {
							int end = findTarget("]]", i);
							if (end != -1)
								i = end + 2;// go after "]]"
						}
						continue;
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
						break;
					case '|':
						if (noc < 3) {
							String ret = str.substring(pointer, i);
							pointer = i;
							return ret;
						}
						break;
					case '=':
						if (noc < 3 && equalPos != null && equalPos[0] == -1) {
							equalPos[0] = i - pointer;
						}
						break;
				}
				i++;
			}
			return null;

		}
		return null;			
	}

	public String getIdentifierOrNumber() {//returns identifier or number, if present at current position
		Character ch = null;
		while ((pointer < str.length()) && isWikiSpace(ch = str.charAt(pointer++)))
			;
		if (ch != null) {
			StringBuilder sb = new StringBuilder().append(ch);
			while ((pointer < str.length()) && (Character.isLetter(ch = str.charAt(pointer)) || Character.isDigit(ch)) || ("/-_ ".indexOf(ch) != -1)) {
				sb.append(ch); pointer++;
			}
			if (sb.length() > 0)
				return sb.toString();
		}
		return null;
	}

	public boolean moveAfter(String marker) {//moves after marker, if present returns true, otherwise does not move and returns false
		int idx = str.indexOf(marker, pointer);
		if (idx != -1) {
			pointer = idx + marker.length();
			return true;
		} else return false;
	}

	public boolean chars_available() {
		return pointer < str.length();
	}

	public void dumpString(String tag) {//print remaining substring
		if (pointer < str.length()) {
			System.out.println("dumpString(" + tag + "): " + str.substring(pointer));
		} else System.out.println("dumpString(" + tag + "): reached end of string");
	}

	public void dumpFullString(String tag) {//print full string
		System.out.println("dumpString(" + tag + "): " + str);
	}

	public boolean isWikiSpace(char ch) {
		return Character.isSpaceChar(ch) || (ch == '\n') || (ch == '\t') || (ch == '\r');
	}

	public int getPointer() {
		return pointer;			
	}

	public void setPointer(int pointer) {
		if (pointer >= 0 && pointer <= str.length())//pointer = str.length() is valid and indicates "end of string"
			this.pointer = pointer;
		else throw new RuntimeException("invalid pointer value: " + pointer);
	}

}//end of class WikiScanner