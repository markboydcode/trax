/*
 * Created on May 8, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package nbdp.trax;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;

import nbdp.trax.data.I_Type;

/**
 * Renderer for the cells in TimesliceView's type drop down selection box.
 *
 * @author Mark Boyd
 * @copyright: Copyright, 2008, The Church of Jesus Christ of Latter Day Saints
 *
 */
class RecentTypeFieldRenderer implements ListCellRenderer
{
    private TypeComboboxModel model = null;
    private JSeparator separator = new JSeparator();
    private JLabel combinedLabel = new JLabel();
    private JPanel labelWithSeparator = new JPanel(new CustomLayoutManger(combinedLabel, separator));
    private JLabel standaloneLabel = new JLabel();

    public RecentTypeFieldRenderer(TypeComboboxModel model)
    {
        this.model = model;
        labelWithSeparator.add(combinedLabel);
        labelWithSeparator.add(separator);

        combinedLabel.setOpaque(true);
        combinedLabel.setHorizontalAlignment(JLabel.CENTER);
        combinedLabel.setVerticalAlignment(JLabel.CENTER);
        standaloneLabel.setOpaque(true);
        standaloneLabel.setHorizontalAlignment(JLabel.CENTER);
        standaloneLabel.setVerticalAlignment(JLabel.CENTER);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(200, 200);
        frame.setBackground(Color.GREEN);
        Container container = frame.getRootPane().getContentPane();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        RecentTypeFieldRenderer type = new RecentTypeFieldRenderer(null);
        type.standaloneLabel.setText("smile there");
        type.standaloneLabel.setOpaque(true);
        type.standaloneLabel.setBackground(Color.WHITE);

        type.combinedLabel.setText("hello there");
        type.combinedLabel.setOpaque(true);
        type.combinedLabel.setBackground(Color.PINK);

        type.separator.setOpaque(true);
        type.separator.setBackground(Color.BLACK);
        type.separator.setForeground(Color.CYAN);

        type.labelWithSeparator.setOpaque(true);
        type.labelWithSeparator.setBackground(Color.YELLOW);

        frame.getRootPane().getContentPane().add(type.labelWithSeparator);
        frame.getRootPane().getContentPane().add(type.standaloneLabel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    /*
     * This method set the text corresponding to the selected
     * value and returns the label, set up to display the text.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        Component renderer = standaloneLabel;
        JLabel label = standaloneLabel;

        //Get the selected index. (The index param isn't
        //always valid, so just use the value.)
        I_Type type = (I_Type) value;
        if (type == null) {
            label.setText("");
        }
        else
        {
            if (model.isLastInRecentlyUsedList(value) &&
                    ! isSelected) {
                // only show separator when viewed in list not when viewed as
                // the selected item.
                label = combinedLabel;
                renderer = labelWithSeparator;
            }
            // show the type's name for the label
            label.setText(type.getName());
            if (isSelected)
            {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else
            {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
        }
        return renderer;
    }

    /**
     * LayoutManager for a container holding a label and a separator that should
     * render immediately below the label with its preferred height but matching the
     * label's width.
     *
     * @author Mark Boyd
     * @copyright: Copyright, 2008, The Church of Jesus Christ of Latter Day Saints
     *
     */
    class CustomLayoutManger implements LayoutManager
    {
        private JSeparator separator = new JSeparator();
        private JLabel label = new JLabel();

        public CustomLayoutManger(JLabel label, JSeparator separator)
        {
            this.label = label;
            this.separator = separator;
        }

        public void addLayoutComponent(String name, Component comp)
        {
        }

        public void layoutContainer(Container parent)
        {
            Dimension labelPref = label.getPreferredSize();
            Dimension cSize = parent.getSize();
            int parentWidth = (int) cSize.getWidth();
            Point labelLoc = new Point(parentWidth/2 - labelPref.width/2,
                    cSize.height/2 - labelPref.height/2);
            label.setLocation(labelLoc);
            label.setSize(labelPref);
            separator.setLocation(0, labelLoc.y + labelPref.height);
            separator.setSize(parentWidth,
                    (int)(separator.getPreferredSize().getHeight()));
        }

        public Dimension minimumLayoutSize(Container parent)
        {
            Dimension size = new Dimension(label.getMinimumSize().width,
                    (int) (label.getMinimumSize().height
                            + separator.getPreferredSize().getHeight()));
            return size;
        }

        public Dimension preferredLayoutSize(Container parent)
        {
            Dimension size = new Dimension(label.getPreferredSize().width,
                    (int) (label.getPreferredSize().height
                            + separator.getPreferredSize().getHeight()));
            return size;
        }

        public void removeLayoutComponent(Component comp)
        {
        }
    }
}