package nbdp.trax.data;
import java.sql.Timestamp;
import java.util.List;

/**
 * Represents a timeline of time slices.
 * @author mboyd
 */
public interface I_Timeline
{
    public static final int NO_LINE_ID = -1;
    
    public int getId();
    public void setId(int id);
    public Timestamp getStart();
    public void setStart(Timestamp t);
    public Timestamp getStop();
    public void setStop(Timestamp t);
    public List getSlices();
    public void setSlices(List l);
}
