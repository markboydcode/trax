

package nbdp.trax;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * 
 * @author Mark Boyd
 *
 */
public final class Constants
{

    public static final String SAX_PARSER_PROP = "org.xml.sax.driver";
    public static final String SAX_PARSER_CLASS = "org.apache.xerces.parsers.SAXParser";

    public static final String[] STRING_ARRAY = new String[] {};

    /**
     * Timestamp format of "2007.11.23 12:45 PM".
     */
    public static final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat(
    "yyyy.MM.dd hh:mm a");

    /**
     * Timestamp format of "Wed., 2007.11.23 12:45 PM".
     */
    public static final SimpleDateFormat DAY_TIMESTAMP_FORMATTER = new SimpleDateFormat(
    "EEE., yyyy.MM.dd hh:mm a");

    /**
     * Timestamp format of "Wed., 2007.11.23 12:45 PM".
     */
    public static final SimpleDateFormat DAY_AND_DATE_FORMATTER = new SimpleDateFormat(
    "EEE., yyyy.MM.dd");

    /**
     * Timestamp format of "12:45 PM".
     */
    public static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat(
    "hh:mm a");

    public static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat(
            "0.00");

}
