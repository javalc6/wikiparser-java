package info.bliki.wiki.template.dates;
/*
code cloned with minor changes from https://github.com/axkr/info.bliki.wikipedia_parser/tree/master/bliki-core/src/main/java/info/bliki/wiki/template/dates/StringToTime.java
*/
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A Java implementation of the PHP function strtotime(String, int): accepts
 * various expressions of time in <code>String</code> format, and instantiates a
 * {@link Date} object.
 *
 * <p>
 * There are two ways to use <code>StringToTime</code> to parse dates:
 * <ul>
 * <li>static methods ({@link #time(Object)}, {@link #date(Object)},
 * {@link #cal(Object)})</li>
 * <li>creating an instance of <code>StringToTime</code> with
 * {@link #StringToTime(Object)}</li>
 * </ul>
 * </p>
 *
 * <p>
 * The static methods provide a UNIX-style timestamp, a {@link Date}
 * instance, or a {@link Calendar} instance. In the event the time
 * expression provided is invalid, these methods return
 * <code>Boolean.FALSE</code>.
 * </p>
 *
 * <p>
 * Instances of <code>StringToTime</code> inherit from {@link Date};
 * so, when instantiated with an expression that the algorithm recognizes, the
 * resulting instance of <code>StringToTime</code> can be passed to any method
 * or caller requiring a {@link Date} object. Unlike the static
 * methods, attempting to create a <code>StringToTime</code> instance with an
 * invalid expression of time results in a {@link StringToTimeException}.
 * </p>
 *
 * <h2>Valid expressions of time</h2>
 *
 * <p>
 * All expressions are case-insensitive.
 * </p>
 *
 * <ul>
 * <li><code>now</code> (equal to <code>new Date()</code>)</li>
 * <li><code>today</code> (equal to <code>StringToTime("00:00:00.000")</code>)</li>
 * <li><code>midnight</code> (equal to
 * <code>StringToTime("00:00:00.000 +24 hours")</code>)</li>
 * <li><code>morning</code> or <code>this morning</code> (by default, equal to
 * <code>StringToTime("07:00:00.000")</code>)</li>
 * <li><code>noon</code> (by default, equal to
 * <code>StringToTime("12:00:00.000")</code></li>
 * <li><code>afternoon</code> or <code>this afternoon</code> (by default, equal
 * to <code>StringToTime("13:00:00.000")</code></li>
 * <li><code>evening</code> or <code>this evening</code> (by default, equal to
 * <code>StringToTime("17:00:00.000")</code></li>
 * <li><code>tonight</code> (by default, equal to
 * <code>StringToTime("20:00:00.000")</code></li>
 * <li><code>tomorrow</code> (by default, equal to
 * <code>StringToTime("now +24 hours")</code>)</li>
 * <li><code>tomorrow morning</code> (by default, equal to
 * <code>StringToTime("morning +24 hours")</code>)</li>
 * <li><code>noon tomorrow</code> or <code>tomorrow noon</code> (by default,
 * equal to <code>StringToTime("noon +24 hours")</code>)</li>
 * <li><code>tomorrow afternoon</code> (by default, equal to
 * <code>StringToTime("afternoon +24 hours")</code>)</li>
 * <li><code>yesterday</code> (by default, equal to
 * <code>StringToTime("now -24 hours")</code>)</li>
 * <li>all the permutations of <code>yesterday</code> and <code>morning</code>,
 * <code>noon</code>, <code>afternoon</code>, and <code>evening</code></li>
 * <li><code>October 26, 1981</code> or <code>Oct 26, 1981</code></li>
 * <li><code>October 26</code> or <code>Oct 26</code></li>
 * <li><code>26 October 1981</code></li>
 * <li><code>26 Oct 1981</code></li>
 * <li><code>26 Oct 81</code></li>
 * <li><code>10/26/1981</code> or <code>10-26-1981</code></li>
 * <li><code>10/26/81</code> or <code>10-26-81</code></li>
 * <li><code>1981/10/26</code> or <code>1981-10-26</code></li>
 * <li><code>10/26</code> or <code>10-26</code></li>
 * </ul>
 *
 * <p>
 * Copied from <a href="https://github.com/collegeman/stringtotime/blob/master/src/main/java/com/clutch/dates/StringToTime.java"
 * >github.com/collegeman/stringtotime</a >
 * </p>
 *
 * @author Aaron Collegeman acollegeman@clutch-inc.com
 * @since JRE 1.5.0
 * @see "https://us3.php.net/manual/en/function.strtotime.php"
 */
public final class StringToTime extends Date {

    private static final long serialVersionUID = 7889493424407815134L;

    // default SimpleDateFormat string is the standard MySQL date format
    private static final String defaultSimpleDateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    // An expression of time (hour)(:(minute))?((:(second))(.(millisecond))?)?(
    // *(am?|pm?))?(RFC 822 time zone|general time zone)?
    private static final String timeExpr = "(\\d{1,2})(:(\\d{1,2}))?(:(\\d{1,2})(\\.(\\d{1,3}))?)?( *(am?|pm?))?( *\\-\\d{4})?";

    /**
     * Patterns and formats recognized by the algorithm; first match wins, so
     * insert most specific patterns first.
     */
    private static final PatternAndFormat[] known = {

    // TODO: ISO 8601 and derivatives

            // just the year
            new PatternAndFormat(Pattern.compile("\\d{4}"), new Format(FormatType.YEAR)),

            // decrement, e.g., -1 day
            new PatternAndFormat(Pattern.compile("-( *\\d+ +[^ ]+)+", Pattern.CASE_INSENSITIVE), new Format(FormatType.DECREMENT)),

            // increment, e.g., +1 day
			new PatternAndFormat(
				Pattern.compile("\\+( *\\d+ +[^ ]+)+", Pattern.CASE_INSENSITIVE),//patch x bliki, removed ? that caused problems parsing "31 July 1711 +11 days"
				new Format(FormatType.INCREMENT)
			),


            // e.g., October 26 and Oct 26
            new PatternAndFormat(Pattern.compile("([a-z]+) +(\\d{1,2})", Pattern.CASE_INSENSITIVE), new Format(FormatType.MONTH_AND_DATE)),

            // e.g., 26 October 1981, or 26 Oct 1981, or 26 Oct 81, or 26. Oct 81
            new PatternAndFormat(Pattern.compile("(\\d{1,2})(?:\\.?)( +[a-z]+ +)(\\d{2}|\\d{4})", Pattern.CASE_INSENSITIVE), new Format("d MMM y")),

            // e.g., October 1981
            new PatternAndFormat(Pattern.compile("[a-z]+ +\\d{4}", Pattern.CASE_INSENSITIVE), new Format("MMM yyyy")),

            // e.g., 1981 October
            new PatternAndFormat(Pattern.compile("\\d{4} +[a-z]+", Pattern.CASE_INSENSITIVE), new Format("yyyy MMM")),//02-10-2024: added parsing of yyyy MMM

            // now or today
            new PatternAndFormat(
                    Pattern
                            .compile(
                                    "(midnight|now|today|(this +)?(morning|afternoon|evening)|tonight|noon( +tomorrow)?|tomorrow|tomorrow +(morning|afternoon|evening|night|noon)?|yesterday|yesterday +(morning|afternoon|evening|night)?)",
                                    Pattern.CASE_INSENSITIVE), new Format(FormatType.WORD)),

            // time, 24-hour and 12-hour
            new PatternAndFormat(Pattern.compile(timeExpr, Pattern.CASE_INSENSITIVE), new Format(FormatType.TIME)),

            // e.g., 1997, March 14
            new PatternAndFormat(Pattern.compile("(\\d{2}|\\d{4})( *, *[a-z]+ +\\d{1,2})", Pattern.CASE_INSENSITIVE),
                    new Format("y, MMM d")),

			// e.g., October 26, 1981 or Oct 26, 1981, or Oct. 26, 1981
            new PatternAndFormat(Pattern.compile("([a-z]+)(?:\\.?)( +\\d{1,2} *, *)(\\d{2}|\\d{4})", Pattern.CASE_INSENSITIVE),
                    new Format("MMM d,y")),//02-10-2024: updated to support parsing of October 26,1981

            // e.g., October 26 1981 or Oct 26 1981, or Oct 26th 1981
            new PatternAndFormat(Pattern.compile("([a-z]+ +\\d{1,2})(?:st|nd|rd|th)?( +)(\\d{2}|\\d{4})", Pattern.CASE_INSENSITIVE),
                    new Format("MMM d y")),

            // e.g., 10/26/1981 or 10/26/81
            new PatternAndFormat(Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{2,4}"), new Format("M/d/y")),

            // e.g., 10-26-1981 or 10-26-81
            new PatternAndFormat(Pattern.compile("\\d{1,2}-\\d{1,2}-\\d{2,4}"), new Format("M-d-y")),

            // e.g., 10/26 or 10-26
            new PatternAndFormat(Pattern.compile("(\\d{1,2})([/\\-])(\\d{1,2})"), new Format(FormatType.MONTH_AND_DATE_WITH_SLASHES)),

            // e.g., 1981/10/26
            new PatternAndFormat(Pattern.compile("\\d{4}/\\d{1,2}/\\d{1,2}"), new Format("y/M/d")),

            // e.g., 19811026
            new PatternAndFormat(Pattern.compile("\\d{4}\\d{2}\\d{2}"), new Format("yyyyMMdd")),

            // e.g., 1981-10-26
            new PatternAndFormat(Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}"), new Format("y-M-d")),

            // e.g., 1981-10
            new PatternAndFormat(Pattern.compile("\\d{4}-\\d{2}"), new Format("yyyy-MM")),

            // e.g., 26.10.1981
            new PatternAndFormat(Pattern.compile("\\d{1,2}\\.\\d{1,2}\\.\\d{4}"), new Format("d.M.y")),

            // e.g., October or Oct
            new PatternAndFormat(
                    Pattern
                            .compile(
                                    "(Jan(uary)?|Feb(ruary)?|Mar(ch)?|Apr(il)?|May|Jun(e)?|Jul(y)?|Aug(ust)?|Sep(tember)?|Oct(ober)?|Nov(ember)?|Dec(ember)?)",
                                    Pattern.CASE_INSENSITIVE), new Format(FormatType.MONTH)),

            // e.g., Tuesday or Tue
            new PatternAndFormat(Pattern.compile("(Sun(day)?|Mon(day)?|Tue(sday)?|Wed(nesday)?|Thu(rsday)?|Fri(day)?|Sat(urday)?)",
                    Pattern.CASE_INSENSITIVE), new Format(FormatType.DAY_OF_WEEK)),

            // next, e.g., next Tuesday
            new PatternAndFormat(Pattern.compile("next +(.*)", Pattern.CASE_INSENSITIVE), new Format(FormatType.NEXT)),

            // last, e.g., last Tuesday
            new PatternAndFormat(Pattern.compile("last +(.*)", Pattern.CASE_INSENSITIVE), new Format(FormatType.LAST)),

			// compound statement
			new PatternAndFormat(
				Pattern.compile("(.*) *((([+\\-]).*)| "+timeExpr+")$", Pattern.CASE_INSENSITIVE),
				new Format(FormatType.COMPOUND)
			)


    };

    /** The format to use in {@link #toString()} */
    private final String simpleDateFormat;

    public StringToTime(Object dateTimeString) {
        this(dateTimeString, new Date(), defaultSimpleDateFormat);
    }

    private StringToTime(Object dateTimeString, Date now) {
        this(dateTimeString, now, defaultSimpleDateFormat);
    }

    private StringToTime(Object dateTimeString, Date now, String simpleDateFormat) {
        super(0);
        assert dateTimeString != null;
        assert now != null;
        assert simpleDateFormat != null;

        /** Date/Time string parsed */
        this.simpleDateFormat = simpleDateFormat;

        /*
      The {@link Date} interpreted from {@link #dateTimeString}, or
      {@link Boolean} <code>false</code>
     */ /**
         * The {@link Date} interpreted from {@link #dateTimeString}, or
         * {@link Boolean} <code>false</code>
         */Object date = StringToTime.date(dateTimeString, now);
        if (!Boolean.FALSE.equals(date))
            setTime(((Date) date).getTime());
        else {
            throw new StringToTimeException(dateTimeString);
		}
    }

    /**
     * @return Calendar set to timestamp {@link Date#getTime()}
     */
    public Calendar getCal() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(super.getTime());
        return cal;
    }

    /**
     * @param simpleDateFormat
     * @see SimpleDateFormat
     * @return Date formatted according to <code>simpleDateFormat</code>
     */
    public String format(String simpleDateFormat) {
        return new SimpleDateFormat(simpleDateFormat, DEFAULT_LOCALE).format(this);
    }

    /**
     * @return If {@link #simpleDateFormat} provided in constructor, then attempts
     *         to format <code>date</code> accordingly; otherwise, returns the
     *         <code>String</code> value of {@link Date#getTime()}.
     */
    @Override
    public String toString() {
        if (simpleDateFormat != null)
            return new SimpleDateFormat(simpleDateFormat, DEFAULT_LOCALE).format(this);
        else
            return new SimpleDateFormat("yyyy/dd/MM", DEFAULT_LOCALE).format(this); // String.valueOf(super.getTime());
    }

    /**
     * A single parameter version of {@link #time(Object, Date)}, passing a new
     * instance of {@link Date} as the second parameter.
     *
     * @param dateTimeString
     * @return A {@link Long} timestamp representative of
     *         <code>dateTimeString</code>, or {@link Boolean}
     *         <code>false</code>.
     * @see #time(Object, Date)
     */
    public static Object time(Object dateTimeString) {
        return time(dateTimeString, new Date());
    }

    /**
     * Parse <code>dateTimeString</code> and produce a timestamp.
     *
     * @param dateTimeString
     * @param now
     * @return <ul>
     *         <li>If equal to &quot;now&quot;, return the number of milliseconds
     *         since January 1, 1970 or the value of <code>now</code>.</li>
     *         <li>If an incremental or decremental statement, e.g., +1 hour or -1
     *         week, or a composite thereof, e.g., +1 hour 1 minute 1 second,
     *         returns a date equal to the increment/decrement plus the value of
     *         <code>now</code>.
     *         </ul>
     */
    private static Object time(Object dateTimeString, Date now) {
        try {
            if (dateTimeString != null) {
                String trimmed = String.valueOf(dateTimeString).trim();
				if (trimmed.endsWith(",") || trimmed.endsWith("."))
					trimmed = trimmed.substring(0, trimmed.length() - 1);//drop last char
                for (PatternAndFormat paf : known) {
                    Matcher m = paf.matches(trimmed);
                    if (m.matches()) {
                        // System.out.println(String.format("[%s] triggered format [%s]: %s",
                        // dateTimeString, paf.f, new Date(time)));
                        return paf.parse(trimmed, now, m);
                    }
                }

                // no match
            }
            return Boolean.FALSE;
        } catch (Exception e) { // thrown by various features of the parser
            if (!Boolean.parseBoolean(System.getProperty(StringToTime.class + ".EXCEPTION_ON_PARSE_FAILURE", "false"))) {
                return Boolean.FALSE;
            } else {
                throw new StringToTimeException(dateTimeString, e);
			}
        }
    }

    private static ParserResult getParserResult(String trimmedDateTimeString, Date now) throws ParseException {
        for (PatternAndFormat paf : known) {
            Matcher m = paf.matches(trimmedDateTimeString);
            if (m.matches()) {
                return new ParserResult(paf.parse(trimmedDateTimeString, now, m), paf.f.type);
            }
        }

        return null;
    }

    public static Object date(Object dateTimeString) {
        return date(dateTimeString, new Date());
    }

    private static Object date(Object dateTimeString, Date now) {
        Object time = time(dateTimeString, now);
        return (Boolean.FALSE.equals(time)) ? Boolean.FALSE : new Date((Long) time);
    }

    public static Object cal(Object dateTimeString) {
        return cal(dateTimeString, new Date());
    }

    private static Object cal(Object dateTimeString, Date now) {
        Object date = date(dateTimeString, now);
        if (Boolean.FALSE.equals(date))
            return Boolean.FALSE;
        else {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) date);
            return cal;
        }
    }

    private static class PatternAndFormat {
        final Pattern p;
        final Format f;

        PatternAndFormat(Pattern p, Format f) {
            this.p = p;
            this.f = f;
        }

        Matcher matches(String dateTimeString) {
            return p.matcher(dateTimeString);
        }

        Long parse(String dateTimeString, Date now, Matcher m) throws ParseException {
            return f.parse(dateTimeString, now, m).getTime();
        }
    }

    private static class ParserResult {
        final FormatType type;
        final Long timestamp;

        ParserResult(Long timestamp, FormatType type) {
            this.timestamp = timestamp;
            this.type = type;
        }
    }

    private static class Format {

        private static final Pattern unit = Pattern
                .compile("(\\d+) +(s(ec(ond)?)?|mo(n(th)?)?|(hour|hr?)|d(ay)?|(w(eek)?|wk)|m(in(ute)?)?|(y(ear)?|yr))s?");

        private static final Pattern removeExtraSpaces = Pattern.compile(" +");

        private static final Map<String, Integer> translateDayOfWeek = new HashMap<>();

        static {
            translateDayOfWeek.put("sunday", 1);
            translateDayOfWeek.put("sun", 1);
            translateDayOfWeek.put("monday", 2);
            translateDayOfWeek.put("mon", 2);
            translateDayOfWeek.put("tuesday", 3);
            translateDayOfWeek.put("tue", 3);
            translateDayOfWeek.put("wednesday", 4);
            translateDayOfWeek.put("wed", 4);
            translateDayOfWeek.put("thursday", 5);
            translateDayOfWeek.put("thu", 5);
            translateDayOfWeek.put("friday", 6);
            translateDayOfWeek.put("fri", 6);
            translateDayOfWeek.put("saturday", 7);
            translateDayOfWeek.put("sat", 7);
        }

        private String sdf;

        private FormatType type;

        Format(FormatType type) {
            this.type = type;
        }

        Format(String sdf) {
            this.sdf = sdf;
        }

        @Override
        public String toString() {
            if (sdf != null)
                return sdf;
            else
                return type.toString();
        }

        Date parse(String dateTimeString, Date now, Matcher m) throws ParseException {
            if (sdf != null) {
				if (m.groupCount() > 1) {
					StringBuilder sb = new StringBuilder(dateTimeString.length());
					for (int i = 1; i <= m.groupCount(); i++) {
						sb.append(m.group(i));
					}
					dateTimeString = sb.toString();
				}
                return new SimpleDateFormat(sdf, DEFAULT_LOCALE).parse(dateTimeString);
            } else {
                dateTimeString = removeExtraSpaces.matcher(dateTimeString).replaceAll(" ").toLowerCase();

                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(now);

                    // word expressions, e.g., "now" and "today" and "tonight"
                    if (type == FormatType.WORD) {
                        switch (dateTimeString) {
                            case "now":
                                return now;
                            case "today":
                                cal.set(Calendar.HOUR_OF_DAY, 0);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                return new Date(cal.getTimeInMillis());
                            case "morning":
                            case "this morning":
                                // by default, this morning begins at 07:00:00.000
                                int thisMorningBeginsAt = Integer.parseInt(System.getProperty(StringToTime.class + ".THIS_MORNING_BEGINS_AT", "7"));
                                cal.set(Calendar.HOUR_OF_DAY, thisMorningBeginsAt);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                return new Date(cal.getTimeInMillis());
                            case "noon":
                                // noon is 12:00:00.000
                                cal.set(Calendar.HOUR_OF_DAY, 12);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                return new Date(cal.getTimeInMillis());
                            case "afternoon":
                            case "this afternoon":
                                // by default, this afternoon begins at 13:00:00.000
                                int thisAfternoonBeginsAt = Integer.parseInt(System.getProperty(StringToTime.class + ".THIS_AFTERNOON_BEGINS_AT",
                                        "13"));
                                cal.set(Calendar.HOUR_OF_DAY, thisAfternoonBeginsAt);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                return new Date(cal.getTimeInMillis());
                            case "evening":
                            case "this evening":
                                // by default, this evening begins at 17:00:00.000
                                int thisEveningBeginsAt = Integer.parseInt(System.getProperty(StringToTime.class + ".THIS_EVENING_BEGINS_AT", "17"));
                                cal.set(Calendar.HOUR_OF_DAY, thisEveningBeginsAt);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                return new Date(cal.getTimeInMillis());
                            case "tonight":
                                // by default, tonight begins at 20:00:00.000
                                int tonightBeginsAt = Integer.parseInt(System.getProperty(StringToTime.class + ".TONIGHT_BEGINS_AT", "20"));
                                cal.set(Calendar.HOUR_OF_DAY, tonightBeginsAt);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                return new Date(cal.getTimeInMillis());
                            case "midnight":
                                return new StringToTime("00:00:00 +24 hours", now);
                            case "tomorrow":
                                return new StringToTime("now +24 hours", now);
                            case "tomorrow morning":
                                return new StringToTime("morning +24 hours", now);
                            case "tomorrow noon":
                            case "noon tomorrow":
                                return new StringToTime("noon +24 hours", now);
                            case "tomorrow afternoon":
                                return new StringToTime("afternoon +24 hours", now);
                            case "tomorrow evening":
                                return new StringToTime("evening +24 hours", now);
                            case "tomorrow night":
                                return new StringToTime("tonight +24 hours", now);
                            case "yesterday":
                                return new StringToTime("now -24 hours", now);
                            case "yesterday morning":
                                return new StringToTime("morning -24 hours", now);
                            case "yesterday noon":
                            case "noon yesterday":
                                return new StringToTime("noon -24 hours", now);
                            case "yesterday afternoon":
                                return new StringToTime("afternoon -24 hours", now);
                            case "yesterday evening":
                                return new StringToTime("evening -24 hours", now);
                            case "yesterday night":
                                return new StringToTime("tonight -24 hours", now);
                            default:
                                throw new ParseException(String.format("Unrecognized date word: %s", dateTimeString), 0);
                        }
                    }

                    // time expressions, 24-hour and 12-hour
                    else if (type == FormatType.TIME) {
                        // An expression of time
                        // (hour)(:(minute))?((:(second))(.(millisecond))?)?(
                        // *(am?|pm?))?(RFC 822 time zone|general time zone)?
                        String hour = m.group(1);
                        String min = m.group(3);
                        String sec = m.group(5);
                        String ms = m.group(7);
                        String amOrPm = m.group(8);

                        if (hour != null) {
                            if (amOrPm != null)
                                cal.set(Calendar.HOUR, Integer.parseInt(hour));
                            else
                                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
                        } else
                            cal.set(Calendar.HOUR, 0);

                        cal.set(Calendar.MINUTE, (min != null ? Integer.parseInt(min) : 0));
                        cal.set(Calendar.SECOND, (sec != null ? Integer.parseInt(sec) : 0));
                        cal.set(Calendar.MILLISECOND, (ms != null ? Integer.parseInt(ms) : 0));

                        if (amOrPm != null)
                            cal.set(Calendar.AM_PM, (amOrPm.equals("a") || amOrPm.equals("am") ? Calendar.AM : Calendar.PM));

                        return new Date(cal.getTimeInMillis());
                    }

                    // increments
                    else if (type == FormatType.INCREMENT || type == FormatType.DECREMENT) {
                        Matcher units = unit.matcher(dateTimeString);
                        while (units.find()) {
                            int val = Integer.parseInt(units.group(1)) * (type == FormatType.DECREMENT ? -1 : 1);
                            String u = units.group(2);

                            // second
                            if ("s".equals(u) || "sec".equals(u) || "second".equals(u))
                                cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) + val);

                            // minute
                            else if ("m".equals(u) || "min".equals(u) || "minute".equals(u))
                                cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE) + val);

                            // hour
                            else if ("h".equals(u) || "hr".equals(u) || "hour".equals(u))
                                cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) + val);

                            // day
                            else if ("d".equals(u) || "day".equals(u))
                                cal.set(Calendar.DATE, cal.get(Calendar.DATE) + val);

                            // week
                            else if ("w".equals(u) || "wk".equals(u) || "week".equals(u))
                                cal.set(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR) + val);

                            // month
                            else if ("mo".equals(u) || "mon".equals(u) || "month".equals(u))
                                cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + val);

                            // year
                            else if ("y".equals(u) || "yr".equals(u) || "year".equals(u))
                                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + val);

                            else
                                throw new IllegalArgumentException(String.format("Unrecognized %s unit: [%s]", type, u));
                        }

                        return new Date(cal.getTimeInMillis());
                    }

                    // compound expressions
                    else if (type == FormatType.COMPOUND) {
                        Object date = StringToTime.date(m.group(1), now);
                        if (!Boolean.FALSE.equals(date))
                            return (Date) StringToTime.date(m.group(2), (Date) date);
                        else
                            throw new IllegalArgumentException(String.format("Couldn't parse %s, so couldn't compound with %s", m.group(1), m
                                    .group(2)));
                    }

                    // month of the year
                    else if (type == FormatType.MONTH) {
                        Calendar ref = Calendar.getInstance();
                        ref.setTime(new SimpleDateFormat("MMM d, y", DEFAULT_LOCALE).parse(String.format("%s 1, 1970", m.group(1))));
                        cal.set(Calendar.MONTH, ref.get(Calendar.MONTH));

                        return new Date(cal.getTimeInMillis());
                    }

                    // day of week
                    else if (type == FormatType.DAY_OF_WEEK) {
                        Integer ref = translateDayOfWeek.get(dateTimeString);
                        if (cal.get(Calendar.DAY_OF_WEEK) >= ref)
                            cal.set(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR) + 1);
                        cal.set(Calendar.DAY_OF_WEEK, ref);

                        return new Date(cal.getTimeInMillis());
                    }

                    // month and day with slashes
                    else if (type == FormatType.MONTH_AND_DATE_WITH_SLASHES) {
                        Calendar ref = Calendar.getInstance();
                        ref.setTime(new SimpleDateFormat("M/d/y", DEFAULT_LOCALE).parse(String.format("%s/%s/1970", m.group(1), m.group(3))));
                        cal.set(Calendar.MONTH, ref.get(Calendar.MONTH));
                        cal.set(Calendar.DATE, ref.get(Calendar.DATE));

                        return new Date(cal.getTimeInMillis());
                    }

                    // month and day long-hand
                    else if (type == FormatType.MONTH_AND_DATE) {
                        Calendar ref = Calendar.getInstance();
                        ref.setTime(new SimpleDateFormat("MMM d, y", DEFAULT_LOCALE).parse(String.format("%s %s, 1970", m.group(1), m.group(2))));
                        cal.set(Calendar.MONTH, ref.get(Calendar.MONTH));
                        cal.set(Calendar.DATE, ref.get(Calendar.DATE));

                        return new Date(cal.getTimeInMillis());
                    }

                    // next X
                    else if (type == FormatType.NEXT) {
                        // Format types MONTH and DAY_OF_WEEK both return future dates, so
                        // no additional processing is needed
                        String expr = m.group(1);
                        ParserResult parsed = StringToTime.getParserResult(expr, now);

                        if (parsed != null
                                && (FormatType.MONTH.equals(parsed.type) || FormatType.DAY_OF_WEEK.equals(parsed.type) || FormatType.MONTH_AND_DATE
                                        .equals(parsed.type)))
                            return new Date(parsed.timestamp);
                        else {
                            if ("week".equals(expr))
                                cal.set(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR) + 1);
                            else if ("month".equals(expr))
                                cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 1);
                            else if ("year".equals(expr))
                                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
                            else
                                throw new IllegalArgumentException(String.format("Invalid expression of time: %s", dateTimeString));

                            return new Date(cal.getTimeInMillis());
                        }
                    }

                    // last X
                    else if (type == FormatType.LAST) {
                        String expr = m.group(1);
                        ParserResult parsed = StringToTime.getParserResult(expr, now);

                        if (parsed != null && (FormatType.MONTH.equals(parsed.type) || FormatType.MONTH_AND_DATE.equals(parsed.type))) {
                            return new StringToTime("-1 year", new Date(parsed.timestamp));
                        }

                        else if (parsed != null && FormatType.DAY_OF_WEEK.equals(parsed.type)) {
                            return new StringToTime("-1 week", new Date(parsed.timestamp));
                        }

                        else {
                            if ("week".equals(expr))
                                cal.set(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR) - 1);
                            else if ("month".equals(expr))
                                cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
                            else if ("year".equals(expr))
                                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
                            else
                                throw new IllegalArgumentException(String.format("Invalid expression of time: %s", dateTimeString));

                            return new Date(cal.getTimeInMillis());
                        }
                    }

                    // year
                    else if (type == FormatType.YEAR) {
                        cal.set(Calendar.YEAR, Integer.parseInt(m.group(0)));
                        return new Date(cal.getTimeInMillis());
                    }

                    // unimplemented format type
                    else
                        throw new IllegalStateException(String.format("Unimplemented FormatType: %s", type));
                } catch (ParseException | IllegalStateException | IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Unknown failure in string-to-time conversion: %s", e.getMessage()), e);
                }
            }
        }
    }

    private enum FormatType {
        COMPOUND, MONTH_AND_DATE_WITH_SLASHES, MONTH_AND_DATE, MONTH, DAY_OF_WEEK, NEXT, LAST, INCREMENT, DECREMENT, WORD, TIME, YEAR
    }

}
