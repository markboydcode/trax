package nbdp.trax.calendar;

import java.util.Date;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.plaf.basic.*;
import java.util.*;
import java.text.DateFormatSymbols;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class CalendarView extends JPanel
{
    private GregorianCalendar pointInTime = null;

    private GregorianCalendar gCal = new GregorianCalendar();

    private JLabel month = null;

    private JLabel year = null;

    private DateFormatSymbols symbols = null;

    private JPanel days = null;

    private int currentDayOfMonth = 1;

    private Vector listeners = null;

    private I_CalendarEvaluatorFactory evalFac = null;

    public CalendarView()
    {
        this(new Date());
    }

    public CalendarView(Date d)
    {
        this(d, null);
    }

    public CalendarView(I_CalendarEvaluatorFactory fac)
    {
        this(new Date(), fac);
    }

    public CalendarView(Date d, I_CalendarEvaluatorFactory fac)
    {
        if (fac == null)
            fac = new DefaultEvaluatorFactory();

        evalFac = fac;
        pointInTime = new GregorianCalendar();
        pointInTime.setTime(d);
        pointInTime.getTime();
        symbols = new DateFormatSymbols();
        createView();
        updateView();
    }

    public synchronized void addListener(I_CalendarListener l)
    {

        if (listeners == null)
            listeners = new Vector();

        listeners.remove(l);
        listeners.add(l);
    }

    private void updateListeners(I_CalendarListener.Item item, int dayOfMonth,
            Date date)
    {
        if (listeners == null)
            return;
        for (Iterator i = listeners.iterator(); i.hasNext();)
        {
            ((I_CalendarListener) i.next())
                    .itemSelected(item, dayOfMonth, date);
        }
    }

    public void setDate(Date d)
    {
        pointInTime.setTime(d);
        updateView();
        updateListeners(I_CalendarListener.MONTH, currentDayOfMonth,
                pointInTime.getTime());
    }

    public Date getDate()
    {
        return pointInTime.getTime();
    }

    public int getDayOfMonth()
    {
        return currentDayOfMonth;
    }

    private void createView()
    {
        GridBagLayout gbl = new GridBagLayout();
        this.setLayout(gbl);
        GridBagConstraints cons = null;

        // month and year panel
        JPanel monthAndYear = new JPanel();
        fillMonthAndYearPanel(monthAndYear);

        cons = new GridBagConstraints(0, 0, // gridx, y
                1, 1, // gridwidth, height
                1.0, 0.0, // weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.HORIZONTAL, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        gbl.setConstraints(monthAndYear, cons);
        this.add(monthAndYear);

        days = new JPanel();
        cons = new GridBagConstraints(0, 1, // gridx, y
                1, 1, // gridwidth, height
                1.0, 1.0, // weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.BOTH, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        gbl.setConstraints(days, cons);
        this.add(days);
    }

    private void updateView()
    {
        updateMonthAndYear();
        updateDaysView();
    }

    private void updateMonthAndYear()
    {
        String[] monthNames = symbols.getMonths();
        month.setText(monthNames[pointInTime.get(Calendar.MONTH)]);
        year.setText("" + pointInTime.get(Calendar.YEAR));
    }

    private void fillMonthAndYearPanel(JPanel monthAndYear)
    {
        GridBagLayout gbl = new GridBagLayout();
        monthAndYear.setLayout(gbl);
        GridBagConstraints cons = null;

        // left month button
        cons = new GridBagConstraints(0, 0, // gridx, y
                1, 1, // gridwidth, height
                0.0, 0.0, // weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.NONE, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        JButton priorMonth = new BasicArrowButton(SwingConstants.WEST);
        priorMonth.setMargin(new Insets(0, 2, 0, 2));
        priorMonth.setBorderPainted(false);
        gbl.setConstraints(priorMonth, cons);
        monthAndYear.add(priorMonth);
        priorMonth.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                pointInTime.add(Calendar.MONTH, -1);
                updateView();
                updateListeners(I_CalendarListener.MONTH, currentDayOfMonth,
                        pointInTime.getTime());
            }
        });

        // month title button
        cons = new GridBagConstraints(1, 0, // gridx, y
                1, 1, // gridwidth, height
                0.3, 0.0, // weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.HORIZONTAL, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        month = new JLabel("----");
        month.setBorder(new EmptyBorder(new Insets(0, 0, 0, 0)));
        Font font = month.getFont();
        font = font.deriveFont(font.getSize2D() + 4);
        month.setFont(font);
        month.setHorizontalAlignment(SwingConstants.CENTER);
        gbl.setConstraints(month, cons);
        monthAndYear.add(month);

        // right month button
        cons = new GridBagConstraints(2, 0, // gridx, y
                1, 1, // gridwidth, height
                0.0, 0.0, // weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.NONE, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        JButton nextMonth = new BasicArrowButton(SwingConstants.EAST);
        // JButton nextMonth = new JButton( " > " );
        nextMonth.setMargin(new Insets(0, 2, 0, 2));
        nextMonth.setBorderPainted(false);
        gbl.setConstraints(nextMonth, cons);
        monthAndYear.add(nextMonth);
        nextMonth.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                pointInTime.add(Calendar.MONTH, 1);
                updateView();
                updateListeners(I_CalendarListener.MONTH, currentDayOfMonth,
                        pointInTime.getTime());
            }
        });

        // left year button
        cons = new GridBagConstraints(0, 1, // gridx, y
                1, 1, // gridwidth, height
                0.0, 0.0, // weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.NONE, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        // JButton priorYear = new JButton( " < " );
        JButton priorYear = new BasicArrowButton(SwingConstants.WEST);
        priorYear.setMargin(new Insets(0, 2, 0, 2));
        priorYear.setBorderPainted(false);
        gbl.setConstraints(priorYear, cons);
        monthAndYear.add(priorYear);
        priorYear.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                pointInTime.add(Calendar.YEAR, -1);
                updateView();
                updateListeners(I_CalendarListener.YEAR, currentDayOfMonth,
                        pointInTime.getTime());
            }
        });

        // year title button
        cons = new GridBagConstraints(1, 1, // gridx, y
                1, 1, // gridwidth, height
                1.0, 0.0, // weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.HORIZONTAL, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        year = new JLabel("yyyy");
        year.setBorder(new EmptyBorder(new Insets(0, 0, 0, 0)));
        font = year.getFont();
        font = font.deriveFont(font.getSize2D() + 4);
        year.setFont(font);
        year.setHorizontalAlignment(SwingConstants.CENTER);
        gbl.setConstraints(year, cons);
        monthAndYear.add(year);

        // right year button
        cons = new GridBagConstraints(2, 1, // gridx, y
                1, 1, // gridwidth, height
                0.0, 0.0, // weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.NONE, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        JButton nextYear = new BasicArrowButton(SwingConstants.EAST);
        // JButton nextYear = new JButton( " > " );
        nextYear.setMargin(new Insets(0, 2, 0, 2));
        nextYear.setBorderPainted(false);
        gbl.setConstraints(nextYear, cons);
        monthAndYear.add(nextYear);
        nextYear.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                pointInTime.add(Calendar.YEAR, 1);
                updateView();
                updateListeners(I_CalendarListener.YEAR, currentDayOfMonth,
                        pointInTime.getTime());
            }
        });
    }

    private class DayToggleButton extends JToggleButton
    {
        int dayOfMonth = 0;

        Date date = null;

        public DayToggleButton(String txt)
        {
            super(txt);
        }
    }

    private JToggleButton[] emptyButtons = createEmptyDayButtons();

    private DayToggleButton[] dayButtons = createDayButtons();

    private JButton[] weekDayTitles = createWeekDayTitles(new String[] { "S",
            "M", "T", "W", "H", "F", "S" });

    private JButton[] createWeekDayTitles(String[] titles)
    {
        JButton[] btns = new JButton[titles.length];

        for (int i = 0; i < titles.length; i++)
        {
            btns[i] = new JButton(titles[i]);
            btns[i].setMargin(new Insets(0, 0, 0, 0));
            btns[i].setEnabled(false);
            btns[i].setBorderPainted(false);
        }
        return btns;
    }

    private void updateDaysView()
    {
        days.removeAll();
        int numberOfWeeksInMonth = pointInTime
                .getActualMaximum(Calendar.WEEK_OF_MONTH);

        // build grid to house days and weekday titles
        days.setLayout(new GridLayout(numberOfWeeksInMonth + 1, 7));

        int numberOfDaysInMonth = pointInTime
                .getActualMaximum(Calendar.DAY_OF_MONTH);
        currentDayOfMonth = pointInTime.get(Calendar.DAY_OF_MONTH);

        // get day of week for first day of month
        gCal.setTime(pointInTime.getTime());
        gCal.set(Calendar.DAY_OF_MONTH, 1);
        int dayOfWeekOfFirstDayOfMonth = gCal.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeekOfFirstDayOfMonth)
        {
        case Calendar.SUNDAY:
            dayOfWeekOfFirstDayOfMonth = 0;
            break;
        case Calendar.MONDAY:
            dayOfWeekOfFirstDayOfMonth = 1;
            break;
        case Calendar.TUESDAY:
            dayOfWeekOfFirstDayOfMonth = 2;
            break;
        case Calendar.WEDNESDAY:
            dayOfWeekOfFirstDayOfMonth = 3;
            break;
        case Calendar.THURSDAY:
            dayOfWeekOfFirstDayOfMonth = 4;
            break;
        case Calendar.FRIDAY:
            dayOfWeekOfFirstDayOfMonth = 5;
            break;
        case Calendar.SATURDAY:
            dayOfWeekOfFirstDayOfMonth = 6;
            break;
        }
        boolean priorToRealDays = true;
        boolean afterRealDays = false;

        int dayOfMonth = 1;

        // add weekday titles
        for (int i = 0; i < weekDayTitles.length; i++)
            days.add(weekDayTitles[i]);

        int emptyDaysUsed = 0;
        int daysUsed = 0;
        I_CalendarEvaluator evaluator = evalFac.getEvaluator(gCal.getTime());

        for (int week = 0; week < numberOfWeeksInMonth; week++)
        {
            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++)
            {
                if (priorToRealDays && dayOfWeek >= dayOfWeekOfFirstDayOfMonth)
                    priorToRealDays = false;
                if (dayOfMonth > numberOfDaysInMonth)
                    afterRealDays = true;

                if (priorToRealDays || afterRealDays)
                    days.add(emptyButtons[emptyDaysUsed++]);
                else
                {
                    DayToggleButton b = dayButtons[daysUsed++];
                    b.setText("" + dayOfMonth);
                    b.setForeground(evaluator.getDayTextColor(dayOfMonth));
                    b.setEnabled(evaluator.isDayEnabled(dayOfMonth));

                    if (dayOfMonth == currentDayOfMonth && b.isEnabled())
                        b.setSelected(true);
                    else
                        b.setSelected(false);

                    days.add(b);
                    b.dayOfMonth = dayOfMonth;
                    b.date = gCal.getTime();
                    dayOfMonth++;
                    gCal.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        }
    }

    private DayToggleButton[] createDayButtons()
    {
        ButtonGroup bGroup = new ButtonGroup();
        ActionListener a = new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                DayToggleButton b = null;
                b = (DayToggleButton) ae.getSource();

                if (b.dayOfMonth == currentDayOfMonth)
                    return;
                pointInTime.add(Calendar.DAY_OF_MONTH, b.dayOfMonth
                        - currentDayOfMonth);
                currentDayOfMonth = b.dayOfMonth;
                updateListeners(I_CalendarListener.DAY, b.dayOfMonth, b.date);
            }
        };

        Insets buttonMargins = new Insets(0, 0, 0, 0);
        DayToggleButton[] btns = new DayToggleButton[31];

        for (int i = 0; i < btns.length; i++)
        {
            btns[i] = new DayToggleButton("");
            btns[i].setEnabled(true);
            btns[i].setMargin(buttonMargins);
            btns[i].addActionListener(a);
            bGroup.add(btns[i]);
        }
        return btns;
    }

    private JToggleButton[] createEmptyDayButtons()
    {
        JToggleButton[] btns = new JToggleButton[12];
        for (int i = 0; i < btns.length; i++)
        {
            btns[i] = new JToggleButton("");
            btns[i].setEnabled(false);
        }
        return btns;
    }

    class Day
    {
        int dayOfMonth = 0;

        Date date = null;

        Day(int dom, Date d)
        {
            dayOfMonth = dom;
            date = d;
        }
    }

    public static void main(String[] s)
    {
        final CalendarView cal = new CalendarView();
        JFrame frame = new JFrame();
        frame.setSize(190, 200);
        frame.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent we)
            {
                java.text.SimpleDateFormat formatter = null;
                String format = "EEE, MMM d, ''yy";
                formatter = new java.text.SimpleDateFormat(format);
                String day = formatter.format(cal.getDate());
                System.out.println("Date Selected: " + day);
                System.exit(0);
            }
        });
        frame.getContentPane().add(cal);
        frame.show();

    }

    private class DefaultEvaluatorFactory implements I_CalendarEvaluatorFactory
    {
        I_CalendarEvaluator eval = null;

        DefaultEvaluatorFactory()
        {
            eval = new DefaultEvaluator();
        }

        public I_CalendarEvaluator getEvaluator(Date d)
        {
            return eval;
        }
    }

    private class DefaultEvaluator implements I_CalendarEvaluator
    {
        Date start = new Date(Long.MIN_VALUE);
        Date end = new Date(Long.MAX_VALUE);

        DefaultEvaluator()
        {
        }

        public Date getStart()
        {
            return start;
        }

        public Date getEnd()
        {
            return end;
        }

        public boolean isDayEnabled(int dayOfMonth)
        {
            return true;
        }

        public Color getDayTextColor(int dayOfMonth)
        {
            return Color.black;
        }
    }
}
