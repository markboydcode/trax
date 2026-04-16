package nbdp.trax;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import nbdp.trax.calendar.CalendarView;
import nbdp.trax.calendar.DateHelper;
import nbdp.trax.calendar.I_CalendarEvaluator;
import nbdp.trax.calendar.I_CalendarEvaluatorFactory;
import nbdp.trax.calendar.I_CalendarListener;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.Period;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TLFileOpenDialog
    extends JDialog
{
    private CalendarView calendar = null;
    private JComboBox slices = null;
    private ComboBoxRenderer slicesRenderer = null;
    private JButton okBtn = null;
    
    public static final int CANCEL_PRESSED = 0;
    public static final int OK_PRESSED = 1;
    private static final SimpleDateFormat timeInDay = 
        new SimpleDateFormat("hh:mm:ss a");
    private static final SimpleDateFormat timeNotInDay = 
        new SimpleDateFormat("MMM dd, hh:mm:ss a");
    
    private int buttonPressed = CANCEL_PRESSED;
    private Helper helper = new Helper();


    
    class ComboBoxRenderer extends JLabel implements ListCellRenderer
    {
        private int dayOfMonth = 0;
        
        void setDayOfMonth(int day)
        {
            this.dayOfMonth = day;
        }
        public ComboBoxRenderer()
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
            I_Timeline line = (I_Timeline) value;
            if (line == null)
                setText("");
            else
            {
                if (isSelected)
                {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else
                {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }

                Date lineDate = new Date(line.getStart().getTime());
                GregorianCalendar lineStartPoint = new GregorianCalendar();
                lineStartPoint.setTime(lineDate);
                int lineDay = lineStartPoint.get(Calendar.DAY_OF_MONTH);

                if (lineDay == dayOfMonth)
                    setText(timeInDay.format(lineDate));
                else
                    setText(timeNotInDay.format(lineDate));
            }
            return this;
        }
    }    
    
    
    
    
    
    
    public TLFileOpenDialog( JFrame owner )
    {
        super(owner);
        setModal(true);
        buildUI();
        setSize(175, 225);
    }
    public void show()
    {
        buttonPressed = CANCEL_PRESSED; // by default so closing window counts
        super.show();
    }
    public int getButtonPressed()
    {
        return buttonPressed;
    }
    
    /*
    public String getTLFilePath()
    {
	FileInfoItem item = (FileInfoItem) slices.getSelectedItem();
	
	if ( item == null )
	    return null;
	
	return item.fileInfo.path;
    }
    */
    public I_Timeline getSelectedTimeline()
    {
        return (I_Timeline) slices.getSelectedItem();
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

	// upper panel with calendar view
	cons = new GridBagConstraints( 0, 0, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 1.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.BOTH, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	calendar = new CalendarView( helper );
	calendar.addListener( helper );
	gbl.setConstraints(calendar, cons);
	main.add(calendar);
	
	// combo box for selecting from multiple timeslice files on a single
	// day.
	cons = new GridBagConstraints( 0, 1, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,5,0), // insets
				       0,0 ); // ipadx, y
        slices = new JComboBox();
        slicesRenderer = new ComboBoxRenderer();
        slices.setRenderer(slicesRenderer);
	slices.setEnabled( false );
	gbl.setConstraints(slices, cons);
	main.add( slices );
	
	// toolbar panel
	JPanel buttons = new JPanel();
	buttons.setLayout( new GridLayout( 1, 2 ) );
	cons = new GridBagConstraints( 0, 2, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	gbl.setConstraints(buttons, cons);
	main.add(buttons);

	// ok button
	okBtn = new JButton( "OK" );
	okBtn.setEnabled( false );
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
	helper.loadMonthTimelines( calendar.getDate() );
	helper.updateSlicesList( calendar.getDayOfMonth() );
    }

    /*
    private class FileInfoItem
    {
	TLFinder.FileInfo fileInfo = null;

	FileInfoItem( TLFinder.FileInfo fi )
	{
	    this.fileInfo = fi;
	}
	public String toString()
	{
	    return fileInfo.name;
	}
    }
    */
    private class Helper
	implements I_CalendarEvaluatorFactory,
		   I_CalendarEvaluator,
		   I_CalendarListener
    {
	private Vector[] timelinesOnDay = new Vector[32];
	private GregorianCalendar gCal = new GregorianCalendar();
	private GregorianCalendar gStartOfDay = new GregorianCalendar();
	private GregorianCalendar gEndOfDay = new GregorianCalendar();
	
	///// I_CalendarListener methods
	public void itemSelected( I_CalendarListener.Item item,
				  int dayOfMonth,
				  Date date )
	{
	    updateSlicesList( dayOfMonth );
	}
	
	private void updateSlicesList( int dayOfMonth )
	{
	    slices.removeAllItems();
        slicesRenderer.setDayOfMonth(dayOfMonth);

	    if ( timelinesOnDay[ dayOfMonth ] == null ||
		 timelinesOnDay[ dayOfMonth ].size() == 0 )
	    {
		slices.setEnabled( false );
		okBtn.setEnabled( false );
		return;
	    }
	    slices.setEnabled( true );
	    okBtn.setEnabled( true );
	    
	    for ( Iterator i=timelinesOnDay[ dayOfMonth ].iterator();
		  i.hasNext(); )
	    {
            I_Timeline line = (I_Timeline) i.next();
	        slices.addItem(line);
	    }
	}
    
	///// I_CalendarEvaluatorFactory methods
	public I_CalendarEvaluator getEvaluator( Date date )
	{
	    loadMonthTimelines( date );
	    return this;
	}
	private void loadMonthTimelines( Date date )
	{
	    gCal.setTime( date );
	    int daysInMonth = gCal.getActualMaximum( Calendar.DAY_OF_MONTH );
	    gCal.set( GregorianCalendar.DAY_OF_MONTH, 1 );
	    Date startOfFirstDOM = DateHelper.getStartOfDay( gCal.getTime() );
	    gCal.set( GregorianCalendar.DAY_OF_MONTH, daysInMonth );
	    Date endOfLastDOM = DateHelper.getEndOfDay( gCal.getTime() );
	    
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();

	    gStartOfDay.setTime( startOfFirstDOM );
	    gEndOfDay.setTime( endOfLastDOM );
	    
	    for (int i = 1; i <= daysInMonth; i++)
        {
            gStartOfDay.set(Calendar.DAY_OF_MONTH, i);
            Date startOfDay = gStartOfDay.getTime();
            gEndOfDay.set(Calendar.DAY_OF_MONTH, i);
            Date endOfDay = gEndOfDay.getTime();

            if (timelinesOnDay[i] != null)
                timelinesOnDay[i].clear();

            Period period = new Period(new Timestamp(startOfDay.getTime()),
                    new Timestamp(endOfDay.getTime()));
            List lines = dao.getTimelinesInPeriod(period);
            for (Iterator iter = lines.iterator(); iter.hasNext();)
            {
                I_Timeline line = (I_Timeline) iter.next();
                if (timelinesOnDay[i] == null)
                    timelinesOnDay[i] = new Vector();
                timelinesOnDay[i].add(line);
            }
        }
	}

	///// I_CalendarEvaluator methods

	private Date start = new Date( Long.MIN_VALUE );
	private Date end = new Date( Long.MAX_VALUE );
	
	public Date getStart()
	{
	    return start;
	}

	public Date getEnd()
	{
	    return end;
	}

	public boolean isDayEnabled( int dayOfMonth )
	{
	    return timelinesOnDay[ dayOfMonth ] != null &&
		timelinesOnDay[ dayOfMonth ].size() != 0;
	}

	public Color getDayTextColor( int dayOfMonth )
	{
	    if ( timelinesOnDay[ dayOfMonth ] == null ||
		 timelinesOnDay[ dayOfMonth ].size() == 0 )
	        return Color.black;
	    
        if ( timelinesOnDay[ dayOfMonth ].size() > 1 )
            return Color.red;
        
        // only one line, what kind of suff does it have? first populate slices
        I_Timeline line = (I_Timeline) timelinesOnDay[ dayOfMonth ].get(0);
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        dao.getSlices(line);
        List slices = line.getSlices();
        boolean hasVacation = false;
        boolean hasSickleave = false;
        boolean hasHoliday = false;

        for(Iterator itr=slices.iterator(); itr.hasNext();)
        {
            I_Timeslice slice = (I_Timeslice) itr.next();
            // TODO fix hardcoding by adding well known name column to types 
            // table and provide unique values for items of interest like these
            // then obtain their ids via the well known names.
            if (slice.getTypeId() == 19)
            {
                hasSickleave = true;
            }
            else if (slice.getTypeId() == 16)
            {
                hasVacation = true;
            }
            else if (slice.getTypeId() == 17)
            {
                hasHoliday = true;
            }
        }
        if (hasVacation && ! hasSickleave && ! hasHoliday)
	        return Color.green;
        if (hasSickleave && ! hasVacation && ! hasHoliday)
            return Color.magenta;
        if (hasHoliday && ! hasVacation && ! hasSickleave)
            return Color.yellow;
	    if (hasHoliday || hasVacation || hasSickleave)
            return Color.orange;
	    return Color.blue;
	}
    }

    public static void main( String[] args )
    throws Exception
    {
    ConfigurableApplicationContext ctx = SpringApplication.run(TraxApplication.class, new String[0]);
    ctx.getBean(ServiceLocator.class); // ensures DAO is wired
    
	TLFileOpenDialog d = new TLFileOpenDialog( new JFrame() );
	d.setSize( 300, 300 );
	d.show();
    I_Timeline line = d.getSelectedTimeline();
    
	if ( d.getButtonPressed() == OK_PRESSED )
    {
        if (line == null)
            System.out.println( "OK pressed. No timeline selected." );
        else
        {
            Date lineDate = new Date(line.getStart().getTime());
            System.out.println( "OK pressed. Selected: " +
                    timeNotInDay.format(lineDate));
        }
    }
	else if ( d.getButtonPressed() == CANCEL_PRESSED )
	    System.out.println( "CANCEL pressed" );
	else
	    System.out.println( "No button pressed?" );
    }
}
