package nbdp.trax;
import java.awt.event.*;
import javax.swing.*;

public class ReportPopupMenu
    extends JPopupMenu
{
    private TimelineView controller = null;

    public ReportPopupMenu(TimelineView timelineView)
    {
        this.controller = timelineView;

        // build menu layout
        add(new JMenuItem(controller.actions.get("Timeline Type Summary Report")));
        add(new JMenuItem(controller.actions.get("Timeline Task Summary Report")));
        add(new JMenuItem(controller.actions.get("Period Type Summary Report")));
        add(new JMenuItem(controller.actions.get("Period Task Summary Report")));
        add(new JMenuItem(controller.actions.get("Period Task Composite Summary Report")));
    }

    public void showMenu( MouseEvent m )
    {
        show( (JComponent) m.getSource(), m.getX(), m.getY() );
    }
}







