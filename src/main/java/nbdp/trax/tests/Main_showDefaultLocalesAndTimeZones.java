package nbdp.trax.tests;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/*
 * Created on Oct 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author mboyd
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Main_showDefaultLocalesAndTimeZones
{

    public static void main(String[] args)
    {
        System.out.println(Locale.getDefault().toString());
        Calendar c = new GregorianCalendar();
        System.out.println(c.getTimeZone().toString());
        System.out.println(c.getTimeZone().getDisplayName());
        String[] zones = TimeZone.getAvailableIDs();
        for(int i=0; i<zones.length; i++)
            System.out.println(zones[i]);
    }
}
