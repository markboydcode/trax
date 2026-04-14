/*
 * Created on May 3, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package nbdp.trax.data.ibatis;

import nbdp.trax.data.I_Task;

/**
 * @author mboyd
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TaskIds
{
    private int oldTaskId = I_Task.NO_TASK_ID;
    private int newTaskId = I_Task.NO_TASK_ID;
    private int taskId = I_Task.NO_TASK_ID;
    
    public int getNewTaskId()
    {
        return newTaskId;
    }
    public void setNewTaskId(int newTaskId)
    {
        this.newTaskId = newTaskId;
    }
    public int getOldTaskId()
    {
        return oldTaskId;
    }
    public void setOldTaskId(int oldTaskId)
    {
        this.oldTaskId = oldTaskId;
    }
    public int getTaskId()
    {
        return taskId;
    }
    public void setTaskId(int taskId)
    {
        this.taskId = taskId;
    }
}
