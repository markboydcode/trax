/*
 * Created on Apr 11, 2006
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
public class DbTask implements I_Task
{
    private int id = -1;
    private String name = null;
    private String description = null;
    private boolean completed = false;
    private int type = -1;
    private int parentId = -1;
    
    public int getParentId()
    {
        return parentId;
    }
    public void setParentId(int parentId)
    {
        this.parentId = parentId;
    }
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String desc)
    {
        this.description = desc;
    }

    public int getTypeId()
    {
        return type;
    }

    public void setTypeId(int type)
    {
        this.type = type;
    }
    public boolean getIsCompleted()
    {
        return completed;
    }

    public void setIsCompleted(boolean b)
    {
        this.completed = b;
    }
    public String toString()
    {
        return "" + this.id + ": [^" + parentId + "] " + name; 
    }
}
