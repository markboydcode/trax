package nbdp.trax;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;

import java.util.*;
import java.text.*;

public class TimelineReporter
    extends JDialog
{
    private UI_TaskManager taskManager = null;
    private static JFrame frame = null;
    private TaskSummary[] summaries = null;
    private static final TaskSummary[] SUMMARIES_ARRAY = new TaskSummary[0];
    private static final String dateFormat = "yyyy.MM.dd";
    private SimpleDateFormat formatter = new SimpleDateFormat( dateFormat );
    private JLabel periodLabel = null;
    private AbstractTableModel dataModel = null;
    
    public TimelineReporter( UI_TaskManager tmgr, JFrame frame )
    {
	this.taskManager = tmgr;
	TimelineReporter.frame = frame;
	setModal( true );
	buildUI();
    }
    private class TaskSummary
    {
	int hours = 0;
	int minutes = 0;
	String desc = "";
    }
    public void show()
    {
	// don't show if called. must use version that takes a linked list.
    }
    public void show( LinkedList slices )
    {
	summaries = null;
	long earliest = -1;
	long latest = -1;
	
	if ( slices.size() != 0 )
	{
	    TreeMap table = new TreeMap();
	    ListIterator iter = slices.listIterator();
	    TaskSummary total = new TaskSummary();
	    
	    while( iter.hasNext() )
	    {
		I_Timeslice slice = (I_Timeslice) iter.next();
		
		if ( earliest == -1 )
		    earliest = slice.getStart().getTime();
		else if ( slice.getStart().getTime() < earliest )
		    earliest = slice.getStart().getTime();
		if ( slice.getStart().getTime() > latest )
		    latest = slice.getStart().getTime();
		
		if ( slice.getDuration() <= 0 ) // skip if without time
		{
		    System.out.println( "skipping since no time " +
					slice.toString() );
		    continue;
		}
		addTime( total, slice.getDuration() );

		//String taskId = slice.getTaskId();
		if ( slice.getTaskId() != I_Task.NO_TASK_ID )
		{
		    // see if already there.
		    TaskSummary summary = (TaskSummary) table.get( "" + slice.getTaskId() );
		    if ( summary == null )
		    {
			summary = new TaskSummary();
			table.put( "" + slice.getTaskId(), summary );
            ServiceLocator locator = ServiceLocator.getInstance();
            I_TraxDao dao = locator.getDAO();
            String taskName = dao.getTaskName(slice.getTaskId());

            if ( taskName == null ) // couldn't find
			    summary.desc = "T_ID: " + slice.getTaskId();
			else
			    summary.desc = "T: " + taskName;
		    }
		    addTime( summary, slice );
		}
		else if ( slice.getNote() != null &&
			  ! slice.getNote().equals( "" ) )
		{
		    // add times for tasks with identical notes
		    String notes = slice.getNote();
		    TaskSummary summary = (TaskSummary) table.get( notes );
		    if ( summary == null )
		    {
			summary = new TaskSummary();
			summary.desc = notes;
			table.put( notes, summary );
		    }
		    addTime( summary, slice );
		}
		else // add to miscellany
		{
		    // see if already there.
		    TaskSummary summary = (TaskSummary) table.get( "" );
		    if ( summary == null )
		    {
			summary = new TaskSummary();
			summary.desc = "";
			table.put( "", summary );
		    }
		    addTime( summary, slice );
		}
	    }
	    summaries = (TaskSummary[]) table.values().toArray( SUMMARIES_ARRAY );
	    // set up title
	    periodLabel.setText( "Total: " + total.hours + " hrs. " +
				 total.minutes + " min. from " +
				 formatter.format( new Date( earliest ) ) +
				 " to " +
				 formatter.format( new Date( latest ) ) );
	}
	else
	    periodLabel.setText( "No Timeline Values to Display" );
	if ( dataModel != null )
	    dataModel.fireTableStructureChanged();
        super.show();
    }
    private void addTime( TaskSummary summary, I_Timeslice slice )
    {
	addTime( summary, slice.getDuration() );
    }
    private void addTime( TaskSummary summary, long dur )
    {
	if ( dur > 0 )
        {
	    long hours = dur / 3600000L;
	    long minutes = (dur - (hours * 3600000L))/60000;
	    summary.hours += (int) hours;
	    summary.minutes += (int) minutes;
	    if ( summary.minutes > 59 )
            {
		summary.hours++;
		summary.minutes -= 60;
	    }
	}
    }
    private void buildUI()
    {
	final GridLayout listLayout = new GridLayout( 1, 1 );
	TableModel dataModel = new AbstractTableModel()
	    {
		public int getColumnCount()
		{
		    return 2;
		}
		public int getRowCount()
		{
		    if ( summaries == null )
			return 0;
		    return summaries.length;
		}
		public String getColumnName( int col )
		{
		    switch (col)
		    {
		    case 0: return "Task";
		    case 1: return "Time Applied";
		    }
		    return "";
		}
		public Object getValueAt( int row, int col )
		{
		    if ( summaries == null )
			return "";
		    
		    if ( col == 0 )
			return summaries[row].desc;
		    else if ( col == 1 )
			return "" + summaries[row].hours + ":" +
			    summaries[row].minutes;
		    return "";
		}
		
	    };
	final JTable table = new JTable( dataModel );
	final JScrollPane scrollPane = new JScrollPane( table );

	// outer panel
	GridBagLayout gbl = new GridBagLayout();
	this.getContentPane().setLayout( gbl );
	GridBagConstraints cons = null;

	// period label
	cons = new GridBagConstraints( 0, 0, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	periodLabel = new JLabel( "" );
	gbl.setConstraints(periodLabel, cons);
	this.getContentPane().add(periodLabel);

	// scroll pane housing list
	cons = new GridBagConstraints( 0, 1, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 1.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.BOTH, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	gbl.setConstraints(scrollPane, cons);
	this.getContentPane().add(scrollPane);

	// toolbar panel
	JPanel buttons = new JPanel();
	cons = new GridBagConstraints( 0, 2, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	gbl.setConstraints(buttons, cons);
	this.getContentPane().add(buttons);

	// ok button
	JButton okBtn = new JButton( "OK" );
	buttons.add( okBtn );
	okBtn.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent ae )
		{
		    hide();
		}
	    });
   }
}
