package nbdp.trax.report;

/**
 * The result of a hierarchical time-by-task report. Contains the root
 * {@link CompositeTime} tree and the period boundaries observed.
 *
 * @author Mark Boyd
 */
public class CompositeReportResult
{
    private final CompositeTime root;
    private final long totalMillis;
    private final long earliest;
    private final long latest;

    public CompositeReportResult(CompositeTime root, long totalMillis, long earliest, long latest)
    {
        this.root = root;
        this.totalMillis = totalMillis;
        this.earliest = earliest;
        this.latest = latest;
    }

    public CompositeTime getRoot()
    {
        return root;
    }

    public long getTotalMillis()
    {
        return totalMillis;
    }

    public double getTotalHours()
    {
        return totalMillis / 3600000.0;
    }

    public long getEarliest()
    {
        return earliest;
    }

    public long getLatest()
    {
        return latest;
    }

    public boolean isEmpty()
    {
        return root == null;
    }
}