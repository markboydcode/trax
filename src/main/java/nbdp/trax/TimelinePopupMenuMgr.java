package nbdp.trax;
import java.awt.event.*;
import javax.swing.*;

public class TimelinePopupMenuMgr
    implements MouseListener
{
    private TimelineSlicePopupMenu sliceMenu  = null;
    private TimelinePopupMenu generalMenu  = null;
    private ReportPopupMenu reportMenu = null;

    private TimelineView controller = null;

    public TimelinePopupMenuMgr( TimelineView timelineView )
    {
        this.controller = timelineView;

        // create menus
        sliceMenu = new TimelineSlicePopupMenu(controller);
        generalMenu = new TimelinePopupMenu(controller);
        reportMenu = new ReportPopupMenu(controller);
    }

    ////// Implementation of MouseListener Interface
    public void mousePressed( MouseEvent e )
    {
        Object source = e.getSource();
        if (source instanceof JButton)
        {
            JButton btn = (JButton) source;
            reportMenu.show(btn, 0, btn.getHeight());
            return;
        }
        showMenu( e );
    }
    public void mouseClicked( MouseEvent e )
    {
    }
    public void mouseReleased( MouseEvent e )
    {
        showMenu( e );
    }
    public void mouseEntered( MouseEvent e )
    {
    }
    public void mouseExited( MouseEvent e )
    {
    }

    ////// End of Listener Interfaces

    /**
       Determine which menu should show.
     */
    private void showMenu(MouseEvent m)
    {
        if (m.isPopupTrigger())
        {
            Object source = m.getSource();

            if (source instanceof JTable)
                sliceMenu.showMenu(m);
            else if (source instanceof JScrollPane)
                generalMenu.showMenu(m);
        }
    }
}







