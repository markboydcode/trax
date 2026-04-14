

package nbdp.trax;
import java.text.*;
import java.util.*;

public class XSLTDateFormatter
{
    public static String formatLongBasedDate( String pattern,
					      String dateInMillis )
    throws Exception
    {
	long millis = Long.parseLong( dateInMillis );
	SimpleDateFormat formatter = new SimpleDateFormat( pattern );
	System.out.println( "**** made it in here. " );
	return formatter.format( new Date( millis ) );
    }
}
