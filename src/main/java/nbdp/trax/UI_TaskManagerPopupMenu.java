package nbdp.trax;
import java.awt.Frame;
import java.awt.event.*;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.tree.*;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_TraxDao;

public class UI_TaskManagerPopupMenu extends JPopupMenu implements
        MouseListener
{
    private UI_TaskNode nodeUnderPoppedMenu = null;

    private JTree tree = null;

    private JMenuItem m_add = null;
    private JMenuItem m_edit = null;
    private JMenuItem m_delete = null;
    private JMenuItem m_move = null;
    private JMenuItem m_drop = null;
    private JMenuItem m_cancel = null;
    private JSeparator m_sep;
    private JMenuItem m_showOrHide = null;

    private UI_TaskNode movingNode = null;
    private int state = NORMAL;
    
    private static final int NORMAL = 0;
    private static final int MOVING = 1;

    private UI_TaskEditorDialog taskEditor = null;
    private UI_TaskManager manager;
    private Frame owner = null;

    public UI_TaskManagerPopupMenu(UI_TaskManager mgr, Frame frame)
    {
        this.manager = mgr;
        this.owner = frame;

        // create add subtask menu item
        m_add = new JMenuItem("Add Sub-Task");
        m_add.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent a)
            {
                addSubTask();
            }
        });
        add(m_add);

        // create edit task menu item
        m_edit = new JMenuItem("Edit Task");
        m_edit.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent a)
            {
                editTask();
            }
        });
        add(m_edit);

        // create delete task menu item
        m_delete = new JMenuItem("Delete Task");
        m_delete.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent a)
            {
                deleteTask();
            }
        });
        add(m_delete);

        // create move task menu item
        m_move = new JMenuItem("Move Task");
        m_move.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent a)
            {
                moveTask();
            }
        });
        add(m_move);

        // create drop task menu item used when moving
        m_drop = new JMenuItem("Add to Sub-Tasks");
        m_drop.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent a)
            {
                dropTask();
            }
        });
        add(m_drop);

        // create cancel task menu item
        m_cancel = new JMenuItem("Cancel");
        m_cancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent a)
            {
                state = NORMAL;
                movingNode = null;
            }
        });
        add(m_cancel);

        // create show/hide completed tasks menu item
        m_sep = new JSeparator();
        add(m_sep);
        m_showOrHide = new JCheckBoxMenuItem("Show Completed Tasks");
        m_showOrHide.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.DESELECTED)
                    manager.setHideCompletedTasks(true);
                else
                    manager.setHideCompletedTasks(false);
            }
        });
        m_showOrHide.setEnabled(true);
        add(m_showOrHide);
    }

    ////// Implementation of MouseListener Interface
    public void mousePressed(MouseEvent e)
    {
        showPopupMenu(e);
    }

    public void mouseClicked(MouseEvent e)
    {
        showPopupMenu(e);
    }

    public void mouseReleased(MouseEvent e)
    {
        showPopupMenu(e);
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    ////// End of MouseListener Interface

    public void reset()
    {
        state = NORMAL;
        movingNode = null;
    }
    
    /**
     * Initiates moving the task over which the move menu item was selected
     */
    public void moveTask()
    {
        movingNode = nodeUnderPoppedMenu;
        if (movingNode != null)
        {
            String message = "To select a destination right click and choose" +
                    " the appropriate action.";
            int urgencyLevel = JOptionPane.INFORMATION_MESSAGE;
            
            int resp = JOptionPane.showConfirmDialog(owner, 
                    message,
                    "Move Task", JOptionPane.OK_CANCEL_OPTION, 
                    urgencyLevel);
            if (resp == JOptionPane.OK_OPTION)
            {
                state = MOVING;
            }
        }
    }

    public void dropTask()
    {
        manager.moveTask(movingNode, nodeUnderPoppedMenu);
        state = NORMAL;
        movingNode = null;
    }

    /**
     * Menu item visibility appears as follows for different states:
     * <code>
     *                NORMAL MENU     MOVE MENU
     *                -------------   --------------  
     *                over    over    over    over
     * Item           task    tree    task    tree
     * ---------      -----   -----   -----   ----- 
     * Add              Y       Y       N       N
     * Edit             Y       DA      N       N
     * Delete           Y       DA      N       N
     * Move             Y       DA      N       N
     * Place Here       N       N       Y       Y
     * Cancel           N       N       Y       Y
     * -------          N       Y       N       N
     * show completed   N       Y       N       N
     * 
     * </code>
     * @param m
     */
    private void showPopupMenu(MouseEvent m)
    {
        // determine if we need to show the popup menu or not and adjust
        // menuItems for state and the root task which is only a directory
        if (m.isPopupTrigger())
        {
            tree = (JTree) m.getComponent();
            TreePath path = tree.getPathForLocation(m.getX(), m.getY());
            tree.setSelectionRow(tree.getRowForLocation(m.getX(), m.getY()));

            if (path == null) // clicked over tree
            {
                nodeUnderPoppedMenu = null;
                if (state == NORMAL)
                {
                    treeNormalMenu(m);
                }
                else // state == MOVING
                {
                    treeMovingMenu(m);
                }
            }
            else // clicked over task
            {
                nodeUnderPoppedMenu = (UI_TaskNode) path.getLastPathComponent();
                String shortName = truncate(nodeUnderPoppedMenu.toString());
                if (state == NORMAL)
                {
                    taskNormalMenu(m, shortName);
                }
                else
                {
                    taskMovingMenu(m, shortName);
                }
            }
            show(tree, m.getX(), m.getY());
        }
    }
    
    private void treeNormalMenu(MouseEvent m)
    {
        m_add.setText("Add Task");
        m_add.setVisible(true);

        m_edit.setText("Edit Task");
        m_edit.setVisible(true);
        m_edit.setEnabled(false);

        m_delete.setText("Delete Task");
        m_delete.setEnabled(false);

        m_move.setEnabled(false);
        m_move.setVisible(true);

        m_drop.setVisible(false);
        m_cancel.setVisible(false);
        m_sep.setVisible(true);
        m_showOrHide.setVisible(true);
        return;
    }

    private void taskNormalMenu(MouseEvent m, String name)
    {
        m_add.setText("Add Sub-Task to " + name);
        m_add.setVisible(true);
        
        m_edit.setText("Edit " + name);
        m_edit.setEnabled(true);
        m_edit.setVisible(true);
        
        m_delete.setText("Delete " + name);
        m_delete.setEnabled(true);
        m_delete.setVisible(true);
        
        m_move.setText("Move " + name);
        m_move.setVisible(true);
        m_move.setEnabled(true);
        
        m_drop.setVisible(false);
        m_cancel.setVisible(false);
        m_sep.setVisible(false);
        m_showOrHide.setVisible(false);
        show(tree, m.getX(), m.getY());
    }

    private void treeMovingMenu(MouseEvent m)
    {
        m_add.setVisible(false);
        m_edit.setVisible(false);
        m_delete.setVisible(false);
        m_move.setVisible(false);

        m_drop.setVisible(true);
        m_drop.setText("Move to Top Level Task");
        m_cancel.setVisible(true);

        m_sep.setVisible(false);
        m_showOrHide.setVisible(false);
    }
    
    private void taskMovingMenu(MouseEvent m, String name)
    {
        m_add.setVisible(false);
        m_edit.setVisible(false);
        m_delete.setVisible(false);
        m_move.setVisible(false);
        
        if (nodeUnderPoppedMenu == movingNode)
            m_drop.setVisible(false);
        else
            m_drop.setVisible(true);
        m_drop.setText("Move to Sub-Task of " + name);
        m_cancel.setVisible(true);

        m_sep.setVisible(false);
        m_showOrHide.setVisible(false);
    }
    
    private String truncate(String name)
    {
        if (name.length() > 10)
            return name.substring(0, 10) + "...";
        return name;
    }

    ////// implementation of the action methods
    public void addSubTask()
    {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        // create top level task
        if (nodeUnderPoppedMenu == null)
        {
            if (taskEditor == null)
                taskEditor = new UI_TaskEditorDialog();

            I_Task t = taskEditor.createSubTaskIn(null);
            if (t == null) // task not created, action cancelled
                return;

            UI_TaskNode node = new UI_TaskNode(t);
            UI_TaskNode root = (UI_TaskNode) model.getRoot();
            int index = manager.getAlphaInsertionPoint(node, root);
            if (index >= 0)
                model.insertNodeInto(node, root, index);
            tree.scrollPathToVisible(new TreePath(node.getPath()));
            return;
        }

        // create sub task of selected task
        I_Task t = nodeUnderPoppedMenu.getTask();

        if (taskEditor == null)
            taskEditor = new UI_TaskEditorDialog();

        I_Task sub = taskEditor.createSubTaskIn(t);

        if (sub == null) // task not created, action cancelled
            return;

        UI_TaskNode node = new UI_TaskNode(sub);
        int index = manager.getAlphaInsertionPoint(node, nodeUnderPoppedMenu);

        if (index >= 0)
        {
            model.insertNodeInto(node, nodeUnderPoppedMenu, index);
        }
        tree.scrollPathToVisible(new TreePath(node.getPath()));
    }

    public void editTask()
    {
        I_Task t = nodeUnderPoppedMenu.getTask();
        String name = t.getName();

        if (taskEditor == null)
            taskEditor = new UI_TaskEditorDialog();

        t = taskEditor.editTask(t);

        if (t == null) // task not edited, action cancelled
            return;
        
        // update the UI
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        
        if (t.getIsCompleted() && manager.getHideCompletedTasks())
            model.removeNodeFromParent(nodeUnderPoppedMenu);
        else
        {
            if (! name.equals(t.getName())) // name changed
            {
                UI_TaskNode parent = (UI_TaskNode) nodeUnderPoppedMenu
                        .getParent();
                model.removeNodeFromParent(nodeUnderPoppedMenu);
                int index = manager.getAlphaInsertionPoint(nodeUnderPoppedMenu, parent);
                if (index >= 0)
                    model.insertNodeInto(nodeUnderPoppedMenu, parent, index);
            }
        }
    }

    public void deleteTask()
    {
        I_Task t = nodeUnderPoppedMenu.getTask();
        if (t != null)
        {
            String message = "There are no time references to, '" + t.getName()
                    + "'.\nDelete this task?";
            int urgencyLevel = JOptionPane.INFORMATION_MESSAGE;
            
            I_TraxDao dao = ServiceLocator.getInstance().getDAO();
            boolean isReferenced = dao.isTaskReferenced(t);
            if (isReferenced)
            {
                message = "Time tracking references exist for, '" + t.getName()
                + "'. These entries will be changed to not reference " +
                        "any task. \nDelete this task?";
                urgencyLevel = JOptionPane.WARNING_MESSAGE;
            }
            int resp = JOptionPane.showConfirmDialog(owner, 
                    message,
                    "Delete Task", JOptionPane.YES_NO_OPTION, 
                    urgencyLevel);
            if (resp == JOptionPane.YES_OPTION)
            {
                dao.deleteTask(t);
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.removeNodeFromParent(nodeUnderPoppedMenu);
            }
        }
    }
}

