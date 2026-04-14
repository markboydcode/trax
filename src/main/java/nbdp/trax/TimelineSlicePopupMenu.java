package nbdp.trax;

import java.awt.event.*;
import javax.swing.*;

public class TimelineSlicePopupMenu extends JPopupMenu
{
    private JMenuItem editBtn = null;

    private JMenuItem deleteBtn = null;

    private TimelineView controller = null;

    private static final int NO_SELECTION = 0;

    private static final int SINGLE_SELECTION = 1;

    private static final int MULTIPLE_SELECTION = 2;

    public TimelineSlicePopupMenu(TimelineView timelineView)
    {
        this.controller = timelineView;

        // build menu layout
        add(new JMenuItem(controller.actions.get("Insert")));
        add(new JMenuItem(controller.actions.get("Continue")));
        add(new JMenuItem(controller.actions.get("Edit")));
        add(new JMenuItem(controller.actions.get("Delete")));
    }

    public void showMenu(MouseEvent m)
    {
        // adjust menuItems based on what it is sitting over

        if (m.getSource() instanceof JTable)
        {
            JTable table = (JTable) m.getSource();
            int row = table.rowAtPoint(m.getPoint());
            int count = table.getSelectedRowCount();

            if (count == 1)
            {
                int idx = table.getSelectedRow();
                if (idx != row) // right-clicked on unselected row
                    table.setRowSelectionInterval(row, row);
                updateView(SINGLE_SELECTION);
            } else if (count > 1)
            {
                int[] indices = table.getSelectedRows();
                boolean found = false;

                for (int i = 0; i < indices.length && !found; i++)
                    if (indices[i] == row)
                        found = true;

                if (found) // right-clicked on one of selected row set
                    updateView(MULTIPLE_SELECTION);
                else
                {
                    table.setRowSelectionInterval(row, row);
                    updateView(SINGLE_SELECTION);
                }
            } else
            {
                table.setRowSelectionInterval(row, row);
                updateView(SINGLE_SELECTION);
            }
        } else if (m.getSource() instanceof JScrollPane)
        {
            controller.table.clearSelection();
            updateView(NO_SELECTION);
        }

        show((JComponent) m.getSource(), m.getX(), m.getY());
    }

    private void updateView(int viewState)
    {
        if (viewState == NO_SELECTION)
        {
            controller.actions.get("Insert").setEnabled(false);
            controller.actions.get("Continue").setEnabled(false);
            controller.actions.get("Edit").setEnabled(false);
            controller.actions.get("Delete").setEnabled(false);
        } else if (viewState == SINGLE_SELECTION)
        {
            controller.actions.get("Insert").setEnabled(true);
            controller.actions.get("Continue").setEnabled(true);
            controller.actions.get("Edit").setEnabled(true);
            controller.actions.get("Delete").setEnabled(true);
        } else if (viewState == MULTIPLE_SELECTION)
        {
            controller.actions.get("Insert").setEnabled(false);
            controller.actions.get("Continue").setEnabled(false);
            controller.actions.get("Edit").setEnabled(false);
            controller.actions.get("Delete").setEnabled(true);
        }
    }
}
