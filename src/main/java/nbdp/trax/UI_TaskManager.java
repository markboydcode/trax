package nbdp.trax;
import java.awt.*;
import java.awt.event.*;
import java.io.StringWriter;
import java.text.Collator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_TraxDao;

public class UI_TaskManager extends JDialog
{
    private static final Log cLog = LogFactory.getLog(UI_TaskManager.class);
    String dirPath = "";
    JTree tree = null;
    private Collator collator = null;

    public static final int EDIT_TASKS = 0;
    public static final int SELECT_TASK = 1;

    public static final int CANCEL_PRESSED = 0;
    public static final int OK_PRESSED = 1;
    private static final String EXPANDED_BRANCH_ASPECT = UI_TaskManager.class.getSimpleName() + "-exp";;
    
    private int buttonPressed = CANCEL_PRESSED;
    private boolean hideCompletedTasks = true;
    
    private int mode = EDIT_TASKS;
    private JPanel buttonPanel = createButtonPanel();
    private UI_TaskManagerPopupMenu taskManagerPopupMenu;

    public int getButtonPressed()
    {
        return buttonPressed;
    }

    public void show()
    {
        buttonPressed = CANCEL_PRESSED; // by default so closing window counts
        pack();
        super.show();
        //super.setVisible(true);
    }
    
    public boolean getHideCompletedTasks()
    {
        return hideCompletedTasks;
    }
    public void setMode( int newValue )
    {
        if ( newValue == SELECT_TASK )
        {
            mode = SELECT_TASK;
            setTitle( "Select Task" );
            addButtons();
        }
        else
        {
            mode = EDIT_TASKS;
            setTitle( "Edit Tasks" );
            removeButtons();
        }
    }

    public int getMode()
    {
        return mode;
    }

    private void addButtons()
    {
        removeButtons();
        getContentPane().add( buttonPanel, BorderLayout.SOUTH );
    }
    
    private void removeButtons()
    {
        getContentPane().remove( buttonPanel );
    }

    private void ok_button_pressed() {
        buttonPressed = OK_PRESSED;
        hide();
        //setVisible(false);
    }

    private JPanel createButtonPanel()
    {
        JPanel p = new JPanel();
        
        JButton ok = new JButton( "Ok" );
        JButton cancel = new JButton( "Cancel" );

        ok.addActionListener( new ActionListener() 
            {
                public void actionPerformed( ActionEvent e )
                {
                    ok_button_pressed();
                }
            });
        cancel.addActionListener( new ActionListener() 
            {
                public void actionPerformed( ActionEvent e )
                {
                    hide();
                    //setVisible(false);
                }
            });
                              
        p.add( ok, BorderLayout.WEST );
        p.add( cancel, BorderLayout. EAST );
        
        return p;
    }

    public I_Task getSelectedTask()
    {
        TreePath path = tree.getSelectionModel().getSelectionPath();
         if ( path == null )
             return null;
         return ((UI_TaskNode) path.getLastPathComponent()).getTask();
    }
    
    public UI_TaskManager( Frame owner )
    {
        super( owner );
        
        this.setModal( true );
        collator = Collator.getInstance();
        collator.setStrength(Collator.TERTIARY);
        
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
               if (taskManagerPopupMenu != null)
                   taskManagerPopupMenu.reset();
            }
        });
        tree = new JTree(createTreeModel());
        restoreExpandedBranches(tree);
        tree.setRootVisible( false );
        tree.setShowsRootHandles(true);
        tree.putClientProperty("JTree.lineStyle", "Angled");

        tree.getSelectionModel().setSelectionMode(DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);
        // enable use of enter key for selecting tasks
        tree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "ok-button-pressed");
        tree.getActionMap().put("ok-button-pressed", new AbstractAction()
        {

            public boolean isEnabled()
            {
                boolean is = UI_TaskManager.this.mode == UI_TaskManager.SELECT_TASK; 
                System.out.println("enabled: " + is);
                return is;
            }

            public void actionPerformed(ActionEvent e)
            {
                ok_button_pressed();
            }

        });

        tree.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2 && mode == SELECT_TASK
                        && tree.getSelectionPath() != null)
                    ok_button_pressed();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tree);
        getContentPane().add( scrollPane, BorderLayout.CENTER );        
        
        tree.addTreeExpansionListener( new TreeExpansionListener()
            {
                public void treeCollapsed( TreeExpansionEvent e )
                {
                    removeExpandedBranchRecord(e.getPath());
                }
                public void treeExpanded( TreeExpansionEvent e )
                {
                    TreePath path = e.getPath();
                    persistExpandedBranchRecord(path);
                    UI_TaskNode n = (UI_TaskNode) path.getLastPathComponent();
                    
                    if( ! n.subtasksLoaded() )
                    {
                        n.loadSubtasks(hideCompletedTasks);
                        DefaultTreeModel m = (DefaultTreeModel)tree.getModel();
                        m.nodeStructureChanged( n );
                    }
                }
            });
        taskManagerPopupMenu = new UI_TaskManagerPopupMenu( this, owner );
        tree.addMouseListener(taskManagerPopupMenu);
    }
    
    protected void persistExpandedBranchRecord(TreePath path)
    {
        String aspect = convertPath(path);
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        dao.setViewAspect(EXPANDED_BRANCH_ASPECT, aspect);
    }

    private String convertPath(TreePath path)
    {
        StringWriter sw = new StringWriter();
        for (int i = 0; i<path.getPathCount(); i++) {
            sw.write(',');
            UI_TaskNode node = (UI_TaskNode) path.getPathComponent(i);
            sw.write(Integer.toString(node.getTaskId()));
        }
        return sw.toString().substring(1);
    }

    private void restoreExpandedBranches(JTree tree2)
    {
        // get the root TreeNode object
        UI_TaskNode node = (UI_TaskNode) tree.getModel().getRoot();
        TreePath treePath = new TreePath(node);
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        List<String> expanded = dao.getViewAspects(EXPANDED_BRANCH_ASPECT);
        restoreExpandedBranch(node, treePath, tree, expanded);
    }

    private void restoreExpandedBranch(UI_TaskNode node,
        TreePath treePath, JTree tree, List<String> expanded)
    {
        String path = convertPath(treePath);
        if (expanded.contains(path)) {
            UI_TaskNode n = (UI_TaskNode) treePath.getLastPathComponent();
            
            if( ! n.subtasksLoaded() )
            {
                n.loadSubtasks(hideCompletedTasks);
                DefaultTreeModel m = (DefaultTreeModel)tree.getModel();
                m.nodeStructureChanged( n );
            }
            tree.expandPath(treePath);
        }
        for( int i=0; i<node.getChildCount(); i++) {
            UI_TaskNode child =  (UI_TaskNode) node.getChildAt(i);
            TreePath childPath = treePath.pathByAddingChild(child);
            restoreExpandedBranch(child, childPath, tree, expanded);
        }
    }

    private void removeExpandedBranchRecord(TreePath path)
    {
        String aspect = convertPath(path);
        
        // clean out persisted notion of any nested expanded children
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        List<String> aspects = dao.getViewAspects(EXPANDED_BRANCH_ASPECT);
        for( String a: aspects) {
            if (a.startsWith(aspect)) {
                dao.deleteViewAspect(EXPANDED_BRANCH_ASPECT, a);
            }
        }
    }

    public void setHideCompletedTasks(boolean b)
    {
        hideCompletedTasks = b;
        tree.setModel(createTreeModel());
    }

    public void setSelectedTask(int taskId)
    {
        hideCompletedTasks = false;
        UI_TaskNode root = (UI_TaskNode) tree.getModel().getRoot();
        UI_TaskNode node = root.findSubTask(taskId);
        if (node != null)
        {
            tree.setSelectionPath(new TreePath(node.getPath()));
            tree.scrollPathToVisible(new TreePath(node.getPath()));
        }
    }
    private DefaultTreeModel createTreeModel()
    {
        UI_TaskNode taskNode = new UI_TaskNode( null );
        taskNode.loadSubtasks(hideCompletedTasks);
        return new DefaultTreeModel( taskNode );
    }

    public static void main( String[] args )
    throws Exception
    {
        if ( args.length == 0 )
        {
            System.out.println( "usage: java UI_TaskManager taskDBdirectory" );
            return;
        }
        
        JFrame frame = new JFrame();
        UI_TaskManager manager = new UI_TaskManager( frame );
        manager.setSize( 300, 400 );
        manager.setMode( UI_TaskManager.SELECT_TASK );
        
        manager.show();
        System.out.println( "Pressed " + ( manager.getButtonPressed() == 
                                           UI_TaskManager.OK_PRESSED ? 
                                           "OK" : "CANCEL" ) );
        I_Task t = manager.getSelectedTask();
        if ( t == null )
            System.out.println( "No Task Selected" );
        else
            System.out.println( "Selected Task: " + t.getName() );
        
        System.exit(0);
    }

    /**
     * @param movingNode
     * @param id
     */
    public void moveTask(UI_TaskNode movingNode, UI_TaskNode parent)
    {
        if (movingNode == null)
            return;
        try
        {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            int taskId = movingNode.getTask().getId();
            int parentId = I_Task.ROOT_TASK_PARENT_ID;

            if (parent == null)
            {
                parent = (UI_TaskNode) model.getRoot();
            }
            else
            {
                parentId = parent.getTask().getId();
            }
            I_TraxDao dao = ServiceLocator.getInstance().getDAO();
            dao.moveTask(taskId, parentId);

            // now update the UI
            model.removeNodeFromParent(movingNode);
            int index = getAlphaInsertionPoint(movingNode, parent);
            if (index >= 0) // node not already in parent
            {
                model.insertNodeInto(movingNode, parent, index);
                
            }
            tree.scrollPathToVisible(new TreePath(movingNode.getPath()));            
        } catch (Exception e)
        {
            cLog.error("Problem occurred moving node.", e);
        }
    }
    
    /**
     * Returns the index at which a node should be inserted into the parent for
     * alphabetical ordering. If the node is already a child of the parent then
     * -1 is returned.
     * 
     * @param node
     * @param parent
     * @return
     */
    public int getAlphaInsertionPoint(UI_TaskNode node, UI_TaskNode parent)
    {
        if (parent.subtasksLoaded() == false)
            parent.loadSubtasks(hideCompletedTasks);

        // return -1 if it is already a child of this parent
        int nodeId = node.getTask().getId();
        for (Enumeration c = parent.children(); c.hasMoreElements();)
        {
            UI_TaskNode child = (UI_TaskNode) c.nextElement();
            if (child == node || child.getTask().getId() == nodeId)
            {
                return -1;
            }
        }
        
        int index = 0;
        I_Task movingTask = node.getTask();

        for (Enumeration c = parent.children(); c.hasMoreElements();)
        {
            UI_TaskNode child = (UI_TaskNode) c.nextElement();
            I_Task task = child.getTask();
            if (collator.compare(movingTask.getName(), task.getName()) < 0)
            {
                return index;
            }
            index++;
        }
        return parent.getChildCount();
    }
}
