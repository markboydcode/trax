package nbdp.trax;
import java.awt.event.*;
import javax.swing.*;

public class TimelinePopupMenu
    extends JPopupMenu
{
    private JMenuItem launchBtn   = null;
    private JMenuItem addBtn      = null;
    private TimelineView controller = null;

    public TimelinePopupMenu( TimelineView timelineView )
    {
        this.controller = timelineView;

	// build menu layout
        add( new JMenuItem( controller.actions.get( "Launch" ) ) );
        add( new JMenuItem( controller.actions.get( "Add" ) ) );
    }

    public void showMenu( MouseEvent m )
    {
        // determine if we need to show the popup menu or not and adjust
        // menuItems based on what the cursor is sitting over

	if ( ! ( m.getSource() instanceof JTable ) )
	    controller.table.clearSelection();

	show( (JComponent) m.getSource(), m.getX(), m.getY() );
    }
}







