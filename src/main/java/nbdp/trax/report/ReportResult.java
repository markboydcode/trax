package nbdp.trax.report;

import java.util.List;

/**
 * The result of a flat time summary report (by type or by task). Contains
 * the sorted list of entries, the total duration, and the earliest/latest
 * timestamps observed.
 *
 * @author Mark Boyd
 */
public class ReportResult
{
    private final List<ReportEntry> entries;
    private final long totalMillis;
    private final long earliest;
    private final long latest;

    public ReportResult(List<ReportEntry> entries, long totalMillis, long earliest, long latest)
    {
        this.entries = entries;
        this.totalMillis = totalMillis;
        this.earliest = earliest;
        this.latest = latest;
    }

    public List<ReportEntry> getEntries()
    {
        return entries;
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
        return entries == null || entries.isEmpty();
    }
}