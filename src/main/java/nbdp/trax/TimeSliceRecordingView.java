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

public class TimeSliceRecordingView extends JPanel {
  //private TimelineRecorder recorder = null;
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JLabel timer = new JLabel();
  private JLabel time = new JLabel();
  private JLabel description = new JLabel();
  private JLabel taskIcon = new JLabel();
  private JLabel notesIcon = new JLabel();

  public TimeSliceRecordingView() {
    try {
      jbInit();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  /*public void setRecorder( TimelineRecorder r )
  {
    recorder = r;
  }*/
  private void jbInit() throws Exception {
    this.setLayout(gridBagLayout1);
    timer.setIcon(new ImageIcon(new java.net.URL("file:///D:/mboyd/apps/Tracks/images/watchDown.JPG")));
    time.setText("HH:MM");
    description.setText("Task or Notes text");
    taskIcon.setIcon(new ImageIcon(new java.net.URL("file:///D:/mboyd/apps/Tracks/images/task.JPG")));
    notesIcon.setIcon(new ImageIcon(new java.net.URL("file:///D:/mboyd/apps/Tracks/images/annotate.JPG")));
    this.add(timer,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
    this.add(time,    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
    this.add(description,   new GridBagConstraints(2, 0, 12, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
    this.add(taskIcon,  new GridBagConstraints(14, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
    this.add(notesIcon,  new GridBagConstraints(15, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
  }

  public static void main( String[] s )
  {
    JFrame frame = new JFrame();
    frame.setSize(400,100);
    frame.getContentPane().add( new TimeSliceRecordingView() );
    frame.show();
  }
}
