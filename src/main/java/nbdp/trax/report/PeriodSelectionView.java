package nbdp.trax.report;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nbdp.trax.TimeEditor;
import nbdp.trax.calendar.CalendarEditor;
import nbdp.trax.calendar.DateHelper;
import nbdp.trax.calendar.I_DateListener;
import nbdp.trax.data.Period;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides a window for selecting the start and stop timestamp of a period for
 * which time incformation is sought.
 *
 * @author Mark Boyd
 *
 */
public class PeriodSelectionView
    extends JDialog
{
    private static final Log LOG = LogFactory.getLog(PeriodSelectionView.class);
    public static final int CANCEL_PRESSED = 0;
    public static final int OK_PRESSED = 1;

    private int buttonPressed = CANCEL_PRESSED;
    private JTextField fromDateField = null;
    private JTextField toDateField = null;
    private String dateFormat = "yyyy.MM.dd";
    private TimeEditor fromTimeEditor = null;
    private TimeEditor toTimeEditor = null;
    private SimpleDateFormat dff = new SimpleDateFormat( dateFormat );
    private CalendarEditor calendarEditor = null;
    private Frame owningFrame = null;
    protected long endPointMillis;
    protected long startPointMillis;

    public void show()
    {
        buttonPressed = CANCEL_PRESSED;
        super.show();
    }

    public PeriodSelectionView(Frame frame)
    {
        super(frame);
        this.owningFrame = frame;
        startPointMillis = DateHelper.getStartOfDay(new Date(System.currentTimeMillis())).getTime();
        endPointMillis = DateHelper.getEndOfDay(new Date(System.currentTimeMillis())).getTime();

        buildUI();
        updateView();
        setModal(true);
    }

    public static void main(String[] s)
    {
        try
        {
            System.out.println("###### Starting PeriodSelectionView Test ######");

            JFrame frame = new JFrame();
            PeriodSelectionView psv = new PeriodSelectionView(frame);
            psv.setSize(220, 220);
            psv.show();
            System.out.println("Exiting");
            System.exit(0);
        } catch (Exception e)
        {
            LOG.error("Exception occurred running TRAX.", e);
        }
    }

    /**
     * Gets the period currently displayed in the viewer.
     * @return
     */
    public Period getPeriod()
    {
        // make sure that hours/minutes in time fields are accounted for
        updatePeriod();
        return new Period(new Timestamp(startPointMillis),
                new Timestamp(endPointMillis));
    }

    /**
     * Sets the period that should be presented.
     * @param p
     */
    public void setPeriod(Period p)
    {
        if (p != null)
        {
            startPointMillis = p.getStart().getTime();
            endPointMillis = p.getStop().getTime();
            updateView();
        }
    }
    public int getButtonPressed()
    {
        return buttonPressed;
    }
    private void updateView()
    {
        fromDateField.setText(dff.format(new Date(startPointMillis)));
        fromTimeEditor.setTime(startPointMillis);
        toDateField.setText(dff.format(new Date(endPointMillis)));
        toTimeEditor.setTime(endPointMillis);
    }

    private void updatePeriod()
    {
        GregorianCalendar pointInTime = new GregorianCalendar();
        pointInTime.setTime(new Date(startPointMillis));
        pointInTime.set(Calendar.HOUR_OF_DAY, fromTimeEditor.getHourOfDay());
        pointInTime.set(Calendar.MINUTE, fromTimeEditor.getMinutes());
        Date startDate = pointInTime.getTime();
        startPointMillis = startDate.getTime();

        pointInTime.setTime(new Date(endPointMillis));
        pointInTime.set(Calendar.HOUR_OF_DAY, toTimeEditor.getHourOfDay());
        pointInTime.set(Calendar.MINUTE, toTimeEditor.getMinutes());
        Date endDate = pointInTime.getTime();
        endPointMillis = endDate.getTime();
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

    // if needed at some point add JLabel here showing, "Select Period.

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

    // From label
    cons = new GridBagConstraints( 0, 0, // gridx, y
               1, 1, // gridwidth, height
               0.0, 0.0, //weightx, y
               GridBagConstraints.WEST, // anchor
               GridBagConstraints.NONE, // fill
               new Insets(0,0,0,0), // insets
               0,0 ); // ipadx, y
    JLabel from = new JLabel("From: ");
    gbl.setConstraints(from, cons);
    p.add(from);

	// From Date label
	cons = new GridBagConstraints( 0, 1, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.EAST, // anchor
				       GridBagConstraints.NONE, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	JLabel date = new JLabel("Date: ");
	gbl.setConstraints(date, cons);
	p.add(date);

    // ---------- start of from date field panel

    // From Date field panel
	JPanel fromDateFieldPanel = new JPanel();
	GridBagLayout fromDateFieldLayout = new GridBagLayout();
	fromDateFieldPanel.setLayout( fromDateFieldLayout );

	cons = new GridBagConstraints( 1, 1, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,5,0,5), // insets
				       0,0 ); // ipadx, y
	gbl.setConstraints(fromDateFieldPanel, cons);
	p.add(fromDateFieldPanel);

	// from date field
	cons = new GridBagConstraints( 0, 0, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,0,5), // insets
				       0,0 ); // ipadx, y
	fromDateField = new JTextField( "" );
	fromDateField.setEditable( false );
	fromDateFieldLayout.setConstraints(fromDateField, cons);
	fromDateFieldPanel.add(fromDateField);

	// from Date Edit button
	cons = new GridBagConstraints( 1, 0, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.NONE, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y

	JButton fromEditDate = new JButton("...");
	fromEditDate.setMargin( new Insets( 0,0,0,0 ) );
	fromDateFieldLayout.setConstraints(fromEditDate, cons);
	fromDateFieldPanel.add(fromEditDate);
	fromEditDate.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                if (calendarEditor == null)
                {
                    calendarEditor = new CalendarEditor((JFrame) owningFrame);
                }
                // make sure that we use a date reflecting the hours/minutes
                // set in the time field
                updatePeriod();
                calendarEditor.show(new Date(startPointMillis));
                if (calendarEditor.getButtonPressed() == CalendarEditor.OK_PRESSED)
                {
                    Date newDt = calendarEditor.getDate();
                    startPointMillis = newDt.getTime();
                    updateView();
                }
            }
        });
    // ---------- end of from date field panel

	// from Time label
	cons = new GridBagConstraints( 0, 2, // gridx, y
				       1, 1, // gridwidth, height
				       0.0, 0.0, //weightx, y
				       GridBagConstraints.EAST, // anchor
				       GridBagConstraints.NONE, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y

	JLabel fromTimeLabel = new JLabel("Time: ");
	gbl.setConstraints(fromTimeLabel, cons);
	p.add(fromTimeLabel);

	// From Time
	cons = new GridBagConstraints( 1, 2, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(5,5,5,5), // insets
				       0, 0); // ipadx, y

	fromTimeEditor = new TimeEditor();
    I_DateListener fromListener = new I_DateListener()
    {
        public void dateChanged(Date newDate)
        {
            startPointMillis = newDate.getTime();
            updateView();
        }
    };
    fromTimeEditor.setDateListener(fromListener);
	fromTimeEditor.setTime( new Date() );
	gbl.setConstraints(fromTimeEditor, cons);
	p.add(fromTimeEditor);

    ////////////// start of "TO" editing controls

    // To label
    cons = new GridBagConstraints( 0, 3, // gridx, y
               2, 1, // gridwidth, height
               0.0, 0.0, //weightx, y
               GridBagConstraints.WEST, // anchor
               GridBagConstraints.NONE, // fill
               new Insets(0,0,0,0), // insets
               0,0 ); // ipadx, y
    JLabel to = new JLabel("To: ");
    gbl.setConstraints(to, cons);
    p.add(to);

    // Date label
    cons = new GridBagConstraints( 0, 4, // gridx, y
                       1, 1, // gridwidth, height
                       0.0, 0.0, //weightx, y
                       GridBagConstraints.EAST, // anchor
                       GridBagConstraints.NONE, // fill
                       new Insets(0,0,0,0), // insets
                       0,0 ); // ipadx, y
    JLabel toDate = new JLabel("Date: ");
    gbl.setConstraints(toDate, cons);
    p.add(toDate);

    // ---------- start of date field panel

    // Date field panel
    JPanel toDateFieldPanel = new JPanel();
    GridBagLayout toDateFieldLayout = new GridBagLayout();
    toDateFieldPanel.setLayout( toDateFieldLayout );

    cons = new GridBagConstraints( 1, 4, // gridx, y
                       1, 1, // gridwidth, height
                       1.0, 0.0, //weightx, y
                       GridBagConstraints.CENTER, // anchor
                       GridBagConstraints.HORIZONTAL, // fill
                       new Insets(0,5,0,5), // insets
                       0,0 ); // ipadx, y
    gbl.setConstraints(toDateFieldPanel, cons);
    p.add(toDateFieldPanel);

    // date field
    cons = new GridBagConstraints( 0, 0, // gridx, y
                       1, 1, // gridwidth, height
                       1.0, 0.0, //weightx, y
                       GridBagConstraints.CENTER, // anchor
                       GridBagConstraints.HORIZONTAL, // fill
                       new Insets(0,0,0,5), // insets
                       0,0 ); // ipadx, y
    toDateField = new JTextField( "" );
    toDateField.setEditable( false );
    toDateFieldLayout.setConstraints(toDateField, cons);
    toDateFieldPanel.add(toDateField);

    // Date Edit button
    cons = new GridBagConstraints( 1, 0, // gridx, y
                       1, 1, // gridwidth, height
                       0.0, 0.0, //weightx, y
                       GridBagConstraints.CENTER, // anchor
                       GridBagConstraints.NONE, // fill
                       new Insets(0,0,0,0), // insets
                       0,0 ); // ipadx, y

    JButton toEditDate = new JButton("...");
    toEditDate.setMargin( new Insets( 0,0,0,0 ) );
    toDateFieldLayout.setConstraints(toEditDate, cons);
    toDateFieldPanel.add(toEditDate);
    toEditDate.addActionListener( new ActionListener()
        {
        public void actionPerformed( ActionEvent ae )
        {
            if( calendarEditor == null )
            {
                calendarEditor = new CalendarEditor( (JFrame)owningFrame );
            }
            // make sure that we use a date reflecting the hours/minutes
            // set in the time field
            updatePeriod();
            calendarEditor.show(new Date(endPointMillis));
            if ( calendarEditor.getButtonPressed() ==
             CalendarEditor.OK_PRESSED )
            {
            Date newDt = calendarEditor.getDate();
            endPointMillis = newDt.getTime();
            updateView();
            }
        }
        });
    // ---------- end of date field panel

    // Time label
    cons = new GridBagConstraints( 0, 5, // gridx, y
                       1, 1, // gridwidth, height
                       0.0, 0.0, //weightx, y
                       GridBagConstraints.EAST, // anchor
                       GridBagConstraints.NONE, // fill
                       new Insets(0,0,0,0), // insets
                       0,0 ); // ipadx, y

    JLabel toTimeLabel = new JLabel("Time: ");
    gbl.setConstraints(toTimeLabel, cons);
    p.add(toTimeLabel);

    // Time
    cons = new GridBagConstraints( 1, 5, // gridx, y
                       1, 1, // gridwidth, height
                       1.0, 0.0, //weightx, y
                       GridBagConstraints.CENTER, // anchor
                       GridBagConstraints.HORIZONTAL, // fill
                       new Insets(5,5,5,5), // insets
                       0, 0); // ipadx, y

    toTimeEditor = new TimeEditor();
    I_DateListener toListener = new I_DateListener()
    {
        public void dateChanged(Date newDate)
        {
            endPointMillis = newDate.getTime();
            updateView();
        }
    };
    toTimeEditor.setDateListener(toListener);
    toTimeEditor.setTime( new Date() );
    gbl.setConstraints(toTimeEditor, cons);
    p.add(toTimeEditor);

    }
}
