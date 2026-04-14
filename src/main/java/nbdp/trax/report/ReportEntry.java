package nbdp.trax.report;

/**
 * A single row in a time summary report, pairing a label (type name or
 * task name) with the total duration in milliseconds.
 *
 * @author Mark Boyd
 */
public class ReportEntry
{
    private final String label;
    private final long sumMillis;

    public ReportEntry(String label, long sumMillis)
    {
        this.label = label;
        this.sumMillis = sumMillis;
    }

    public String getLabel()
    {
        return label;
    }

    public long getSumMillis()
    {
        return sumMillis;
    }

    public double getHours()
    {
        return sumMillis / 3600000.0;
    }
}