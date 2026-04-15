package nbdp.trax;
import nbdp.trax.calendar.DateHelper;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.Period;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.io.*;
import java.util.*;
import java.sql.Timestamp;
import java.text.*;

public class TLFinder
{
    private static final Log LOG = LogFactory.getLog(TLFinder.class);
    static TLFinder instance = null;
    static SimpleDateFormat formatter =
        new SimpleDateFormat("yyyy.MM.dd_hh.mm.ss_SSS.zzz");
    LinkedList files = new LinkedList();
    private static String[] STRING_ARRAY = new String[] {};
    File baseDir = null;
        
    ///////// loading code


    ///////////////// finding code

    /**
       Returns any timelines with any elapsed time occuring from the start of
       'fromDay', early morning midnight, to the end of 'toDay', 1 millisecond
       before midnight.
     */
    public List getTimelinesForDays( Date fromDay, Date toDay )
    {
        // first change from and to to days start and days end respectively
	Date startOfFromDay = DateHelper.getStartOfDay( fromDay );
	Date endOfToDay = DateHelper.getEndOfDay( toDay );
	return getTimelinesInPeriod( startOfFromDay, endOfToDay );
    }
    
    /**
       Returns any timelines with any elapsed time occuring from and including
       the point in time represnted by 'from' to and including the point in
       time represented by 'to'.
     */
    public List getTimelinesInPeriod( Date from, Date to )
    {
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        Timestamp start = new Timestamp(from.getTime());
        Timestamp stop = new Timestamp(to.getTime());
        Period period = new Period(start, stop);
        return dao.getTimelinesInPeriod(period);
    }

    /**
       Returns true if the periods strictly overlap meaning that any
       millisecond from and including start1 upto and including end1 occupies
       the same point in time as some millisecond from and including start2
       upto and including end2.
     */
    public static boolean periodsIntersect( Date start1, Date end1,
				     Date start2, Date end2 )
    {
	return ( ( start1.equals( start2 ) ||
		   start1.equals( end2 ) ||
		   end1.equals( start2 ) ||
		   end1.equals( end2 ) ||
		   ( start1.after( start2 ) &&
		     start1.before( end2 ) ) ||
		   ( end1.after( start2 ) &&
		     end1.before( end2 ) ) ) );
    }

    /////////////

    
    public static void main(String[] s) throws Exception
    {
        GregorianCalendar g = new GregorianCalendar();
        g.set(Calendar.HOUR_OF_DAY, 23);
        g.set(Calendar.MINUTE, 59);
        g.set(Calendar.SECOND, 59);
        g.set(Calendar.MILLISECOND, 999);
        
        if (LOG.isDebugEnabled())
            LOG.debug("date is " + formatter.format(g.getTime()));
        g.add(Calendar.MILLISECOND, 1);
        if (LOG.isDebugEnabled())
            LOG.debug("date is " + formatter.format(g.getTime()));

        if (s.length < 2)
        {
            System.out.println("Usage: java TLFinder fromDate toDate");
            System.exit(0);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        Date from = sdf.parse(s[0]);
        Date to = sdf.parse(s[1]);

        ConfigurableApplicationContext ctx = SpringApplication.run(TraxApplication.class, new String[0]);
        ctx.getBean(ServiceLocator.class); // ensures DAO is wired
        
        TLFinder f = new TLFinder();
        List lines = f.getTimelinesForDays(from, to);

        System.out.println("Timelines\n from "
                + formatter.format(from) + "\n   to "
                + formatter.format(to));

        for (Iterator i = lines.iterator(); i.hasNext();)
        {
            I_Timeline line = (I_Timeline) i.next();
            Date lineDate = new Date(line.getStart().getTime());
            System.out.println(formatter.format(lineDate));
        }
    }

}
