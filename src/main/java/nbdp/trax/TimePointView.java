package nbdp.trax;
import java.awt.*;
import javax.swing.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class TimePointView extends JPanel {
  private JLabel jDate = new JLabel();
  public JComboBox jMonth = new JComboBox();
  public JComboBox jDay = new JComboBox();
  public JComboBox jYear = new JComboBox();
  public JComboBox jhours = new JComboBox();
  private JLabel jTimeLabel = new JLabel();
  public JComboBox jminutes = new JComboBox();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();

  public TimePointView() {
    try {
      jbInit();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  void jbInit() throws Exception {
    jDate.setHorizontalAlignment(SwingConstants.RIGHT);
    jDate.setText("Date:  ");
    this.setLayout(gridBagLayout1);
    jTimeLabel.setText("Time:  ");
    jTimeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    this.add(jMonth,    new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(7, 0, 0, 0), -54, 0));
    this.add(jDay,    new GridBagConstraints(2, 0, 1, 2, 1.0, 0.0
            ,GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(7, 0, 0, 0), -60, 0));
    this.add(jYear,    new GridBagConstraints(3, 0, 1, 4, 1.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(6, 1, 36, 30), -59, 0));
    this.add(jDate,    new GridBagConstraints(0, 0, 1, 3, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(8, 12, 0, 1), 33, 0));
    this.add(jhours,   new GridBagConstraints(1, 1, 1, 3, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 7, 0), -54, 0));
    this.add(jminutes,  new GridBagConstraints(2, 2, 1, 2, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 7, 0), -60, 0));
    this.add(jTimeLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 13, 7, 0), 31, 0));
  }

  public void setMonthModel( ComboBoxModel m )
  {
    jMonth.setModel(m);
  }

    public static void main(String[] s)
    {
	JFrame f = new JFrame();
	f.setSize( 300, 200 );
        TimePointView tpv = new TimePointView();
	
	tpv.jMonth.addItem( "Jan" );
	tpv.jMonth.addItem( "Feb" );
	f.getContentPane().add( tpv );
	f.show();
    }
}
