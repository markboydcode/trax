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
    /**
     * Returns a new, unpersisted slice. It reaches the database only when its
     * timeline is saved, so an editor that is cancelled leaves nothing behind.
     */
    public I_Timeslice newTimeslice(int lineId, Timestamp start);

    public void getSlices(I_Timeline t);
    public List getTimelinesInPeriod(Period p);
    public I_Timeline getLatestTimeline();
    public I_Timeline getTimelineAt(Timestamp t);

    // fine-grained writes used by the MCP server. Callers run them in a
    // transaction after lockConfig() and lockTimeline().
    public void lockConfig();
    public I_Timeline lockTimeline(int lineId);
    public void updateTimeline(I_Timeline t);
    public void insertTimeslice(I_Timeslice s);
    public void updateTimeslice(I_Timeslice s);
    public void deleteTimeslice(I_Timeslice s);
    public void moveTimeslice(int lineId, Timestamp from, Timestamp to);

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
    public List getAllTasks();
    public void deleteTask(I_Task task);
    public boolean isTaskReferenced(I_Task task);
    public void moveTask(int task_id, int parent_id);
    
    // methods for persisting aspects of views to enabled the views to restore
    // some portion of their state since the last time that they were viewed.
    public List getViewAspects(String viewId);
    public void setViewAspect(String viewId, String aspect);
    public void deleteViewAspect(String viewId, String aspect);
}
