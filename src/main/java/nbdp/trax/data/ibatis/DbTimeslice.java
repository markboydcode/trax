package nbdp.trax.data.ibatis;
import java.sql.Timestamp;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_Type;

/**
 * Represents a the start of a slice of time.
 * 
 * @author mboyd
 */
public class DbTimeslice implements I_Timeslice
{
    private int lineId = I_Timeline.NO_LINE_ID;
    private Timestamp start = null;
    private int taskId = I_Task.NO_TASK_ID;
    private int typeId = I_Type.NO_TYPE_ID;
    private String note = null;
    private long duration = 0;
    
    
    public long getDuration()
    {
        return duration;
    }
    public void setDuration(long duration)
    {
        this.duration = duration;
    }
    public int getTimelineId()
    {
        return lineId;
    }
    public void setTimelineId(int id)
    {
        this.lineId = id;
    }
    public String getNote()
    {
        return note;
    }
    public void setNote(String note)
    {
        this.note = note;
    }
    public Timestamp getStart()
    {
        return start;
    }
    public void setStart(Timestamp start)
    {
        this.start = start;
    }
    public int getTaskId()
    {
        return taskId;
    }
    public void setTaskId(int taskId)
    {
        this.taskId = taskId;
    }
    public int getTypeId()
    {
        return typeId;
    }
    public void setTypeId(int typeId)
    {
        this.typeId = typeId;
    }
    /* (non-Javadoc)
     * @see nbdp.trax.data.I_Timeslice#copy()
     */
    public I_Timeslice copy()
    {
        DbTimeslice slice = new DbTimeslice();
        slice.setDuration(this.getDuration());
        slice.setNote(this.getNote());
        slice.setTaskId(this.getTaskId());
        slice.setTimelineId(this.getTimelineId());
        slice.setTypeId(this.getTypeId());
        slice.setStart(new Timestamp(this.getStart().getTime()));
        return slice;
    }
}
