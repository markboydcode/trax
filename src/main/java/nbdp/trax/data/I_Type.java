package nbdp.trax.data;

/**
 * Represents a task type. These types have an integer id and a UI 
 * presentable name. 
 * 
 * @author mboyd
 */
public interface I_Type
{
    public static final int NO_TYPE_ID = -1;
    public static final int MISC_TYPE_ID = NO_TYPE_ID;
    public static final int OFFLINE_TYPE_ID = 0;
    
    public int getId();
    public void setId(int id);
    public String getName();
    public void setName(String name);
}
