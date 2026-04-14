/*
 * Created on Apr 1, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package nbdp.trax.calendar;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author mboyd
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DateHelper
{
    public static Date getStartOfDay( Date d )
    {
    GregorianCalendar g = new GregorianCalendar();
    g.setTime( d );
    g.set( GregorianCalendar.HOUR_OF_DAY, 0 );
    g.set( GregorianCalendar.MINUTE, 0 );
    g.set( GregorianCalendar.SECOND, 0 );
    g.set( GregorianCalendar.MILLISECOND, 0 );
    return g.getTime();
    }

    public static Date getEndOfDay( Date d )
    {
    GregorianCalendar g = new GregorianCalendar();
    g.setTime( d );
    g.set( GregorianCalendar.HOUR_OF_DAY, 23 );
    g.set( GregorianCalendar.MINUTE, 59 );
    g.set( GregorianCalendar.SECOND, 59 );
    g.set( GregorianCalendar.MILLISECOND, 999 );
    return g.getTime();
    }


}
