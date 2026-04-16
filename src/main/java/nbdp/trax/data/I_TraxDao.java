package nbdp.trax.data;
import java.sql.Timestamp;
import java.util.List;

/**
 * Represents a persistent store of timeline information.
 *
 * @author mboyd
 */
public interface I_TraxDao
{
    public I_Timeline getTimeline(int timelineId);
    public void saveTimeline(I_Timeline t);
    public I_Timeline createTimeline(Timestamp start);
    public void deleteTimeline(I_Timeline t);
    public I_Timeslice createTimeslice(int lineId, Timestamp start);
    public I_Timeline getCurrentTimeline();
    public I_Timeline createCurrentTimeline();
    public void concludeCurrentTimeline();
    public void clearCurrentTimeline();

    public void getSlices(I_Timeline t);
    public List getTimelinesInPeriod(Period p);

    public I_Type[] getTypes();
    public I_Type getTypeById(int typeId);
    public void setTypes(I_Type[] types);
    public I_Type createType(String name);
    public void updateType(I_Type type);
    public void deleteType(I_Type type);
    public boolean isTypeReferenced(I_Type type);
    public List getSlicesInPeriod(Period p);

    public I_Task migrateLegacyTask(int id, int parentId, int typeId,
            String name, String description, boolean isCompleted);

    public void migrateLegacyNextTaskId(int nextTaskId);
    public I_Task createTask(int parentId, int typeId,
            String name, String description);
    public void updateTask(I_Task task);
    public String getTaskName(int taskId);
    public I_Task getTask(int taskId);
    public List getSubTasks(int taskId);
    public void deleteTask(I_Task task);
    public boolean isTaskReferenced(I_Task task);
    public void moveTask(int task_id, int parent_id);
    
    // methods for persisting aspects of views to enabled the views to restore
    // some portion of their state since the last time that they were viewed.
    public List getViewAspects(String viewId);
    public void setViewAspect(String viewId, String aspect);
    public void deleteViewAspect(String viewId, String aspect);
}
