/*
 * Created on May 2, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package nbdp.trax.legacy;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mboyd
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MillisToDate
{

    public static void main(String[] args)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd_hh.mm.ss a");
        if (args.length < 1)
        {
            System.out.println("Usage: java MillisToDate <millisNumber>");
            return;
        }
        long millis = Long.parseLong(args[0]);
        System.out.println(formatter.format(new Date(millis)));
    }
    
}
