package nbdp.trax;
import javax.swing.tree.*;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_TraxDao;

import java.io.*;
import java.util.*;
import java.text.*;

public class UI_TaskNode extends DefaultMutableTreeNode
{
    private boolean subtasksLoaded = false;

    public UI_TaskNode(Object o)
    {
            setUserObject(o);
    }

    public boolean getAllowsChildren()
    {
        return true;
    }

    public boolean isLeaf()
    {
        return false;
    }

    public boolean isRootNode()
    {
        return getUserObject() == null;
    }
    
    public int getTaskId() {
        if (isRootNode()) {
            return I_Task.ROOT_TASK_PARENT_ID;
        }
        return getTask().getId();
    }

    public I_Task getTask()
    {
        return (I_Task) getUserObject();
    }

    public boolean subtasksLoaded()
    {
        return subtasksLoaded;
    }

    public String toString()
    {
        I_Task t = (I_Task) getUserObject();
        if (t != null)
            return t.getName();
        return "Root UI Task";
    }
    
    /**
     * Searches all nested subtasks looking for one with an id matching the 
     * passed-in value.
     * 
     * @param taskId
     * @return
     */
    public UI_TaskNode findSubTask(int taskId)
    {
        loadSubtasks(false);
        if (this.children != null && this.children.size() > 0)
        {
            for( Iterator itr = this.children.listIterator(); itr.hasNext();)
            {
                UI_TaskNode utn = (UI_TaskNode) itr.next();
                I_Task t = utn.getTask();
                if (t.getId() == taskId)
                    return utn;
            }
            // if we didn't find it then pass through again and load their
            // children and ask them for the task. This may not be that big 
            // of savings.
            for( Iterator itr = this.children.listIterator(); itr.hasNext();)
            {
                UI_TaskNode utn = (UI_TaskNode) itr.next();
                UI_TaskNode target = utn.findSubTask(taskId);
                if (target != null)
                    return target;
            }
        }
        // couldn't find it beneath this task node
        return null;
    }

    public void loadSubtasks(boolean hideCompletedTasks)
    {
        if (!subtasksLoaded)
        {
            ServiceLocator locator = ServiceLocator.getInstance();
            I_TraxDao dao = locator.getDAO();
            I_Task t = (I_Task) getUserObject();
            List subTasks = null;
            
            if (t == null) // root of UI tree
            {
                subTasks = dao.getSubTasks(I_Task.ROOT_TASK_PARENT_ID);
            }
            else
            {
                subTasks = dao.getSubTasks(t.getId());
            }

            if (subTasks != null && subTasks.size()>0)
            {
                Collator c = Collator.getInstance();
                c.setStrength(Collator.TERTIARY);
                TreeMap sortedMap = new TreeMap(c);
                
                for (Iterator i = subTasks.iterator(); i.hasNext();)
                {
                    I_Task child = (I_Task) i.next();
                    if (hideCompletedTasks == false
                        || (hideCompletedTasks == true && child
                                .getIsCompleted() == false))
                    {
                        sortedMap.put(child.getName(), child);
                    }
                }
                for (Iterator i = sortedMap.values().iterator(); i.hasNext();)
                    add(new UI_TaskNode((I_Task) i.next()));
            }
            subtasksLoaded = true;
        }
    }

    public void insert(MutableTreeNode child, int index)
    {
        System.out.println("inserting child into " + this.toString()
                + " at index " + index + " with numChildren "
                + this.getChildCount());
        super.insert(child, index);
    }
}

