package nbdp.trax;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.border.*;

import nbdp.trax.calendar.I_DateListener;

import java.util.*;
import java.text.*;

public class TimeEditor
    extends JPanel
    implements ActionListener
{
    private GregorianCalendar model = null;
    private JTextField textView = null;

    private BasicArrowButton hourPrevBtn = null;
    private BasicArrowButton hourNextBtn = null;
    private BasicArrowButton minutePrevBtn = null;
    private BasicArrowButton minuteNextBtn = null;
    private BasicArrowButton minute15PrevBtn = null;
    private BasicArrowButton minute15NextBtn = null;
    
    private static final Dimension zeroSize = new Dimension(0, 0);
    private static final String timeFormat = "hh:mm a";
    SimpleDateFormat formatter = new SimpleDateFormat( timeFormat );
    private I_DateListener dateListener = null;
    
    public TimeEditor()
    {
        model = new GregorianCalendar();
        model.setTime(new Date());
        buildUI();
        updateView();
    }
    
    public void setDateListener(I_DateListener l)
    {
        dateListener = l;
    }
    public void updateView()
    {
        textView.setText( formatter.format( model.getTime() ) );
    }
    public void actionPerformed(ActionEvent ae)
    {
        int dayOfMonth = model.get(Calendar.DAY_OF_MONTH);
        
        Object source = ae.getSource();
        if (source == hourPrevBtn)
            model.add(Calendar.HOUR_OF_DAY, -1);
        else if (source == hourNextBtn)
            model.add(Calendar.HOUR_OF_DAY, 1);
        else if (source == minutePrevBtn)
            model.add(Calendar.MINUTE, -1);
        else if (source == minuteNextBtn)
            model.add(Calendar.MINUTE, 1);
        else if (source == minute15PrevBtn)
        {
            int mins = model.get(Calendar.MINUTE);
            if (mins > 0 && mins <= 15)
                model.add(Calendar.MINUTE, -mins);
            else if (mins > 15 && mins <= 30)
                model.add(Calendar.MINUTE, -(mins - 15));
            else if (mins > 30 && mins <= 45)
                model.add(Calendar.MINUTE, -(mins - 30));
            else if (mins > 45 && mins <= 59)
                model.add(Calendar.MINUTE, -(mins - 45));
            else if (mins == 0)
                model.add(Calendar.MINUTE, -15);
        } else if (source == minute15NextBtn)
        {
            int mins = model.get(Calendar.MINUTE);
            if (mins >= 0 && mins < 15)
                model.add(Calendar.MINUTE, 15 - mins);
            else if (mins >= 15 && mins < 30)
                model.add(Calendar.MINUTE, 30 - mins);
            else if (mins >= 30 && mins < 45)
                model.add(Calendar.MINUTE, 45 - mins);
            else if (mins >= 45 && mins <= 59)
                model.add(Calendar.MINUTE, 60 - mins);
            else if (mins == 0)
                model.add(Calendar.MINUTE, 15);
        }
        int newDayOfMonth = model.get(Calendar.DAY_OF_MONTH);
        if (newDayOfMonth != dayOfMonth && dateListener != null)
        {
            dateListener.dateChanged(model.getTime());
        }
        updateView();
    }
    private void buildUI()
    {
        this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        this.setLayout(new TimeEditorLayout());

        // text field
        textView = new JTextField("------");
        textView.setEditable(false);
        //	textView.setBorder( BorderFactory.createCompoundBorder(
        // BorderFactory.createLineBorder( Color.BLACK ),
        // BorderFactory.createEmptyBorder( 0,2,0,2 ) ) );
        //textView.setBorder( BorderFactory.createEmptyBorder( 0,2,0,2 ) );
        this.add("Field", textView);

        // hours prev
        hourPrevBtn = new BasicArrowButton(SwingConstants.NORTH);
        this.add("HourPrevious", hourPrevBtn);
        hourPrevBtn.addActionListener(this);

        // hours next
        hourNextBtn = new BasicArrowButton(SwingConstants.NORTH);
        this.add("HourNext", hourNextBtn);
        hourNextBtn.addActionListener(this);

        // minutes prev
        minutePrevBtn = new BasicArrowButton(SwingConstants.NORTH);
        this.add("MinutePrevious", minutePrevBtn);
        minutePrevBtn.addActionListener(this);

        // minutes next
        minuteNextBtn = new BasicArrowButton(SwingConstants.SOUTH);
        this.add("MinuteNext", minuteNextBtn);
        minuteNextBtn.addActionListener(this);

        // 15 minutes prev
        minute15PrevBtn = new BasicArrowButton(SwingConstants.NORTH);
        this.add("Minute15Previous", minute15PrevBtn);
        minute15PrevBtn.addActionListener(this);

        // 15 minutes next
        minute15NextBtn = new BasicArrowButton(SwingConstants.SOUTH);
        this.add("Minute15Next", minute15NextBtn);
        minute15NextBtn.addActionListener(this);
    }
    public void setTime(long t)
    {
        setTime(new Date(t));
    }

    public void setTime(Date d)
    {
        model.setTime(new Date(d.getTime()));
        updateView();
    }

    public int getHourOfDay()
    {
        return model.get(Calendar.HOUR_OF_DAY);
    }

    public int getMinutes()
    {
        return model.get(Calendar.MINUTE);
    }

    private static class TimeEditorLayout implements LayoutManager
    {
        private Component hourNextButton = null;
        private Component hourPrevButton = null;
        private Component minuteNextButton = null;
        private Component minutePrevButton = null;
        private Component minute15NextButton = null;
        private Component minute15PrevButton = null;
        private Component field = null;

        public void addLayoutComponent(String name, Component c)
        {
            if ("HourNext".equals(name))
                hourNextButton = c;
            else if ("HourPrevious".equals(name))
                hourPrevButton = c;
            else if ("MinuteNext".equals(name))
                minuteNextButton = c;
            else if ("MinutePrevious".equals(name))
                minutePrevButton = c;
            else if ("Minute15Next".equals(name))
                minute15NextButton = c;
            else if ("Minute15Previous".equals(name))
                minute15PrevButton = c;
            else if ("Field".equals(name))
                field = c;
        }

        public void removeLayoutComponent(Component c)
        {
            if (c == hourNextButton)
                hourNextButton = null;
            else if (c == hourPrevButton)
                hourPrevButton = null;
            else if (c == minuteNextButton)
                minuteNextButton = null;
            else if (c == minutePrevButton)
                minutePrevButton = null;
            else if (c == minute15NextButton)
                minute15NextButton = null;
            else if (c == minute15PrevButton)
                minute15PrevButton = null;
            else if (c == field)
                field = null;
        }

        private Dimension preferredSize(Component c)
        {
            return (c == null) ? zeroSize : c.getPreferredSize();
        }

        public Dimension preferredLayoutSize(Container parent)
        {
            Dimension hourNextD = preferredSize(hourNextButton);
            Dimension hourPrevD = preferredSize(hourPrevButton);
            Dimension minuteNextD = preferredSize(minuteNextButton);
            Dimension minutePrevD = preferredSize(minutePrevButton);
            Dimension minute15NextD = preferredSize(minute15NextButton);
            Dimension minute15PrevD = preferredSize(minute15PrevButton);
            Dimension fieldD = preferredSize(field);

            /*
             * Force the editors height to be a multiple of 2
             */
            fieldD.height = ((fieldD.height + 1) / 2) * 2;

            Dimension size = new Dimension(fieldD.width, fieldD.height);
            size.width += Math.max(hourNextD.width, hourPrevD.width);
            size.width += Math.max(minuteNextD.width, minutePrevD.width);
            size.width += Math.max(minute15NextD.width, minute15PrevD.width);
            Insets insets = parent.getInsets();
            size.width += insets.left + insets.right;
            size.height += insets.top + insets.bottom;
            return size;
        }

        public Dimension minimumLayoutSize(Container parent)
        {
            return preferredLayoutSize(parent);
        }

        private void setBounds(Component c, int x, int y, int width, int height)
        {
            if (c != null)
            {
                c.setBounds(x, y, width, height);
            }
        }

        public void layoutContainer(Container parent)
        {
            Insets insets = parent.getInsets();
            int availWidth = parent.getWidth() - (insets.left + insets.right);
            int availHeight = parent.getHeight() - (insets.top + insets.bottom);
            Dimension hourNextD = preferredSize(hourNextButton);
            Dimension hourPrevD = preferredSize(hourPrevButton);
            Dimension minuteNextD = preferredSize(minuteNextButton);
            Dimension minutePrevD = preferredSize(minutePrevButton);
            Dimension minute15NextD = preferredSize(minute15NextButton);
            Dimension minute15PrevD = preferredSize(minute15PrevButton);
            int nextHeight = availHeight / 2;
            int prevHeight = availHeight - nextHeight;
            int hourWidth = Math.max(hourNextD.width, hourPrevD.width);
            int minuteWidth = Math.max(minuteNextD.width, minutePrevD.width);
            int minute15Width = Math.max(minute15NextD.width,
                    minute15PrevD.width);
            int fieldWidth = availWidth - hourWidth - minuteWidth
                    - minute15Width;

            int fieldX = insets.left;
            int hourX = fieldX + fieldWidth;
            int minuteX = hourX + hourWidth;
            int minute15X = minuteX + minuteWidth;

            int nextY = insets.top + prevHeight;
            setBounds(field, fieldX, insets.top, fieldWidth, availHeight);
            setBounds(hourNextButton, hourX, insets.top, hourWidth, prevHeight);
            setBounds(hourPrevButton, hourX, nextY, hourWidth, nextHeight);
            setBounds(minuteNextButton, minuteX, insets.top, minuteWidth,
                    prevHeight);
            setBounds(minutePrevButton, minuteX, nextY, minuteWidth, nextHeight);
            setBounds(minute15NextButton, minute15X, insets.top, minute15Width,
                    prevHeight);
            setBounds(minute15PrevButton, minute15X, nextY, minute15Width,
                    nextHeight);
        }
    }

    public static void main(String[] s)
    {
        JFrame f = new JFrame();
        f.setSize(100, 100);
        TimeEditor te = new TimeEditor();
        f.getContentPane().add(te);
        f.show();
    }
}
