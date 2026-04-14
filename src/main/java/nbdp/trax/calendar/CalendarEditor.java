package nbdp.trax.calendar;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class CalendarEditor
    extends JDialog
{
    private CalendarView calendar = null;
    public static final int CANCEL_PRESSED = 0;
    public static final int OK_PRESSED = 1;
    
    private int buttonPressed = CANCEL_PRESSED;

    public CalendarEditor( JFrame owner )
    {
	super( owner );
	setModal( true );
	buildUI();
	setSize( 175, 225 );
    }
    public void show( Date date )
    {
	calendar.setDate( date );
        buttonPressed = CANCEL_PRESSED; // by default so closing window counts
        super.show();
    }
    public void show()
    {
	show( new Date() );
    }
    public int getButtonPressed()
    {
        return buttonPressed;
    }
    public Date getDate()
    {
	return calendar.getDate();
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
	calendar = new CalendarView();
	gbl.setConstraints(calendar, cons);
	main.add(calendar);
	
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

}
