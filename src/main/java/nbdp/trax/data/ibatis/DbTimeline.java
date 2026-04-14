package nbdp.trax.data.ibatis;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nbdp.trax.data.I_Timeline;

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
public class DbTimeline implements I_Timeline
{
    private int id = 0;
    private Timestamp start = null;
    private Timestamp stop = null;
    private List slices = null;
    
    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }
    public List getSlices()
    {
        return slices;
    }
    public void setSlices(List slices)
    {
        this.slices = slices;
    }
    public Timestamp getStart()
    {
        return start;
    }
    public void setStart(Timestamp start)
    {
        this.start = start;
    }
    public Timestamp getStop()
    {
        return stop;
    }
    public void setStop(Timestamp stop)
    {
        this.stop = stop;
    }
    public String toString()
    {
        return toString(new SimpleDateFormat("yyyy.MM.dd_hh.mm.ss_SSS.zzz"));
    }
    public String toString(SimpleDateFormat formatter)
    {
        StringBuffer b = new StringBuffer();
        //b.append("ID:");
        b.append(getId());
        b.append(" ");
        //b.append("\nstart:[");
        //b.append(getStart().getTime());
        //b.append("] ");
        b.append(formatter.format(new Date(getStart().getTime())));
        //b.append("\nstop:[");
        //b.append(getStop().getTime());
        //b.append("] ");
        b.append(" ");
        b.append(formatter.format(new Date(getStop().getTime())));
        b.append(" ");
        //b.append("\nduration:");
        long duration = getStop().getTime() - getStart().getTime();
        b.append(duration);
        return b.toString();
    }
}
