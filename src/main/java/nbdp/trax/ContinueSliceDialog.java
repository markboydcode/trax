package nbdp.trax;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;

import java.text.*;
import java.util.*;

public class ContinueSliceDialog
    extends JDialog
{
    private UI_TaskManager taskManager = null;
    private static JFrame frame = null;
    private LinkedList list = null;
    private I_Timeslice selectedSlice = null;
    AbstractTableModel dataModel = null;

    public static final int CANCEL_PRESSED = 0;
    public static final int OK_PRESSED = 1;
    
    private int buttonPressed = CANCEL_PRESSED;

    public ContinueSliceDialog( UI_TaskManager tm,
				JFrame frame )
    {
	this.taskManager = tm;
	setFrame( frame );
	list = new LinkedList();
	setModal( true );
	buildUI();
    }

	private static void setFrame( JFrame f )
	{		
		frame = f;
	}	
	private static JFrame getFrame()
	{
	 	return frame;
	}
    public void show( LinkedList list )
    {
	this.list = list;
	dataModel.fireTableStructureChanged();
	selectedSlice = null;
        buttonPressed = CANCEL_PRESSED; // by default so closing window counts
        super.show();
    }
    public void show()
    {
	show( new LinkedList() );
    }
    public I_Timeslice getSelectedTimeSlice()
    {
	return selectedSlice;
    }
    
    public int getButtonPressed()
    {
        return buttonPressed;
    }

    
    private void buildUI()
    {
        final GridLayout listLayout = new GridLayout(1, 1);
        String timeFormat = "hh:mm a";
        String dateFormat = "yyyy.MM.dd";

        final SimpleDateFormat timeF = new SimpleDateFormat(timeFormat);
        final SimpleDateFormat dateF = new SimpleDateFormat(dateFormat);
        dataModel = new AbstractTableModel()
        {
            public int getColumnCount()
            {
                return 2;
            }

            public int getRowCount()
            {
                return list.size();
            }

            public String getColumnName(int col)
            {
                switch (col)
                {
                case 0:
                    return "Task";
                case 1:
                    return "Note";
                }
                return "";
            }
            public Object getValueAt(int row, int col)
            {
                I_Timeslice ts = (I_Timeslice) list.get(row);

                if (col == 0)
                {
                    if (ts.getTaskId() != I_Task.NO_TASK_ID)
                    {
                        ServiceLocator locator = ServiceLocator.getInstance();
                        I_TraxDao dao = locator.getDAO();
                        String taskName = dao.getTaskName(ts.getTaskId());

                        if (taskName != null)
                            return "T: " + taskName;
                        return "T_ID: " + ts.getTaskId();
                    }
                    return (ts.getNote() != null ? "N: " + ts.getNote() : "");
                }
                return (ts.getNote() != null ? ts.getNote() : "");
            }

        };
	final JTable table = new JTable( dataModel );
	final JScrollPane scrollPane = new JScrollPane( table );

	// outer panel
	GridBagLayout gbl = new GridBagLayout();
	this.getContentPane().setLayout( gbl );
	GridBagConstraints cons = null;

	// scroll pane housing list
	cons = new GridBagConstraints( 0, 0, // gridx, y
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
	buttons.setLayout( new GridLayout( 1, 2 ) );
	cons = new GridBagConstraints( 0, 1, // gridx, y
				       1, 1, // gridwidth, height
				       1.0, 0.0, //weightx, y
				       GridBagConstraints.CENTER, // anchor
				       GridBagConstraints.HORIZONTAL, // fill
				       new Insets(0,0,0,0), // insets
				       0,0 ); // ipadx, y
	gbl.setConstraints(buttons, cons);
	this.getContentPane().add(buttons);

	// OK button
	JButton btn = new JButton( "OK" );
	buttons.add( btn );
	btn.setMargin( new Insets( 5,5,5,5 ) );
	btn.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent ae )
		{
		    int idx = table.getSelectedRow();
		    if ( idx == -1 )
			return;

		    selectedSlice = (I_Timeslice) list.get( idx );
                    buttonPressed = OK_PRESSED;
                    hide();
		}		    
	    });

	// launch button
	btn = new JButton( "Cancel" );
	buttons.add( btn );
	btn.setMargin( new Insets( 5,5,5,5 ) );
	btn.addActionListener( new ActionListener()
	    {
                public void actionPerformed( ActionEvent e )
                {
                    hide();
                }
	    });
   }
}
