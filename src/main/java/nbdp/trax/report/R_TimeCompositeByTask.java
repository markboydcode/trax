package nbdp.trax.report;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import nbdp.trax.Constants;
import nbdp.trax.ServiceLocator;
import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.Period;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class R_TimeCompositeByTask
    extends JDialog
{
    private static Log cLog = LogFactory.getLog(R_TimeCompositeByTask.class);
    private static String EXPANDED_BRANCH_ASPECT = R_TimeCompositeByTask.class.getSimpleName() + "-exp";
    private JLabel periodLabel = null;
    private JPanel treePanel = null;
    private Period periodOfReport = null;

    public R_TimeCompositeByTask(JFrame frame)
    {
        super(frame);
        setModal(true);
        buildUI();
    }

    public void show()
    {
        throw new UnsupportedOperationException();
    }

    public void show(Period period)
    {
        periodOfReport = period;
        ReportEngine engine = new ReportEngine();
        CompositeReportResult result = engine.summarizeByTaskComposite(period);

        if (!result.isEmpty())
        {
            double hours = result.getTotalHours();
            periodLabel.setText(this.getReportTitle(result.getEarliest(), result.getLatest(), hours));

            // create tree and contained tree model with translator TreeNodes
            // containing our CompositeTime report objects that contain an I_Task
            // for which they hold accummulated time.
            TreeModel model = new DefaultTreeModel(new TreeNode(result.getRoot()));
            JTree tree = new JTree(model);
            tree.setRootVisible( false );
            tree.setShowsRootHandles(true);
            tree.putClientProperty("JTree.lineStyle", "Angled");

            // Set up the
            // expanded branches in the tree to conform to the expanded ones
            // had last time the view was presented. Must do before adding an
            // expansion listener since we don't want to know about expansions
            // that we are firing.
            restoreExpandedBranches(tree);

            tree.addTreeExpansionListener(new TreeExpansionListener() {

                public void treeCollapsed(TreeExpansionEvent event)
                {
                    removeExpandedBranchRecord(event.getPath());
                }


                public void treeExpanded(TreeExpansionEvent event)
                {
                    persistExpandedBranchRecord(event.getPath());
                }
            });

            JScrollPane scrollPane = new JScrollPane(tree);
            treePanel.removeAll();
            treePanel.add(scrollPane, BorderLayout.CENTER);
        } else
        {
            long earliest = periodOfReport.getStart().getTime();
            long latest = periodOfReport.getStop().getTime();
            periodLabel.setText(getReportTitle(earliest, latest, 0));
            treePanel.removeAll();
        }
        super.show();
    }


    /**
     * Loads the set of persisted references to expanded nodes and walks the
     * tree to see which nodes should start out in expanded state.
     *
     * @param tree
     */
    private void restoreExpandedBranches(JTree tree)
    {
        // get the root TreeNode object
        TreeNode node = (TreeNode) tree.getModel().getRoot();
        TreePath treePath = new TreePath(node);
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        List<String> expanded = dao.getViewAspects(EXPANDED_BRANCH_ASPECT);
        restoreExpandedBranch(node, treePath, tree, expanded);
    }

    /**
     * Recursive call to determine if a node should start out in expanded state
     * and then passes each of its children to this method for them to be
     * handled likewise.
     *
     * @param node
     * @param treePath
     * @param tree
     * @param expanded
     */
    private void restoreExpandedBranch(TreeNode node, TreePath treePath, JTree tree, List<String> expanded)
    {
        String path = convertPath(treePath);
        if (expanded.contains(path)) {
            tree.expandPath(treePath);
        }
        for( int i=0; i<node.getChildCount(); i++) {
            TreeNode child = (TreeNode) node.getChildAt(i);
            TreePath childPath = treePath.pathByAddingChild(child);
            restoreExpandedBranch(child, childPath, tree, expanded);
        }
    }

    /**
     * Removes from persistence the record of a node having been expanded.
     * @param path
     */
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

    /**
     * Persists a reference to a node that has been expanded in the ui so that
     * it will show as expanded when next the ui is restored.
     *
     * @param path
     */
    private void persistExpandedBranchRecord(TreePath path)
    {
        String aspect = convertPath(path);
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        dao.setViewAspect(EXPANDED_BRANCH_ASPECT, aspect);
    }

    /**
     * Converts a TreePath of TreeNode objects containing user objects of type
     * {@link CompositeTime} to a String of comma delimited hashCode values
     * representing the task identifiers of the tasks expanded in the report
     * view.
     *
     * @param path
     * @return
     */
    private String convertPath(TreePath path)
    {
        StringWriter sw = new StringWriter();
        for (int i = 0; i<path.getPathCount(); i++) {
            sw.write(',');
            TreeNode node = (TreeNode) path.getPathComponent(i);
            sw.write(Integer.toString(node.getUserObject().hashCode()));
        }
        return sw.toString().substring(1);
    }

    private String getReportTitle(long earliest, long latest, double hours)
    {
        StringBuffer text = new StringBuffer();
        if (hours == -1) {
            text.append("<html><font color=#blue>No Timeline Values to Display</font></html>");
        }
        else {
            String metric = "hours";
            String totalHours = "No Timeline";
            if (hours == 0){
                metric = "values";
            }
            else {
                totalHours = Constants.DECIMAL_FORMATTER.format(hours);
            }
            String fromDay = Constants.DAY_AND_DATE_FORMATTER.format(new Date(earliest));
            String fromTime = Constants.TIME_FORMATTER.format(new Date(earliest));
            String toDay = Constants.DAY_AND_DATE_FORMATTER.format(new Date(latest));
            String toTime = Constants.TIME_FORMATTER.format(new Date(latest));

            if (fromDay.equals(toDay)) {
                text.append("<html><font color=#00CF20>")
                .append(totalHours)
                .append("</font><font color='gray'> ")
                .append(metric)
                .append(" on </font><font color=blue>")
                .append(fromDay)
                .append("</font><font color='gray'> from </font><font color=purple>")
                .append(fromTime)
                .append("</font><font color='gray'> to </font><font color=purple>")
                .append(toTime)
                .append("</font></html>");
            }
            else {
                text.append("<html><font color=#00CF20>")
                .append(totalHours)
                .append("</font><font color='gray'> ")
                .append(metric)
                .append(" from </font><font color=blue>")
                .append(fromDay)
                .append("</font><font color='gray'> at </font><font color=purple>")
                .append(fromTime)
                .append("</font><font color='gray'> to </font><font color=blue>")
                .append(toDay)
                .append("</font><font color='gray'> at </font><font color=purple>")
                .append(toTime)
                .append("</font></html>");
            }

        }

        return text.toString();
    }

    /**
     * Class that translates between JTree component elements and underlying
     * domain objects which in this case are instances of {@link CompositeTime},
     * String (for the miscellaneous catch-all time bucket for timeslices with
     * no attached task), and null (for the root node of the tree). This subclass
     * of {@link DefaultMutableTreeNode} provides a toString method that presents
     * what should be shown in the UI for the {@link CompositeTime} contained in
     * the tree node.
     *
     *
     * @author Mark Boyd
     * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
     *
     */
    private class TreeNode extends DefaultMutableTreeNode
    {
        /**
         * Constructs a new TreeNode for the passed-in domain object setting it
         * as its userObject and walking that object's domain children adding
         * each of them wrapped in its own TreeNode as children of this node.
         *
         * @param o
         */
        public TreeNode(Object o)
        {
            setUserObject(o);
            CompositeTime ct = (CompositeTime) o;
            for (Iterator i = ct.iterator(); i.hasNext();)
            {
                CompositeTime nt = (CompositeTime) i.next();
                this.add(new TreeNode(nt));
            }
        }

        public String toString()
        {
            CompositeTime ct = (CompositeTime) getUserObject();

            if (ct.getDomainObject() == null) // should never call for root
                return "";
            String taskTotal = Constants.DECIMAL_FORMATTER.format(ct.getTotalTime()/3600000.0);
            String taskIndirect = Constants.DECIMAL_FORMATTER.format(ct.getIndirectTime()/3600000.0);
            String taskDirect =  Constants.DECIMAL_FORMATTER.format(ct.getDirectTime()/3600000.0);
            long allTime = ct.getRootComposite().getIndirectTime(); // root is aggregator and only has indirect
            String percentOfAllTime = Constants.DECIMAL_FORMATTER.format((100.0 * ct.getTotalTime())/allTime);
            System.out.println();
            if (cLog.isDebugEnabled())
                cLog.debug("---> all: " + allTime + ", ind: " + ct.getIndirectTime()
                		+ ", ind/all: " + ((100.0 * ct.getIndirectTime())/allTime)
                		+ ", x100: " + ((100.0 * ct.getIndirectTime())/allTime));

            String percentIndirectOfAllTime = Constants.DECIMAL_FORMATTER.format((100.0 * ct.getIndirectTime())/allTime);
            String percentDirectOfAllTime = Constants.DECIMAL_FORMATTER.format((100.0 * ct.getDirectTime())/allTime);
            String percentOfParentTime = Constants.DECIMAL_FORMATTER.format((100.0 * ct.getTotalTime())/ct.getParent().getTotalTime());
            String hours = null;
            String percentages = null;
            boolean topLevelOrComposite = ct.getParent() == ct.getRootComposite();

            if (ct.getTotalTime() == ct.getDirectTime()) // don't show composite
            {
                hours = (topLevelOrComposite ? "<font color='purple'><strong>" : "")
                		+ taskTotal
                		+ (topLevelOrComposite ? "</strong></font>" : "")
                		+ " (" + ct.getDirectContributions()
                		+ ") ";
                percentages = " [" + (topLevelOrComposite ? "<font color='purple'><strong>" : "")
                		+ percentOfAllTime + "%, " + (topLevelOrComposite ? "</strong></font>" : "")
                		+percentOfParentTime + "%]";
            }
            else
            {
                hours = (topLevelOrComposite ? "<font color='purple'><strong>" : "")
                		+ taskTotal
                		+ (topLevelOrComposite ? "</strong></font>" : "")
                		+ " [" + taskDirect
                		+ " (" + ct.getDirectContributions()
                		+ "), " + taskIndirect + "] ";
                percentages = " [" + (topLevelOrComposite ? "<font color='purple'><strong>" : "")
                		+ percentOfAllTime + "% ("
                		+ (topLevelOrComposite ? "</strong></font>" : "")
                		+ percentDirectOfAllTime + ", " + percentIndirectOfAllTime
                		+ "), " + percentOfParentTime + "%]";
            }

            // handle singular miscellaneous composite
            if (ct.getDomainObject() instanceof String)
                return "<html>" + hours + "<font color='blue'>&nbsp;\"" + ct.getDomainObject() + "\"&nbsp;</font>" + percentages + "</html>";

            // handle all others that represent specific tasks
            I_Task t = (I_Task) ct.getDomainObject();
            return "<html>" +
            		hours +
            		"<font color='blue'>&nbsp;&nbsp;" + t.getName() + "&nbsp;&nbsp;</font>" +  percentages + "</html>";
        }
    }

    private void buildUI()
    {
        // outer panel
        GridBagLayout gbl = new GridBagLayout();
        this.getContentPane().setLayout(gbl);
        GridBagConstraints cons = null;

        // period label
        cons = new GridBagConstraints(0, 0, // gridx, y
                1, 1, // gridwidth, height
                1.0, 0.0, //weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.HORIZONTAL, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        periodLabel = new JLabel("");
        gbl.setConstraints(periodLabel, cons);
        this.getContentPane().add(periodLabel);

        // scroll pane panel
        BorderLayout bl = new BorderLayout();
        treePanel = new JPanel(bl);

        cons = new GridBagConstraints(0, 1, // gridx, y
                1, 1, // gridwidth, height
                1.0, 1.0, //weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.BOTH, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        gbl.setConstraints(treePanel, cons);
        this.getContentPane().add(treePanel);

        // toolbar panel
        JPanel buttons = new JPanel(new BorderLayout());
        cons = new GridBagConstraints(0, 2, // gridx, y
                1, 1, // gridwidth, height
                1.0, 0.0, //weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.HORIZONTAL, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        gbl.setConstraints(buttons, cons);
        this.getContentPane().add(buttons);

        // previous day button
        JButton prevDay = new JButton("Prev. Day");
        buttons.add(prevDay, BorderLayout.WEST);
        prevDay.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                changePeriodDay(-1);
                //hide();
                //show(periodOfReport);
            }
        });

        // ok button
        JButton okBtn = new JButton("Done");
        buttons.add(okBtn, BorderLayout.CENTER);
        okBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                hide();
            }
        });

        // next day button
        JButton nextDay = new JButton("Next. Day");
        buttons.add(nextDay, BorderLayout.EAST);
        nextDay.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                changePeriodDay(+1);
                hide();
                show(periodOfReport);
            }
        });

    }

    /**
     * Returns the currently displayed period of the report if any.
     * @return
     */
    public Period getPeriod()
    {
        return periodOfReport;
    }

    /**
     * Changes the currently displayed period by one day in the direction
     * indicated. +1 moves the period forward in time. -1 moves the period
     * backward in time.
     *
     * @param direction
     */
    private void changePeriodDay(int direction)
    {
        Timestamp t = periodOfReport.getStart();
        t.setTime(t.getTime() + (direction * 1000L * 60 * 60 * 24));
        t = periodOfReport.getStop();
        t.setTime(t.getTime() + (direction * 1000L * 60 * 60 * 24));
        show(periodOfReport);
    }
}