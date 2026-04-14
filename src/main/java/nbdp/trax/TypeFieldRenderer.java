/*
 * Created on May 8, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package nbdp.trax;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import nbdp.trax.data.I_Type;

/**
 * Renderer for the cells in TimesliceView's type drop down selection box.
 *
 * @author Mark Boyd
 * @copyright: Copyright, 2008, The Church of Jesus Christ of Latter Day Saints
 *
 */
class TypeFieldRenderer extends JLabel implements ListCellRenderer
{
    public TypeFieldRenderer()
    {
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
    }

    /*
     * This method set the text corresponding to the selected
     * value and returns the label, set up to display the text.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        //Get the selected index. (The index param isn't
        //always valid, so just use the value.)
        I_Type type = (I_Type) value;
        if (type == null)
            setText("");
        else
        {
            // show the type's name for the label
            setText(type.getName());
            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        }
        return this;
    }
}