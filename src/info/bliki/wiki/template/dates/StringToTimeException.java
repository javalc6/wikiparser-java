package info.bliki.wiki.template.dates;
/*
code cloned from https://github.com/collegeman/stringtotime/blob/master/src/main/java/com/clutch/dates/StringToTimeException.java
*/
public final class StringToTimeException extends RuntimeException {

    private static final long serialVersionUID = -3777846121104246071L;

    public StringToTimeException(Object dateTimeString) {
        super(String.format("Failed to parse [%s] into a java.util.Date", dateTimeString));
    }

    public StringToTimeException(Object dateTimeString, Throwable cause) {
        super(String.format("Failed to parse [%s] into a java.util.Date", dateTimeString), cause);
    }

}
