package nbdp.trax.calendar;

import java.util.Date;

/**
 * And interface allowing communication of date changes.
 * 
 * @author mboyd
 */
public interface I_DateListener
{
    public void dateChanged(Date newDate);
}
