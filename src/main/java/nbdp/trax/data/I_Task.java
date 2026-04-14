package nbdp.trax.data;

/**
 * Representation of a Task. Also contains several constants specific to tasks.
 * @author mboyd
 */
public interface I_Task
{
    public static final String UNASSIGNED_TASK_LABEL = "Unassigned";
    public static final int UNASSIGNED_TASK_ID = -2;
    public static final int NO_TASK_ID = -1;
    public static final int ROOT_TASK_PARENT_ID = NO_TASK_ID;
    
    public int getId();
    public void setId(int id);
    public String getName();
    public void setName(String name);
    public String getDescription();
    public void setDescription(String desc);
    public int getTypeId();
    public void setTypeId(int type);
    public boolean getIsCompleted();
    public void setIsCompleted(boolean b);
    public int getParentId();
    public void setParentId(int id);
}
