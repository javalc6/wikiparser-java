/**
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package wiki.parserfunctions.expr;
import java.util.ArrayList;
import java.util.HashMap;

/* Wiki expression parser implemented in Java based on PHP source code: https://github.com/wikimedia/mediawiki-extensions-ParserFunctions/blob/master/includes/ExprParser.php
*/

final public class ExprParser {

	// Character classes
	final static String EXPR_WHITE_CLASS = " \t\r\n";
	final static String EXPR_NUMBER_CLASS = "0123456789.";

	// Token types
	final static int EXPR_WHITE = 1;
	final static int EXPR_NUMBER = 2;
	final static int EXPR_NEGATIVE = 3;
	final static int EXPR_POSITIVE = 4;
	final static int EXPR_PLUS = 5;
	final static int EXPR_MINUS = 6;
	final static int EXPR_TIMES = 7;
	final static int EXPR_DIVIDE = 8;
	final static int EXPR_MOD = 9;
	final static int EXPR_OPEN = 10;
	final static int EXPR_CLOSE = 11;
	final static int EXPR_AND = 12;
	final static int EXPR_OR = 13;
	final static int EXPR_NOT = 14;
	final static int EXPR_EQUALITY = 15;
	final static int EXPR_LESS = 16;
	final static int EXPR_GREATER = 17;
	final static int EXPR_LESSEQ = 18;
	final static int EXPR_GREATEREQ = 19;
	final static int EXPR_NOTEQ = 20;
	final static int EXPR_ROUND = 21;
	final static int EXPR_EXPONENT = 22;
	final static int EXPR_SINE = 23;
	final static int EXPR_COSINE = 24;
	final static int EXPR_TANGENS = 25;
	final static int EXPR_ARCSINE = 26;
	final static int EXPR_ARCCOS = 27;
	final static int EXPR_ARCTAN = 28;
	final static int EXPR_EXP = 29;
	final static int EXPR_LN = 30;
	final static int EXPR_ABS = 31;
	final static int EXPR_FLOOR = 32;
	final static int EXPR_TRUNC = 33;
	final static int EXPR_CEIL = 34;
	final static int EXPR_POW = 35;
	final static int EXPR_PI = 36;
	final static int EXPR_FMOD = 37;
	final static int EXPR_SQRT = 38;

	final static int MAX_STACK_SIZE = 100;

	final static HashMap<Integer, Integer> PRECEDENCE = new HashMap<>();
	final static HashMap<Integer, String> NAMES = new HashMap<>();
	final static HashMap<String, Integer> WORDS = new HashMap<>();

	static {
		PRECEDENCE.put(EXPR_NEGATIVE, 10);
		PRECEDENCE.put(EXPR_POSITIVE, 10);
		PRECEDENCE.put(EXPR_EXPONENT, 10);
		PRECEDENCE.put(EXPR_SINE, 9);
		PRECEDENCE.put(EXPR_COSINE, 9);
		PRECEDENCE.put(EXPR_TANGENS, 9);
		PRECEDENCE.put(EXPR_ARCSINE, 9);
		PRECEDENCE.put(EXPR_ARCCOS, 9);
		PRECEDENCE.put(EXPR_ARCTAN, 9);
		PRECEDENCE.put(EXPR_EXP, 9);
		PRECEDENCE.put(EXPR_LN, 9);
		PRECEDENCE.put(EXPR_ABS, 9);
		PRECEDENCE.put(EXPR_FLOOR, 9);
		PRECEDENCE.put(EXPR_TRUNC, 9);
		PRECEDENCE.put(EXPR_CEIL, 9);
		PRECEDENCE.put(EXPR_NOT, 9);
		PRECEDENCE.put(EXPR_SQRT, 9);
		PRECEDENCE.put(EXPR_POW, 8);
		PRECEDENCE.put(EXPR_TIMES, 7);
		PRECEDENCE.put(EXPR_DIVIDE, 7);
		PRECEDENCE.put(EXPR_MOD, 7);
		PRECEDENCE.put(EXPR_FMOD, 7);
		PRECEDENCE.put(EXPR_PLUS, 6);
		PRECEDENCE.put(EXPR_MINUS, 6);
		PRECEDENCE.put(EXPR_ROUND, 5);
		PRECEDENCE.put(EXPR_EQUALITY, 4);
		PRECEDENCE.put(EXPR_LESS, 4);
		PRECEDENCE.put(EXPR_GREATER, 4);
		PRECEDENCE.put(EXPR_LESSEQ, 4);
		PRECEDENCE.put(EXPR_GREATEREQ, 4);
		PRECEDENCE.put(EXPR_NOTEQ, 4);
		PRECEDENCE.put(EXPR_AND, 3);
		PRECEDENCE.put(EXPR_OR, 2);
		PRECEDENCE.put(EXPR_PI, 0);
		PRECEDENCE.put(EXPR_OPEN, -1);
		PRECEDENCE.put(EXPR_CLOSE, -1);

		NAMES.put(EXPR_NEGATIVE, "-");
		NAMES.put(EXPR_POSITIVE, "+");
		NAMES.put(EXPR_NOT, "not");
		NAMES.put(EXPR_TIMES, "*");
		NAMES.put(EXPR_DIVIDE, "/");
		NAMES.put(EXPR_MOD, "mod");
		NAMES.put(EXPR_FMOD, "fmod");
		NAMES.put(EXPR_PLUS, "+");
		NAMES.put(EXPR_MINUS, "-");
		NAMES.put(EXPR_ROUND, "round");
		NAMES.put(EXPR_EQUALITY, "=");
		NAMES.put(EXPR_LESS, "<");
		NAMES.put(EXPR_GREATER, ">");
		NAMES.put(EXPR_LESSEQ, "<=");
		NAMES.put(EXPR_GREATEREQ, ">=");
		NAMES.put(EXPR_NOTEQ, "<>");
		NAMES.put(EXPR_AND, "and");
		NAMES.put(EXPR_OR, "or");
		NAMES.put(EXPR_EXPONENT, "e");
		NAMES.put(EXPR_SINE, "sin");
		NAMES.put(EXPR_COSINE, "cos");
		NAMES.put(EXPR_TANGENS, "tan");
		NAMES.put(EXPR_ARCSINE, "asin");
		NAMES.put(EXPR_ARCCOS, "acos");
		NAMES.put(EXPR_ARCTAN, "atan");
		NAMES.put(EXPR_LN, "ln");
		NAMES.put(EXPR_EXP, "exp");
		NAMES.put(EXPR_ABS, "abs");
		NAMES.put(EXPR_FLOOR, "floor");
		NAMES.put(EXPR_TRUNC, "trunc");
		NAMES.put(EXPR_CEIL, "ceil");
		NAMES.put(EXPR_POW, "^");
		NAMES.put(EXPR_PI, "pi");
		NAMES.put(EXPR_SQRT, "sqrt");

		WORDS.put("mod", EXPR_MOD);
		WORDS.put("fmod", EXPR_FMOD);
		WORDS.put("and", EXPR_AND);
		WORDS.put("or", EXPR_OR);
		WORDS.put("not", EXPR_NOT);
		WORDS.put("round", EXPR_ROUND);
		WORDS.put("div", EXPR_DIVIDE);
		WORDS.put("e", EXPR_EXPONENT);
		WORDS.put("sin", EXPR_SINE);
		WORDS.put("cos", EXPR_COSINE);
		WORDS.put("tan", EXPR_TANGENS);
		WORDS.put("asin", EXPR_ARCSINE);
		WORDS.put("acos", EXPR_ARCCOS);
		WORDS.put("atan", EXPR_ARCTAN);
		WORDS.put("exp", EXPR_EXP);
		WORDS.put("ln", EXPR_LN);
		WORDS.put("abs", EXPR_ABS);
		WORDS.put("trunc", EXPR_TRUNC);
		WORDS.put("floor", EXPR_FLOOR);
		WORDS.put("ceil", EXPR_CEIL);
		WORDS.put("pi", EXPR_PI);
		WORDS.put("sqrt", EXPR_SQRT);
	}

	/**
	 * Evaluate a mathematical expression
	 *
	 * The algorithm here is based on the infix to RPN algorithm given in
	 * http://montcs.bloomu.edu/~bobmon/Information/RPN/infix2rpn.shtml
	 * It"s essentially the same as Dijkstra"s shunting yard algorithm.
	 * @param string expr
	 * @return string
	 * @throws RuntimeException
	 */
	public String doExpression( String expr ) {
		ArrayList<Double> operands = new ArrayList<>();
		ArrayList<Integer> operators = new ArrayList<>();

		// Unescape inequality operators
		expr = expr.replace("&lt;", "<").replace("&gt;", ">").replace("&minus;", "-").replace("−", "-");

		int p = 0;
		int end = expr.length();
		String expecting = "expression";
		String name = "";

		Integer op = null;

		while ( p < end ) {
			if ( operands.size() > MAX_STACK_SIZE || operators.size() > MAX_STACK_SIZE ) {
				throw new RuntimeException( "stack_exhausted" );
			}
			char ch = expr.charAt(p);
			String char2;
			if (p < expr.length() - 1)
				char2 = expr.substring(p, p + 2);
			else char2 = expr.substring(p, p + 1);
			// Mega if-else if-else construct
			// Only binary operators fall through for processing at the bottom, the rest
			// finish their processing and continue

			// First the unlimited length classes

			// @phan-suppress-next-line PhanParamSuspiciousOrder false positive
			if ( EXPR_WHITE_CLASS.indexOf(ch) != -1 ) {
				// Whitespace
				p += strspn( expr, EXPR_WHITE_CLASS, p );
				continue;
				// @phan-suppress-next-line PhanParamSuspiciousOrder false positive
			} else if ( EXPR_NUMBER_CLASS.indexOf(ch) != -1 ) {
				// Number
				if ( !expecting.equals("expression") ) {
					throw new RuntimeException( "unexpected_number" );
				}

				// Find the rest of it
				int length = strspn( expr, EXPR_NUMBER_CLASS, p );
				// Convert it to float, silently removing double decimal points
				operands.add(Double.parseDouble(expr.substring(p, p + length )));
				p += length;
				expecting = "operator";
				continue;
			} else if ( Character.isAlphabetic( ch ) ) {
				// Word
				// Find the rest of it
				StringBuilder sb = new StringBuilder();
				sb.append(ch);
				while ((++p < expr.length()) && Character.isAlphabetic( ch = expr.charAt(p) )) {
					sb.append(ch);
				}

				String word = sb.toString().toLowerCase();

				// Interpret the word
				op = WORDS.get(word);
				if ( op == null ) {
					throw new RuntimeException( "unrecognised_word: " + word );
				}
				
				switch ( op ) {
					// constant
					case EXPR_EXPONENT:
						if ( !expecting.equals("expression") ) {
							break;
						}
						operands.add(Math.E);
						expecting = "operator";
						continue;
					case EXPR_PI:
						if ( !expecting.equals("expression") ) {
							throw new RuntimeException( "unexpected_number" );
						}
						operands.add(Math.PI);
						expecting = "operator";
						continue;
					// Unary operator
					case EXPR_NOT:
					case EXPR_SINE:
					case EXPR_COSINE:
					case EXPR_TANGENS:
					case EXPR_ARCSINE:
					case EXPR_ARCCOS:
					case EXPR_ARCTAN:
					case EXPR_EXP:
					case EXPR_LN:
					case EXPR_ABS:
					case EXPR_FLOOR:
					case EXPR_TRUNC:
					case EXPR_CEIL:
					case EXPR_SQRT:
						if ( !expecting.equals("expression") ) {
							throw new RuntimeException( "unexpected_operator: " + word );
						}
						operators.add(op);
						continue;
				}
				// Binary operator, fall through
				name = word;
			} else if ( char2.equals("<=") ) {
				name = char2;
				op = EXPR_LESSEQ;
				p += 2;
			} else if ( char2.equals(">=") ) {
				name = char2;
				op = EXPR_GREATEREQ;
				p += 2;
			} else if ( char2.equals("<>") || char2.equals("!=") ) {
				name = char2;
				op = EXPR_NOTEQ;
				p += 2;
			} else if ( ch == '+' ) {
				++p;
				if ( expecting.equals("expression") ) {
					// Unary plus
					operators.add(EXPR_POSITIVE);
					continue;
				} else {
					// Binary plus
					op = EXPR_PLUS;
				}
			} else if ( ch == '-' ) {
				++p;
				if ( expecting.equals("expression") ) {
					// Unary minus
					operators.add(EXPR_NEGATIVE);
					continue;
				} else {
					// Binary minus
					op = EXPR_MINUS;
				}
			} else if ( ch == '*' ) {
				name = "" + ch;
				op = EXPR_TIMES;
				++p;
			} else if ( ch == '/' ) {
				name = "" + ch;
				op = EXPR_DIVIDE;
				++p;
			} else if ( ch == '^' ) {
				name = "" + ch;
				op = EXPR_POW;
				++p;
			} else if ( ch == '(' ) {
				if ( expecting.equals("operator") ) {
					throw new RuntimeException( "unexpected_operator: (" );
				}
				operators.add(EXPR_OPEN);
				++p;
				continue;
			} else if ( ch == ')' ) {
				Integer lastOp = end( operators );
				while ( lastOp != null && !lastOp.equals(EXPR_OPEN) ) {
					doOperation( lastOp, operands );
					array_pop( operators );
					lastOp = end( operators );
				}
				if ( lastOp != null ) {
					array_pop( operators );
				} else {
					throw new RuntimeException( "unexpected_closing_bracket" );
				}
				expecting = "operator";
				++p;
				continue;
			} else if ( ch == '=' ) {
				name = "" + ch;
				op = EXPR_EQUALITY;
				++p;
			} else if ( ch == '<' ) {
				name = "" + ch;
				op = EXPR_LESS;
				++p;
			} else if ( ch == '>' ) {
				name = "" + ch;
				op = EXPR_GREATER;
				++p;
			} else {
				throw new RuntimeException( "unrecognised_punctuation: " + expr.substring(p) );
			}

			// Binary operator processing
			if ( expecting.equals("expression") ) {
				throw new RuntimeException( "unexpected_operator: " + name );
			}

			// Shunting yard magic
			Integer lastOp = end( operators );
			while ( (lastOp != null) && PRECEDENCE.get(op) <= PRECEDENCE.get(lastOp) ) {
				doOperation( lastOp, operands );
				array_pop( operators );
				lastOp = end( operators );
			}
			operators.add(op);
			expecting = "expression";
		}

		// Finish off the operator array
		// phpcs:ignore MediaWiki.ControlStructures.AssignmentInControlStructures.AssignmentInControlStructures
		while ( (op = array_pop( operators ) ) != null) {
			if ( op.equals(EXPR_OPEN) ) {
				throw new RuntimeException( "unclosed_bracket" );
			}
			doOperation( op, operands );
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Double val: operands) {
			if (!first)
				sb.append("<br />\n");
			else first = false;
			sb.append(getWikiNumberFormat(val));
		}

		return sb.toString();
	}

	private void doOperation( int op, ArrayList<Double> stack ) {
		switch ( op ) {
			case EXPR_NEGATIVE:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				Double arg = array_pop( stack );
				stack.add(-arg);
				break;
			case EXPR_POSITIVE:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				break;
			case EXPR_TIMES:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				Double right = array_pop( stack );
				Double left = array_pop( stack );
				stack.add(left * right);
				break;
			case EXPR_DIVIDE:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				if (Math.abs(right) < EPSILON) {
					throw new RuntimeException( "division_by_zero: " + NAMES.get(op) );
				}
				stack.add(left / right);
				break;
			case EXPR_MOD:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				int iright = array_pop( stack ).intValue();
				int ileft = array_pop( stack ).intValue();
				if ( iright == 0 ) {
					throw new RuntimeException( "division_by_zero: " + NAMES.get(op) );
				}
				stack.add((double) (ileft % iright));
				break;
			case EXPR_FMOD:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = (double)array_pop( stack );
				left = (double)array_pop( stack );
				if (Math.abs(right) < EPSILON) {
					throw new RuntimeException( "division_by_zero: " + NAMES.get(op) );
				}
				double div = left / right;
				stack.add(div - (int) div);
				break;
			case EXPR_PLUS:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(left + right);
				break;
			case EXPR_MINUS:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(left - right);
				break;
			case EXPR_AND:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(((left != 0) && (right != 0)) ? 1.0 : 0.0);
				break;
			case EXPR_OR:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(((left != 0) || (right != 0)) ? 1.0 : 0.0);
				break;
			case EXPR_EQUALITY:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add((Math.abs(left - right) < EPSILON) ? 1.0 : 0.0);
				break;
			case EXPR_NOT:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add((Math.abs(arg) < EPSILON) ? 1.0 : 0.0);
				break;
			case EXPR_ROUND:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				int digits = array_pop( stack ).intValue();
				Double value = array_pop( stack );
				double pow10 = Math.pow(10, digits);
				stack.add(Math.round(value * pow10) / pow10);
				break;
			case EXPR_LESS:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(( left < right ) ? 1.0 : 0.0);
				break;
			case EXPR_GREATER:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(( left > right ) ? 1.0 : 0.0);
				break;
			case EXPR_LESSEQ:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(( left <= right ) ? 1.0 : 0.0);
				break;
			case EXPR_GREATEREQ:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(( left >= right ) ? 1.0 : 0.0);
				break;
			case EXPR_NOTEQ:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(( Math.abs(left - right) > EPSILON ) ? 1.0 : 0.0);
				break;
			case EXPR_EXPONENT:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(left * Math.pow( 10, right));
				break;
			case EXPR_SINE:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add(Math.sin( arg ));
				break;
			case EXPR_COSINE:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add(Math.cos( arg ));
				break;
			case EXPR_TANGENS:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add(Math.tan( arg ));
				break;
			case EXPR_ARCSINE:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				if ( arg < -1 || arg > 1 ) {
					throw new RuntimeException( "invalid_argument: " + NAMES.get(op) );
				}
				stack.add(Math.asin( arg ));
				break;
			case EXPR_ARCCOS:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				if ( arg < -1 || arg > 1 ) {
					throw new RuntimeException( "invalid_argument: " + NAMES.get(op) );
				}
				stack.add(Math.acos( arg ));
				break;
			case EXPR_ARCTAN:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add(Math.atan( arg ));
				break;
			case EXPR_EXP:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add(Math.exp( arg ));
				break;
			case EXPR_LN:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				if ( arg <= 0 ) {
					throw new RuntimeException( "invalid_argument_ln: " + NAMES.get(op) );
				}
				stack.add(Math.log( arg ));
				break;
			case EXPR_ABS:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add(Math.abs( arg ));
				break;
			case EXPR_FLOOR:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add(Math.floor( arg ));
				break;
			case EXPR_TRUNC:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add((double) arg.intValue());
				break;
			case EXPR_CEIL:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				stack.add(Math.ceil( arg ));
				break;
			case EXPR_POW:
				if ( stack.size() < 2 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				right = array_pop( stack );
				left = array_pop( stack );
				stack.add(Math.pow( left, right ));
				break;
			case EXPR_SQRT:
				if ( stack.size() < 1 ) {
					throw new RuntimeException( "missing_operand: " + NAMES.get(op) );
				}
				arg = array_pop( stack );
				if ( arg < 0 ) {
					throw new RuntimeException( "not_a_number: " + NAMES.get(op) );
				}
				Double result = Math.sqrt( arg );
				stack.add(result);
				break;
			default:
				// Should be impossible to reach here.
				// @codeCoverageIgnoreStart
				throw new RuntimeException( "unknown_error" );
				// @codeCoverageIgnoreEnd
		}
	}

    public static final double EPSILON = 1.0e-17d;
	public static String getWikiNumberFormat(double d) {
        double dInt = Math.rint(d);
        if (Math.abs(dInt - d) < EPSILON) {
            return Long.toString(Math.round(d));
        }
        return Double.toString(d).toUpperCase();
    }
/*
	public static void main(String[] args) {
		ExprParser ep = new ExprParser();
		System.out.println(ep.doExpression("4 + cos1 -pi +(cos2/pi+trunc sin1)"));
	}
*/
	private static <T> T end(ArrayList<T> stack) {//php function end
		if (stack.size() == 0)
			return null;
		return stack.get(stack.size() - 1);
	}

	private static <T> T array_pop(ArrayList<T> stack) {//php function array_pop
		if (stack.size() == 0)
			return null;
		T last = stack.get(stack.size() - 1);
		stack.remove(stack.size() - 1);
		return last;
	}

	private static int strspn(String string, String characters, int offset ) {//php function strspn
		for (int i = offset; i < string.length() ; i++) {
			if (characters.indexOf(string.charAt(i)) == -1)
				return i - offset;
		}
		return string.length() - offset;
	}

	private static Double array_pop_as_Double(ArrayList<String> stack) {
		if (stack.size() == 0)
			return null;
		String last = stack.get(stack.size() - 1);
		stack.remove(stack.size() - 1);
		return Double.parseDouble(last);
	}
}