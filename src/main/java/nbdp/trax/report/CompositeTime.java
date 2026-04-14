package nbdp.trax.report;

import java.text.Collator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import nbdp.trax.data.I_Task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for summarizing time for nested tasks and providing resolution of how
 * time was summed for nested tasks using direct time (against this task
 * directly) and indirect time (applied against sub-tasks of this one). Knows
 * its containing {@link CompositeTime} parent, has a set of
 * {@link CompositeTime} objects representing its immediately nested children,
 * and has a domain object representing either an I_Task instance, a String, or
 * null for the root composite of the view. They also have a handle on the root
 * composite allowing for total percentage calculation.
 * 
 * @author mboyd
 * 
 */
public class CompositeTime
{
    private static Log cLog = LogFactory.getLog(R_TimeCompositeByTask.class);

    private long directTime = 0;

    private long indirectTime = 0;

    private int directContributions = 0;

    private Object domainObject = null;

    private static final Collator collator = getCollator();

    private Set<CompositeTime> children = new TreeSet<CompositeTime>(new Comp());

    private CompositeTime parent = null;
    private CompositeTime root = null;

    private class Comp implements Comparator<CompositeTime>
    {
        public int compare(CompositeTime arg0, CompositeTime arg1)
        {
            CompositeTime ct0 = arg0;
            CompositeTime ct1 = arg1;
            return collator.compare(ct0.getLabelBase(), ct1.getLabelBase());
        }
    }

    public String getLabelBase()
    {
        if (domainObject == null)
            return "";
        if (domainObject instanceof String)
            return (String) domainObject;
        return ((I_Task) domainObject).getName();
    }

    public boolean equals(Object obj)
    {
        return this.hashCode() == obj.hashCode();
    }

    public int hashCode()
    {
        if (domainObject == null)
            return I_Task.ROOT_TASK_PARENT_ID;
        if (domainObject == I_Task.UNASSIGNED_TASK_LABEL)
            return I_Task.UNASSIGNED_TASK_ID;
        return ((I_Task) domainObject).getId();
    }

    public CompositeTime(Object o, CompositeTime root)
    {
        domainObject = o;
        this.root = root;
    }

    private static Collator getCollator()
    {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.TERTIARY);
        return c;
    }

    public boolean contains(CompositeTime t)
    {
        return children.contains(t);
    }

    public void addChild(CompositeTime t)
    {
        t.parent = this;
        children.add(t);
    }

    public CompositeTime getParent()
    {
        return parent;
    }

    /**
     * Return an interator over any nested CompositeTime instances.
     * 
     * @return
     */
    public Iterator iterator()
    {
        return children.iterator();
    }

    public Object getDomainObject()
    {
        return domainObject;
    }
    
    /**
     * Returns the root object that contains the full hierarchy as passed into
     * this composite at its creation.
     * 
     * @return
     */
    public CompositeTime getRootComposite() {
    	return root;
    }

    public long getDirectTime()
    {
        return directTime;
    }

    public void addDirectTime(long directTime)
    {
        if (cLog.isDebugEnabled())
            cLog.debug("----CT: " + getLabelBase() + ", add DT=" + directTime);
        this.directTime += directTime;
        this.directContributions++;
    }

    public int getDirectContributions()
    {
        return directContributions;
    }

    public long getIndirectTime()
    {
        return indirectTime;
    }

    public void addIndirectTime(long indirectTime)
    {
        if (cLog.isDebugEnabled())
            cLog
                    .debug("----CT: " + getLabelBase() + ", add IT="
                            + indirectTime);
        this.indirectTime += indirectTime;
    }

    public long getTotalTime()
    {
        return directTime + indirectTime;
    }
}
