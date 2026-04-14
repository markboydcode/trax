package nbdp.trax.report;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import nbdp.trax.ServiceLocator;
import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;
import nbdp.trax.data.Period;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Aggregation engine for time reports. Extracts the data-processing logic
 * from the Swing report dialogs so that it can be reused by non-UI callers
 * such as an MCP server.
 *
 * @author Mark Boyd
 */
public class ReportEngine
{
    private static Log cLog = LogFactory.getLog(ReportEngine.class);

    private final I_TraxDao dao;

    public ReportEngine(I_TraxDao dao)
    {
        this.dao = dao;
    }

    /**
     * Convenience constructor that obtains the DAO from the ServiceLocator.
     */
    public ReportEngine()
    {
        this(ServiceLocator.getInstance().getDAO());
    }

    /**
     * Summarizes time by activity type for the given timeslices. Skips
     * offline time. Returns entries sorted alphabetically by type name.
     */
    public ReportResult summarizeByType(List slices)
    {
        if (slices == null || slices.size() == 0)
        {
            return new ReportResult(new ArrayList<ReportEntry>(), 0, -1, -1);
        }

        TreeMap<Integer, Long> table = new TreeMap<Integer, Long>();
        long earliest = -1;
        long latest = -1;
        long total = 0;
        ListIterator iter = slices.listIterator();

        while (iter.hasNext())
        {
            I_Timeslice slice = (I_Timeslice) iter.next();
            int typeId = slice.getTypeId();

            if (typeId == I_Type.OFFLINE_TYPE_ID)
            {
                continue;
            }

            long start = slice.getStart().getTime();
            long end = start + slice.getDuration();

            if (earliest == -1)
                earliest = start;
            else if (start < earliest)
                earliest = start;
            if (end > latest)
                latest = end;

            total += slice.getDuration();

            Long sum = table.get(typeId);
            if (sum == null)
                table.put(typeId, slice.getDuration());
            else
                table.put(typeId, sum + slice.getDuration());
        }

        // resolve type names and sort alphabetically
        TreeMap<String, ReportEntry> sorted = new TreeMap<String, ReportEntry>();
        for (Map.Entry<Integer, Long> entry : table.entrySet())
        {
            I_Type type = dao.getTypeById(entry.getKey());
            String name = type.getName();
            sorted.put(name, new ReportEntry(name, entry.getValue()));
        }

        return new ReportResult(
            new ArrayList<ReportEntry>(sorted.values()),
            total, earliest, latest);
    }

    /**
     * Summarizes time by task for the given timeslices. Skips offline time
     * and zero-duration entries. Returns entries sorted alphabetically by
     * task name.
     */
    public ReportResult summarizeByTask(List slices)
    {
        if (slices == null || slices.size() == 0)
        {
            return new ReportResult(new ArrayList<ReportEntry>(), 0, -1, -1);
        }

        TreeMap<Integer, Long> table = new TreeMap<Integer, Long>();
        long earliest = -1;
        long latest = -1;
        long total = 0;
        ListIterator iter = slices.listIterator();

        while (iter.hasNext())
        {
            I_Timeslice slice = (I_Timeslice) iter.next();

            if (slice.getTypeId() == I_Type.OFFLINE_TYPE_ID || slice.getDuration() <= 0)
            {
                continue;
            }

            long start = slice.getStart().getTime();
            long end = start + slice.getDuration();

            if (earliest == -1)
                earliest = start;
            else if (start < earliest)
                earliest = start;
            if (end > latest)
                latest = end;

            total += slice.getDuration();

            int taskId = slice.getTaskId();
            Long sum = table.get(taskId);
            if (sum == null)
                table.put(taskId, slice.getDuration());
            else
                table.put(taskId, sum + slice.getDuration());
        }

        // resolve task names and sort alphabetically
        TreeMap<String, ReportEntry> sorted = new TreeMap<String, ReportEntry>();
        for (Map.Entry<Integer, Long> entry : table.entrySet())
        {
            int taskId = entry.getKey();
            String name;
            if (taskId == I_Task.NO_TASK_ID)
                name = I_Task.UNASSIGNED_TASK_LABEL;
            else
                name = dao.getTask(taskId).getName();
            sorted.put(name, new ReportEntry(name, entry.getValue()));
        }

        return new ReportResult(
            new ArrayList<ReportEntry>(sorted.values()),
            total, earliest, latest);
    }

    /**
     * Builds a hierarchical composite-time tree for the given period.
     * Skips offline time and zero-duration entries when accumulating
     * task buckets. Trims slices that extend beyond the period boundaries.
     */
    public CompositeReportResult summarizeByTaskComposite(Period period)
    {
        List slices = dao.getSlicesInPeriod(period);
        long earliest = period.getStop().getTime();
        long latest = period.getStart().getTime();

        if (slices == null || slices.size() == 0)
        {
            return new CompositeReportResult(null, 0,
                period.getStart().getTime(), period.getStop().getTime());
        }

        Map<Integer, CompositeTime> timeByTask = new HashMap<Integer, CompositeTime>();
        CompositeTime rootComposite = new CompositeTime(null, null);
        CompositeTime miscComposite = new CompositeTime(I_Task.UNASSIGNED_TASK_LABEL, rootComposite);
        rootComposite.addChild(miscComposite);

        ListIterator iter = slices.listIterator();

        while (iter.hasNext())
        {
            I_Timeslice slice = (I_Timeslice) iter.next();
            int taskId = slice.getTaskId();
            int typeId = slice.getTypeId();
            long start = slice.getStart().getTime();
            long end = start + slice.getDuration();
            long pStart = period.getStart().getTime();
            long pEnd = period.getStop().getTime();

            // ensure some portion of slice is in period
            if (end < pStart || start > pEnd)
            {
                continue;
            }

            // track earliest and latest, trimming to period boundaries
            if (start < pStart && end > pStart)
            {
                earliest = pStart;
                slice.setDuration(slice.getDuration() - (pStart - start));
                slice.setStart(new Timestamp(pStart));
            }
            else if (start > pStart && start < pEnd && start < earliest)
            {
                earliest = start;
            }
            if (start < pEnd && end > pEnd)
            {
                latest = pEnd;
                slice.setDuration(slice.getDuration() - (end - pEnd));
            }
            else if (end < pEnd && end > pStart && end > latest)
            {
                latest = end;
            }

            // skip offline time and empty time when adding to task buckets
            if (typeId == I_Type.OFFLINE_TYPE_ID || slice.getDuration() <= 0)
            {
                continue;
            }

            if (cLog.isDebugEnabled())
                cLog.debug("----- slice: " + slice.getNote());

            if (taskId == I_Task.NO_TASK_ID)
            {
                miscComposite.addDirectTime(slice.getDuration());
                applyIndirectTime(slice, miscComposite, rootComposite, timeByTask);
            }
            else
            {
                Integer tID = taskId;
                CompositeTime ct = timeByTask.get(tID);
                if (ct == null)
                {
                    I_Task task = dao.getTask(taskId);
                    ct = new CompositeTime(task, rootComposite);
                    timeByTask.put(taskId, ct);
                }
                ct.addDirectTime(slice.getDuration());
                applyIndirectTime(slice, ct, rootComposite, timeByTask);
            }
        }

        long totalMillis = rootComposite.getIndirectTime();
        return new CompositeReportResult(rootComposite, totalMillis, earliest, latest);
    }

    /**
     * Recursively applies indirect time up the task hierarchy until the
     * root composite is reached, creating parent composites as needed.
     */
    private void applyIndirectTime(I_Timeslice slice, CompositeTime ct,
        CompositeTime rootComposite, Map<Integer, CompositeTime> timeByTask)
    {
        if (cLog.isDebugEnabled())
            cLog.debug(".....ait: " + ct.getLabelBase());

        CompositeTime parent = ct.getParent();
        if (parent != null)
        {
            parent.addIndirectTime(slice.getDuration());
            applyIndirectTime(slice, parent, rootComposite, timeByTask);
            return;
        }
        if (ct == rootComposite)
        {
            return;
        }
        I_Task t = (I_Task) ct.getDomainObject();
        int parentId = t.getParentId();

        if (parentId == I_Task.ROOT_TASK_PARENT_ID)
        {
            rootComposite.addChild(ct);
            rootComposite.addIndirectTime(slice.getDuration());
            return;
        }
        parent = timeByTask.get(parentId);
        if (parent != null)
        {
            parent.addChild(ct);
            parent.addIndirectTime(slice.getDuration());
            applyIndirectTime(slice, parent, rootComposite, timeByTask);
            return;
        }
        I_Task parentTask = dao.getTask(parentId);
        parent = new CompositeTime(parentTask, rootComposite);
        timeByTask.put(parentTask.getId(), parent);
        parent.addChild(ct);
        parent.addIndirectTime(slice.getDuration());
        applyIndirectTime(slice, parent, rootComposite, timeByTask);
    }
}