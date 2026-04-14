package nbdp.trax.data;
import java.sql.Timestamp;

/**
 * Represents the start of an interval of time devoted to a specific activity 
 * and an optional related task.
 * 
 * @author mboyd
 */
public interface I_Timeslice
{
    public int getTimelineId();
    public void setTimelineId(int id);
    public Timestamp getStart();
    public void setStart(Timestamp t);
    public int getTaskId();
    public void setTaskId(int id);
    public int getTypeId();
    public void setTypeId(int id);
    public String getNote();
    public void setNote(String note);
    public long getDuration();
    public void setDuration(long duration);
   
    public I_Timeslice copy();
}
