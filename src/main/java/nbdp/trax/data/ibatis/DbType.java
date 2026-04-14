package nbdp.trax.data.ibatis;

import nbdp.trax.data.I_Type;

/**
 * Represents a task type. These types have a two character id and a UI 
 * presentable name. 
 * 
 * @author mboyd
 */
public class DbType implements I_Type
{
    private int id = -1;
    private String name = null;
    
    /**
     * Default constructor used by ibatis sql map result sets.
     *
     */
    public DbType()
    {
        
    }
    
    /**
     * @param misc_type_id
     * @param misc_type_name
     */
    public DbType(int id, String name)
    {
        this.id = id;
        this.name = name;
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
    public boolean equals(Object o)
    {
        if (o == null || ! (o instanceof DbType))
            return false;
        DbType t = (DbType) o;
        return t.getId() == this.id;
    }
}
