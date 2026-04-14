package nbdp.trax.data;

import java.sql.Timestamp;

/**
 * A class to convey for database queries the time period in which to search 
 * for information. Includes a start and stop value both of type 
 * {@link java.sql.Timestamp}.
 * 
 * @author mboyd
 */
public class Period
{
    private Timestamp start = null;
    private Timestamp stop = null;
    
    public Period(Timestamp start, Timestamp stop)
    {
        this.start = start;
        this.stop = stop;
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
}
