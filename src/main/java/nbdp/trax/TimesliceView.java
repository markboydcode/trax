package nbdp.trax;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import nbdp.trax.TLFileOpenDialog.ComboBoxRenderer;
import nbdp.trax.calendar.CalendarEditor;
import nbdp.trax.calendar.I_DateListener;
import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;

import java.sql.Timestamp;
import java.text.*;
import java.util.*;

/**
 * Dialog that allows for editing values for a time slice including the date
 * and time at the start of the slice, the task, its type, and notes for the
 * slice.
 *
 * @author Mark Boyd
 * @copyright: Copyright, 2008, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class TimesliceView
    extends JDialog
{
    private static final Log LOG = LogFactory.getLog(TimesliceView.class);
    private UI_TaskManager taskManager = null;
    public static final int CANCEL_PRESSED = 0;
    public static final int OK_PRESSED = 1;

    private int buttonPressed = CANCEL_PRESSED;
    private I_Timeslice timeslice = null;
    private JTextField dateField = null;
    private JTextField taskField = new JTextField( "" );
    private JComboBox typeField = new JComboBox();
    private String dateFormat = "yyyy.MM.dd";
    private TimeEditor timeEditor = null;
    private SimpleDateFormat dff = new SimpleDateFormat( dateFormat );
    private CalendarEditor calendarEditor = null;
    private JFrame owningFrame = null;
    private JTextArea note = null;

    public TimesliceView( UI_TaskManager tm,
			  JFrame frame )
    {
	super( frame );
	this.owningFrame = frame;
	this.taskManager = tm;

    I_TraxDao dao = ServiceLocator.getInstance().getDAO();
    I_Type[] types = dao.getTypes();
    I_Type misc = null;

	for( int i=0; i<types.length; i++)
    {
        if (types[i].getId() == I_Type.MISC_TYPE_ID)
            misc = types[i];
	    typeField.addItem( types[i] );
    }
    if (misc != null)
	typeField.setSelectedItem(misc);

	buildUI();
	setModal( true );
    }

    public void show(I_Timeslice slice)
    {
        timeslice = slice;
        ServiceLocator locator = ServiceLocator.getInstance();
        I_TraxDao dao = locator.getDAO();

        if (slice.getNote() == null)
            note.setText("");
        else
            note.setText(slice.getNote());

        I_Task t = null;

        if (slice.getTaskId() != I_Task.NO_TASK_ID)
        {
            t = dao.getTask(slice.getTaskId());
        }

        if (t != null)
            taskField.setText(t.getName());
        else
            taskField.setText("");

        // refresh the type list in case new types were added
        typeField.removeAllItems();
        I_Type[] types = dao.getTypes();
        I_Type selectedType = null;
        int typeId = slice.getTypeId();
        for (int i = 0; i < types.length; i++)
        {
            typeField.addItem(types[i]);
            if (types[i].getId() == typeId)
                selectedType = types[i];
        }
        if (selectedType != null)
            typeField.setSelectedItem(selectedType);

        timeEditor.setTime(slice.getStart());
        updateView();
        buttonPressed = CANCEL_PRESSED; // by default so closing window counts
        super.show();
        if (buttonPressed != CANCEL_PRESSED)
            updateTimeslice();
    }
    public void show()
    {
        throw new UnsupportedOperationException();
    }
    public I_Timeslice getTimeSlice()
    {
	return timeslice;
    }

    public int getButtonPressed()
    {
        return buttonPressed;
    }

    private static void usage()
    {
        System.out.println( "\nusage: java Timeslice -db DBpath" );
        System.out.println( "\nTo specify the current directory use "
                            + "--> java Timeslice \"\"\n" );
        System.exit( 0 );
    }

    public static void main(String[] s)
    {
        try
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("###### Starting TimesliceView Test ######");
            }
            if (s.length == 0 || (s.length > 0 && s[0].equals("?"))
                    || !s[0].equals("-db")
                    || (s[0].equals("-db") && s.length == 1))
                usage();

            JFrame frame = new JFrame();
            UI_TaskManager manager = new UI_TaskManager(frame);
            TimesliceView tv = new TimesliceView(manager, frame);
            tv.setSize(220, 300);
            tv.show();
            System.out.println("Button pressed: "
                    + (tv.getButtonPressed() == CANCEL_PRESSED ? "Cancel"
                            : "OK"));
            System.exit(0);
        } catch (Exception e)
        {
            LOG.error("Exception occurred running TRAX.", e);
        }
    }

    private void updateView()
    {
        dateField.setText(dff.format(new Date(timeslice.getStart().getTime())));
    }

    private void updateTimeslice()
    {
        GregorianCalendar pointInTime = new GregorianCalendar();
        pointInTime.setTime(new Date(timeslice.getStart().getTime()));
        pointInTime.set(Calendar.HOUR_OF_DAY, timeEditor.getHourOfDay());
        pointInTime.set(Calendar.MINUTE, timeEditor.getMinutes());
        timeslice.setStart(new Timestamp(pointInTime.getTime().getTime()));

        String noteText = note.getText();

        if (noteText == null || noteText.equals(""))
            timeslice.setNote(null);
        else
            timeslice.setNote(note.getText());

        I_Type type = (I_Type) typeField.getSelectedItem();
        timeslice.setTypeId(type.getId());
    }

    private void buildUI()
    {
	// outer panel
	JPanel main = new JPanel();
	getContentPane().add( main );
	main.setBorder( BorderFactory.createEmptyBorder( 4,4,4,4 ) );
	GridBagLayout gbl = new GridBagLayout();
	main.setLayout( gbl );
	GridBagConstraints cons = null;

	// upper panel with editing fields
	cons = new GridBagConstraints( 0, 0, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 1.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.BOTH, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	JPanel pane = new JPanel();
	gbl.setConstraints(pane, cons);
	main.add(pane);

	createEditPanel( pane );

	// toolbar panel
	JPanel buttons = new JPanel();
	buttons.setLayout( new GridLayout( 1, 2 ) );
	cons = new GridBagConstraints( 0, 1, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	gbl.setConstraints(buttons, cons);
	main.add(buttons);

	// ok button
	JButton okBtn = new JButton( "OK" );
	buttons.add( okBtn );
	okBtn.setMargin( new Insets( 5,5,5,5 ) );
        okBtn.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    buttonPressed = OK_PRESSED;
                    hide();
                }
            });

	// cancel button
	JButton cancelBtn = new JButton( "Cancel" );
	buttons.add( cancelBtn );
	cancelBtn.setMargin( new Insets( 5,5,5,5 ) );
        cancelBtn.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    hide();
                }
            });

    }

    private void createEditPanel( JPanel p )
    {
	GridBagLayout gbl = new GridBagLayout();
	p.setLayout( gbl );
	GridBagConstraints cons = null;

	// Date label
	cons = new GridBagConstraints( 0, 0, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.EAST, // anchor
				       GridBagConstraints.NONE, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	JLabel date = new JLabel("Date: ");
	gbl.setConstraints(date, cons);
	p.add(date);

	// Date field panel
	JPanel dateFieldPanel = new JPanel();
	GridBagLayout dateFieldLayout = new GridBagLayout();
	dateFieldPanel.setLayout( dateFieldLayout );

	cons = new GridBagConstraints( 1, 0, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,5,0,5), // insets
				       0,0 ); // ipadx, y
	gbl.setConstraints(dateFieldPanel, cons);
	p.add(dateFieldPanel);

	// date field
	cons = new GridBagConstraints( 0, 0, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,0,5), // insets
				       0,0 ); // ipadx, y
	dateField = new JTextField( "" );
	dateField.setEditable( false );
	dateFieldLayout.setConstraints(dateField, cons);
	dateFieldPanel.add(dateField);

	// Date Edit button
	cons = new GridBagConstraints( 1, 0, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.NONE, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y

	JButton editDate = new JButton("...");
	editDate.setMargin( new Insets( 0,0,0,0 ) );
	dateFieldLayout.setConstraints(editDate, cons);
	dateFieldPanel.add(editDate);
	editDate.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent ae )
		{
		    if( calendarEditor == null )
			calendarEditor = new CalendarEditor( owningFrame );
		    calendarEditor.show( new Date( timeslice.getStart().getTime() ) );
		    if ( calendarEditor.getButtonPressed() ==
			 CalendarEditor.OK_PRESSED )
		    {
			Date newDt = calendarEditor.getDate();
			long millis = newDt.getTime();
			timeslice.setStart( new Timestamp(millis) );
			updateView();
		    }
		}
	    });

	// Time label
	cons = new GridBagConstraints( 0, 1, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.EAST, // anchor
				       GridBagConstraints.NONE, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y

	JLabel timeLabel = new JLabel("Time: ");
	gbl.setConstraints(timeLabel, cons);
	p.add(timeLabel);

	// Time
	cons = new GridBagConstraints( 1, 1, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(5,5,5,5), // insets
				       0, 0); // ipadx, y

	timeEditor = new TimeEditor();
    I_DateListener listener = new I_DateListener()
    {
        public void dateChanged(Date newDate)
        {
            timeslice.setStart(new Timestamp(newDate.getTime()));
            updateView();
        }
    };
    timeEditor.setDateListener(listener);
	timeEditor.setTime( new Date() );
	gbl.setConstraints(timeEditor, cons);
	p.add(timeEditor);

	// Task label
	cons = new GridBagConstraints( 0, 2, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.EAST, // anchor
				       GridBagConstraints.BOTH, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y

	JLabel taskLabel = new JLabel("Task: ");
	gbl.setConstraints(taskLabel, cons);
	p.add(taskLabel);

	// Task field panel
	JPanel taskFieldPanel = new JPanel();
	GridBagLayout taskFieldLayout = new GridBagLayout();
	taskFieldPanel.setLayout( taskFieldLayout );

	cons = new GridBagConstraints( 1, 2, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,5,0,5), // insets
				       0,0 ); // ipadx, y
	gbl.setConstraints(taskFieldPanel, cons);
	p.add(taskFieldPanel);

	// task field
	cons = new GridBagConstraints( 0, 0, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,0,5), // insets
				       0,0 ); // ipadx, y
	taskField.setEditable( false );
	taskFieldLayout.setConstraints(taskField, cons);
	taskFieldPanel.add(taskField);

	// task Edit button
	cons = new GridBagConstraints( 1, 0, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.NONE, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y

	JButton editTask = new JButton("...");
	editTask.setMargin( new Insets( 0,0,0,0 ) );
	taskFieldLayout.setConstraints(editTask, cons);
	taskFieldPanel.add(editTask);
	editTask.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent ae )
		{
		    taskManager.setMode( UI_TaskManager.SELECT_TASK );
            if (timeslice.getTaskId() != I_Task.NO_TASK_ID)
            {
                taskManager.setSelectedTask(timeslice.getTaskId());
            }
		    taskManager.show();

		    if ( taskManager.getButtonPressed() ==
			 UI_TaskManager.CANCEL_PRESSED )
			return;

		    I_Task t = taskManager.getSelectedTask();
		    if ( t == null )
		    {
		        taskField.setText( "" );
		        timeslice.setTaskId( I_Task.NO_TASK_ID );
		        return;
		    }
		    timeslice.setTaskId( t.getId() );
		    taskField.setText( t.getName() );

            I_Type selectedType = (I_Type) typeField.getSelectedItem();

            // allow overriding of timeslice's type only if it is misc so that
            // if a slice has already been marked with some specific type it
            // doesn't get clobbered.
            if (selectedType != null && selectedType.getId() == I_Type.MISC_TYPE_ID)
            {
                I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                typeField.setSelectedItem( dao.getTypeById( t.getTypeId() ) );
            }
		}
	    });

	// Type label
	cons = new GridBagConstraints( 0, 3, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.EAST, // anchor
				       GridBagConstraints.BOTH, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y

	JLabel label = new JLabel("Type: ");
	gbl.setConstraints(label, cons);
	p.add(label);

	// type field

    TypeFieldRenderer renderer = new TypeFieldRenderer();
    typeField.setRenderer(renderer);
	cons = new GridBagConstraints( 1, 3, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(5,5,0,5), // insets
				       0,0 ); // ipadx, y
        typeField.setFont( taskField.getFont() );
	gbl.setConstraints(typeField, cons);
	p.add(typeField);

	// notes label
	cons = new GridBagConstraints( 0, 4, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.NORTH, // anchor
				       GridBagConstraints.NONE, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y

	JLabel noteLabel = new JLabel("Note: ");
	gbl.setConstraints(noteLabel, cons);
	p.add(noteLabel);

	// notes entry
	cons = new GridBagConstraints( 1, 4, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 1.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.BOTH, // fill
				       new Insets(5,5,5,5), // insets
				       0, 0); // ipadx, y

	note = new JTextArea();
	note.setLineWrap( true );
	JScrollPane pane = new JScrollPane( note );
	pane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
	gbl.setConstraints(pane, cons);
	p.add(pane);

    }
}
