package nbdp.trax;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import nbdp.trax.cfg.Holder;
import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.Period;
import nbdp.trax.report.PeriodSelectionView;
import nbdp.trax.report.R_TimeByTask;
import nbdp.trax.report.R_TimeByType;
import nbdp.trax.report.R_TimeCompositeByTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class TimelineView extends JPanel implements TableModelListener
{
    private static final Logger LOG = LoggerFactory.getLogger(TimelineView.class);
    private UI_TaskManager taskManager = null;
    private TypeManagerDialog typeManager = null;
    private static JFrame frame = null;
    private TimesliceView timesliceEditor = null;
    private R_TimeByType typeSummaryReporter = null;
    private PeriodSelectionView periodSelector = null;
    private TLFileOpenDialog tlFileOpener = null;
    private LinkedList list = new LinkedList();
    private I_Timeline currentLine = null;
    private ContinueSliceDialog continuationDialog = null;
    private String timeFormat = "hh:mm a";
    private String dateFormat = "yyyy.MM.dd";
    ActionMap actions = new ActionMap();

    private SimpleDateFormat timeF = new SimpleDateFormat(timeFormat);
    private SimpleDateFormat dateF = new SimpleDateFormat(dateFormat);
    private AbstractTableModel dataModel = createTableModel();
    JTable table = null;
    private R_TimeByTask taskSummaryReporter;
    private R_TimeCompositeByTask taskCompositeSummaryReporter;

    private void setCurrentTimeline(I_Timeline line)
    {
        this.currentLine = line;
        Action tsr = actions.get("Timeline Type Summary Report");
        if (tsr != null)
        {
            tsr.setEnabled(line != null);
        }
        tsr = actions.get("Timeline Task Summary Report");
        if (tsr != null)
        {
            tsr.setEnabled(line != null);
        }
    }

    public TimelineView(JFrame frame)
    {

        TimelineView.frame = frame;
        buildActionMap();
        buildUI();
    }
    private void buildActionMap()
    {
        actions = new ActionMap();
        actions.put("Open", new AbstractAction("Open")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    if (tlFileOpener == null)
                    {
                        tlFileOpener = new TLFileOpenDialog(frame);
                    }
                    tlFileOpener.show();

                    if (tlFileOpener.getButtonPressed() == TLFileOpenDialog.CANCEL_PRESSED)
                        return;

                    setCurrentTimeline(tlFileOpener.getSelectedTimeline());
                    I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                    dao.getSlices(currentLine);
                    list.clear();

                    I_Timeslice slice = null;
                    for (Iterator i = currentLine.getSlices().iterator(); i
                            .hasNext();)
                    {
                        slice = (I_Timeslice) i.next();
                        list.add(slice);
                    }
                    dataModel.fireTableStructureChanged();
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });
        actions.put("Current", new AbstractAction("Current")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                    I_Timeline currentLine = dao.getCurrentTimeline();

                    if (currentLine == null) {
                        return;
                    }
                    setCurrentTimeline(currentLine);
                    dao.getSlices(currentLine);
                    list.clear();

                    I_Timeslice slice = null;
                    for (Iterator i = currentLine.getSlices().iterator(); i
                            .hasNext();)
                    {
                        slice = (I_Timeslice) i.next();
                        list.add(slice);
                    }
                    dataModel.fireTableStructureChanged();
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });
        actions.put("Close", new AbstractAction("Close")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    setCurrentTimeline(null);
                    list.clear();
                    dataModel.fireTableDataChanged();
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });
        actions.put("Stop", new AbstractAction("Stop Rec.")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    if (currentLine == null)
                    {
                        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                        dao.concludeCurrentTimeline();
                        setCurrentTimeline(null);
                        list.clear();
                        dataModel.fireTableDataChanged();
                    }
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });
        actions.put("Start", new AbstractAction("Start Rec.")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    // see if we need a new timeline
                    if (currentLine == null)
                    {
                        list.clear(); // should already be clear
                        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                        setCurrentTimeline(dao.createCurrentTimeline());
                        long time = System.currentTimeMillis();
                        I_Timeslice newSlice = dao.createTimeslice(currentLine
                                .getId(), new Timestamp(time));
                        list.add(newSlice);
                        save();
                        dataModel.fireTableRowsInserted(list.size() - 1, list
                                .size() - 1);

                        // now open editor for specifics now that time is recording
                        TimesliceView tv = getTimesliceEditor();
                        tv.show(newSlice);

                        if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
                        {
                            dao.deleteTimeline(currentLine);
                            dao.clearCurrentTimeline();
                            setCurrentTimeline(null);
                            list.clear();
                            dataModel.fireTableDataChanged();
                            return;
                        }
                        // update duration again incase changed
                        save();
                        dataModel.fireTableRowsUpdated(list.size() - 1,
                                list.size() - 1);
                    }

                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });

        actions.put("Launch", new AbstractAction("Launch")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    if (currentLine == null)
                    {
                        return;
                    }
                    long time = System.currentTimeMillis();
                    I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                    // see if we need a new timeline

                    I_Timeslice newSlice = dao.createTimeslice(currentLine
                            .getId(), new Timestamp(time));
                    list.add(newSlice);

                    if (list.size() > 1)
                    {
                        int idx = list.size() - 2;
                        I_Timeslice last = (I_Timeslice) list.get(idx);
                        long duration = newSlice.getStart().getTime()
                                - last.getStart().getTime();

                        // TBD: add loop here with message box indicating
                        // that the selected start is prior to last slice
                        // and force reselection until valid or cancel pressed.

                        if (duration < 0) // start selected prior to last
                        {
                            RuntimeException e = new IllegalArgumentException(
                                    "Start time of a time slice must be after "
                                            + "the immediately preceding timeslice.");
                            e.fillInStackTrace();
                            LOG.error(e.getMessage(), e);
                            throw e;
                        }
                        last.setDuration(duration);
                        dataModel.fireTableRowsUpdated(idx, idx);
                    }
                    save();
                    dataModel.fireTableRowsInserted(list.size() - 1, list
                            .size() - 1);

                    // now open editor for specifics now that time is recording
                    TimesliceView tv = getTimesliceEditor();
                    tv.show(newSlice);

                    if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
                    {
                        return;
                    }
                    // update duration again incase changed
                    if (list.size() > 1)
                    {
                        int idx = list.size() - 2;
                        I_Timeslice last = (I_Timeslice) list.get(idx);
                        long duration = newSlice.getStart().getTime()
                                - last.getStart().getTime();

                        // TBD: add loop here with message box indicating
                        // that the selected start is prior to last slice
                        // and force reselection until valid or cancel pressed.

                        if (duration < 0) // start selected prior to last
                        {
                            RuntimeException e = new IllegalArgumentException(
                                    "Start time of a time slice must be after "
                                            + "the immediately preceding timeslice.");
                            e.fillInStackTrace();
                            LOG.error(e.getMessage(), e);
                            throw e;
                        }
                        last.setDuration(duration);
                        dataModel.fireTableRowsUpdated(idx, idx);
                    }
                    save();
                    dataModel.fireTableRowsUpdated(list.size() - 1,
                            list.size() - 1);
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });

        actions.put("Delete", new AbstractAction("Delete")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    int[] indices = table.getSelectedRows();
                    if (indices.length == 0)
                        return;
                    String message = "Delete the selected Timeslice?";
                    if (indices.length > 1)
                        message = "Delete selected Slices?";

                    int resp = JOptionPane.showConfirmDialog(frame, message,
                            "Delete Timeslice(s)", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (resp == JOptionPane.YES_OPTION)
                    {
                        if (indices.length == 0)
                            return;
                        // make sure that the indices are sorted then walk from
                        // highest index to lowest so that we don't screw up
                        // following indices when removing lower items
                        Arrays.sort(indices);
                        for (int i = indices.length - 1; i >= 0; i--)
                        {
                            list.remove(indices[i]);
                        }
                        updateDurations();
                        save();
                    }
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });
        actions.put("Tasks", new AbstractAction("Tasks")
        {
            public void actionPerformed(ActionEvent a)
            {
                try
                {
                    UI_TaskManager m = getTaskManager();
                    m.setMode(UI_TaskManager.EDIT_TASKS);
                    m.show();
                } catch (Exception e)
                {
                    LOG.error("Exception occurred editing timeslice.", e);
                }
            }
        });
        actions.put("Types", new AbstractAction("Types")
        {
            public void actionPerformed(ActionEvent a)
            {
                try
                {
                    TypeManagerDialog m = getTypeManager();
                    m.showDialog();
                } catch (Exception e)
                {
                    LOG.error("Exception occurred editing types.", e);
                }
            }
        });

        /*
        actions.put("Report", new AbstractAction("Report")
        {
            public void actionPerformed(ActionEvent a)
            {
                try
                {
                    R_TimeByType reporter = getReporter();
                    reporter.show(list);
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });
        */
        Action lineTypeReport = new AbstractAction("Timeline Type Summary")
        {
            public void actionPerformed(ActionEvent a)
            {
                try
                {
                    R_TimeByType reporter = getTypeSummaryReporter();
                    reporter.show(list);
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        };
        // disable line report until we have a line loaded
        lineTypeReport.setEnabled(false);
        actions.put("Timeline Type Summary Report", lineTypeReport);

        Action lineTaskReport = new AbstractAction("Timeline Task Summary")
        {
            public void actionPerformed(ActionEvent a)
            {
                try
                {
                    R_TimeByTask reporter = getTaskSummaryReporter();
                    reporter.show(list);
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        };
        // disable line report until we have a line loaded
        lineTaskReport.setEnabled(false);
        actions.put("Timeline Task Summary Report", lineTaskReport);

        actions.put("Period Type Summary Report",
                new AbstractAction("Period Type Summary")
                {
                    public void actionPerformed(ActionEvent a)
                    {
                        try
                        {
                            PeriodSelectionView psv = getPeriodSelector();
                            psv.show();
                            if (psv.getButtonPressed() == PeriodSelectionView.OK_PRESSED)
                            {
                                I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                                Period period = psv.getPeriod();
                                List slices = dao.getSlicesInPeriod(period);
                                R_TimeByType reporter = getTypeSummaryReporter();
                                reporter.show(slices);
                            }
                        } catch (Exception e)
                        {
                            LOG.error("Exception occurred.", e);
                        }
                    }
                });

        actions.put("Period Task Summary Report",
                new AbstractAction("Period Task Summary")
                {
                    public void actionPerformed(ActionEvent a)
                    {
                        try
                        {
                            PeriodSelectionView psv = getPeriodSelector();
                            psv.show();
                            if (psv.getButtonPressed() == PeriodSelectionView.OK_PRESSED)
                            {
                                I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                                Period period = psv.getPeriod();
                                List slices = dao.getSlicesInPeriod(period);
                                R_TimeByTask reporter = getTaskSummaryReporter();
                                reporter.show(slices);
                            }
                        } catch (Exception e)
                        {
                            LOG.error("Exception occurred.", e);
                        }
                    }
                });

        actions.put("Period Task Composite Summary Report",
                new AbstractAction("Period Task Composite Summary")
                {
                    public void actionPerformed(ActionEvent a)
                    {
                        try
                        {
                            R_TimeCompositeByTask reporter = getTaskCompositeSummaryReporter();
                            PeriodSelectionView psv = getPeriodSelector();

                            if (reporter.getPeriod() != null)
                            {
                                psv.setPeriod(reporter.getPeriod());
                            }
                            psv.show();
                            if (psv.getButtonPressed() == PeriodSelectionView.OK_PRESSED)
                            {
                                Period period = psv.getPeriod();
                                reporter.show(period);
                            }
                        } catch (Exception e)
                        {
                            LOG.error("Exception occurred.", e);
                        }
                    }
                });

        actions.put("Create", new AbstractAction("Create")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                boolean timelineNewlyCreated = false;
                I_TraxDao dao = ServiceLocator.getInstance().getDAO();

                // first verify that we have a timeline
                if (currentLine == null)
                {
                    long time = System.currentTimeMillis();
                    setCurrentTimeline(dao.createTimeline(new Timestamp(time)));
                    timelineNewlyCreated = true;
                }
                I_Timeslice newSlice = null;
                if (list.size() > 0)
                {
                    // take last slice as proto for new one
                    I_Timeslice last = (I_Timeslice) list.getLast();
                    newSlice = dao.createTimeslice(currentLine.getId(), last
                            .getStart());
                }
                else
                {
                    // use line as proto for new one
                    newSlice = dao.createTimeslice(currentLine.getId(),
                            currentLine.getStart());
                }
                TimesliceView tv = getTimesliceEditor();
                tv.show(newSlice);

                if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
                {
                    if (timelineNewlyCreated)
                    {
                        dao.deleteTimeline(currentLine);
                        setCurrentTimeline(null);
                        list.clear();
                        dataModel.fireTableDataChanged();
                    }
                    return;
                }

                if (list.size() > 0)
                {
                    I_Timeslice last = (I_Timeslice) list.getLast();
                    long duration = newSlice.getStart().getTime()
                        - last.getStart().getTime();

                    // TBD: add loop here with message box indicating
                    // that the selected start is prior to last slice
                    // and force reselection until valid or cancel pressed.

                    if (duration < 0) // start selected prior to last
                    {
                        RuntimeException e = new IllegalArgumentException(
                                "Start time of a time slice must be after " +
                                "the immediately preceding timeslice.");
                        e.fillInStackTrace();
                        LOG.error( e.getMessage(),e);
                        throw e;
                    }
                    last.setDuration(duration);
                }
                list.add(newSlice);
                save();
                dataModel.fireTableStructureChanged();
            }
            catch(Exception e)
            {
                LOG.error("Exception occurred.", e);
            }
            }
        });

        actions.put("Add", new AbstractAction("Add")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    if (currentLine == null)
                    {
                        return;
                    }
                I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                I_Timeslice newSlice = null;
                if (list.size() > 0)
                {
                    // take last slice as proto for new one
                    I_Timeslice last = (I_Timeslice) list.getLast();
                    newSlice = dao.createTimeslice(currentLine.getId(), last
                            .getStart());
                }
                else
                {
                    // use line as proto for new one
                    newSlice = dao.createTimeslice(currentLine.getId(),
                            currentLine.getStart());
                }
                TimesliceView tv = getTimesliceEditor();
                tv.show(newSlice);

                if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
                {
                    /*
                     *TODO: ADD remove dao.timeSlice to remove aliased slices
                     *
                    if (timelineNewlyCreated)
                    {
                        dao.deleteTimeline(currentLine);
                        setCurrentTimeline(null);
                        list.clear();
                        dataModel.fireTableDataChanged();
                    }
                    */
                    return;
                }

                if (list.size() > 0)
                {
                    I_Timeslice last = (I_Timeslice) list.getLast();
                    long duration = newSlice.getStart().getTime()
                        - last.getStart().getTime();

                    // TBD: add loop here with message box indicating
                    // that the selected start is prior to last slice
                    // and force reselection until valid or cancel pressed.

                    if (duration < 0) // start selected prior to last
                    {
                        RuntimeException e = new IllegalArgumentException(
                                "Start time of a time slice must be after " +
                                "the immediately preceding timeslice.");
                        e.fillInStackTrace();
                        LOG.error( e.getMessage(),e);
                        throw e;
                    }
                    last.setDuration(duration);
                }
                list.add(newSlice);
                save();
                dataModel.fireTableStructureChanged();
            }
            catch(Exception e)
            {
                LOG.error("Exception occurred.", e);
            }
            }
        });

        actions.put("Edit", new AbstractAction("Edit")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                int idx = table.getSelectedRow();
                if (idx == -1)
                    return;
                I_Timeslice real = (I_Timeslice) list.get(idx);
                I_Timeslice copy = real.copy();
                TimesliceView tv = getTimesliceEditor();
                tv.show(copy);

                if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
                    return;

                if (idx > 0)
                {
                    I_Timeslice last = (I_Timeslice) list.get(idx - 1);
                    I_Timeslice next =
                        (list.size() > idx + 1
                            ? (I_Timeslice) list.get(idx + 1)
                            : null);

                    // TBD: add loop here with message box indicating
                    // that the selected start is prior to last slice
                    // and force reselection until valid or cancel pressed.
                    if (last.getStart().getTime() > copy.getStart().getTime()
                            || (next != null && next.getStart().getTime() < copy
                                    .getStart().getTime()))
                    {
                        RuntimeException e = new IllegalArgumentException(
                                "Start time of a time slice must be after " +
                                "the immediately preceding timeslice and " +
                                "before immediately following timeslice.");
                        e.fillInStackTrace();
                        LOG.error( e.getMessage(),e);
                        throw e;
                    }
                }
                real.setStart(copy.getStart());
                real.setTaskId(copy.getTaskId());
                real.setTypeId(copy.getTypeId());
                real.setNote(copy.getNote());
                updateDurations();
                save();

                /*		    if ( idx > 0 )
                dataModel.fireTableRowsUpdated( idx-1, idx );
                else
                dataModel.fireTableRowsUpdated( idx, idx );
                */
                }
                catch(Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });

        actions.put("Insert", new AbstractAction("Insert")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                    long time = System.currentTimeMillis();
                    I_TraxDao dao = ServiceLocator.getInstance().getDAO();

                    int idx = table.getSelectedRow();
                    if (idx == -1)
                        return;

                    // take slice under mouse as proto for new one
                    I_Timeslice currentSlice = (I_Timeslice) list.get(idx);
                    I_Timeslice newSlice = dao.createTimeslice(
                            currentLine.getId(), currentSlice.getStart());
                    list.add(idx, newSlice);
                    save();
                    dataModel.fireTableRowsInserted(idx, idx);
                    table.getSelectionModel().clearSelection();
                    table.getSelectionModel().setSelectionInterval(idx, idx);

                    // now open editor for specifics
                    TimesliceView tv = getTimesliceEditor();
                    tv.show(newSlice);

                    if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
                    {
                        list.remove(idx);
                        save();
                        dataModel.fireTableDataChanged();
                        return;
                    }
                    // update duration again incase changed
                    if (list.size() > 1)
                    {
                        I_Timeslice last = (I_Timeslice) list.getFirst();
                        I_Timeslice curr = null;

                        for (int i=1; i<list.size()-1; i++)
                        {
                            curr = (I_Timeslice) list.get(i);
                            long duration = curr.getStart().getTime()
                            - last.getStart().getTime();
                            if (duration < 0) // start selected prior to last
                            {
                                RuntimeException e = new IllegalArgumentException(
                                        "Start time of time slice at index "
                                        + i + " must be after that of the "
                                        + "immediately preceding timeslice.");
                                e.fillInStackTrace();
                                LOG.error(e.getMessage(), e);
                                throw e;
                            }
                            last.setDuration(duration);
                            last = curr;
                        }
                    }
                    save();
                    dataModel.fireTableRowsUpdated(0, list.size() - 1);
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });

        actions.put("Continue", new AbstractAction("Continue")
        {
            public void actionPerformed(ActionEvent ae)
            {
                try
                {
                if (table.getSelectedRow() < 0)
                    return;

                // take selected slice as proto for new one
                I_Timeslice selected =
                    (I_Timeslice) list.get(table.getSelectedRow());
                I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                I_Timeslice newSlice = dao.createTimeslice(currentLine.getId(),
                        new Timestamp(System.currentTimeMillis()));

                newSlice.setNote(selected.getNote());
                newSlice.setTaskId(selected.getTaskId());
                newSlice.setTypeId(selected.getTypeId());
                list.add(newSlice);

                if (list.size() > 1)
                {
                    int idx = list.size() - 2;
                    I_Timeslice last = (I_Timeslice) list.get(idx);
                    long duration = newSlice.getStart().getTime()
                            - last.getStart().getTime();

                    // TBD: add loop here with message box indicating
                    // that the selected start is prior to last slice
                    // and force reselection until valid or cancel pressed.

                    if (duration < 0) // start selected prior to last
                    {
                        RuntimeException e = new IllegalArgumentException(
                                "Start time of a time slice must be after " +
                                "the immediately preceding timeslice.");
                        e.fillInStackTrace();
                        LOG.error( e.getMessage(),e);
                        throw e;
                    }
                    last.setDuration(duration);
                    dataModel.fireTableRowsUpdated(idx, idx);
                }
                save();
                dataModel.fireTableRowsInserted(
                    list.size() - 1,
                    list.size() - 1);
                }
                catch(Exception e)
                {
                    LOG.error("Exception occurred.", e);
                }
            }
        });
    }
    private void updateDurations()
    {
        for (int i = 0; i < list.size() - 1; i++)
        {
            I_Timeslice current = (I_Timeslice) list.get(i);
            I_Timeslice next = (I_Timeslice) list.get(i + 1);

            System.out.println(
                "Setting "
                    + i
                    + " dur from "
                    + current.getDuration()
                    + " to "
                    + (next.getStart().getTime() - current.getStart().getTime()));
            current.setDuration(next.getStart().getTime()
                    - current.getStart().getTime());
        }
        if (list.size() > 0)
        {
            I_Timeslice last = (I_Timeslice) list.get(list.size() - 1);
            last.setDuration(0);
        }
        // didn't include the following line because of possible
        // extensions needing to be made to timeline to indicate a
        // timeline that is currently being recorded or one that
        // has been completed. That indicator may be that the last
        // slice has a duration value set.
        //last.setDuration( -1 );
        dataModel.fireTableDataChanged();
        //	dataModel.fireTableRowsUpdated( 0, list.size()-1 );
    }
    private static void usage()
    {
    	StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);
        pw.println("\nusage: java TimelineView -db DBpath");
        pw.println(
            "\nTo specify the current directory use "
                + "--> java TimelineView \"\"\n");
        pw.flush();

    	LOG.error(sw.toString());
    	System.out.println(sw.toString());
        System.exit(0);
    }

    private void save()
    {
        if (list.size() == 0)
            return;

        I_Timeslice slice = (I_Timeslice) list.getFirst();
        currentLine.setStart(slice.getStart());
        currentLine.setSlices(list);
        slice = (I_Timeslice) list.getLast();
        long stop = slice.getStart().getTime() + slice.getDuration();
        currentLine.setStop(new Timestamp(stop));
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        dao.saveTimeline(currentLine);
    }

    private AbstractTableModel createTableModel()
    {
        list = new LinkedList();
        AbstractTableModel m = new AbstractTableModel()
        {
            String[] colTitles =
                new String[] {
                    "Time",
                    "Date",
                    "Type",
                    "Task",
                    "Note",
                    "Duration" };

            public int getColumnCount()
            {
                return colTitles.length;
            }
            public int getRowCount()
            {
                return list.size();
            }
            public String getColumnName(int col)
            {
                return colTitles[col];
            }
            public Object getValueAt(int row, int col)
            {
                I_Timeslice ts = (I_Timeslice) list.get(row);
                if (col == 0) // Time
                {
                    Date start = new Date(ts.getStart().getTime());
                    return timeF.format(start);
                }
                else if (col == 1) // Date
                {
                    Date start = new Date(ts.getStart().getTime());
                    return dateF.format(start);
                }
                else if (col == 2) // Type
                {
                    I_TraxDao dao = ServiceLocator.getInstance().getDAO();
                    return dao.getTypeById(ts.getTypeId()).getName();
                }
                else if (col == 3) // Task
                {
                    if (ts.getTaskId() != I_Task.NO_TASK_ID)
                    {
                        ServiceLocator locator = ServiceLocator.getInstance();
                        I_TraxDao dao = locator.getDAO();
                        String taskName = dao.getTaskName(ts.getTaskId());

                        if ( taskName != null )
                            return taskName;
                        return "T_ID: " + ts.getTaskId();
                    }
                    return "";
                }
                else if (col == 4) // Note
                {
                    return (ts.getNote() != null ? ts.getNote() : "");
                }
                else // Duration
                {
                    if (ts.getDuration() == -1)
                        return "???";
                    long dur = ts.getDuration();
                    long hours = dur / 3600000L;
                    long minutes = (dur - (hours * 3600000L)) / 60000;
                    double decimalHours = dur/3600000.0;
                    return Constants.DECIMAL_FORMATTER.format(decimalHours) +
                    ",  " + (hours == 0 ? 0 : hours) + ":" + minutes;
                }
            }

        };
        m.addTableModelListener(this);
        return m;
    }

    /**
     * Handle table changes to update Frame's title.
     * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
     */
    public void tableChanged(TableModelEvent e)
    {
        if (list.size() > 0)
        {
            I_Timeslice slice = (I_Timeslice) list.getLast();
            int tid = slice.getTaskId();
            if (tid != I_Task.NO_TASK_ID)
            {
                ServiceLocator locator = ServiceLocator.getInstance();
                I_TraxDao dao = locator.getDAO();
                String taskName = dao.getTaskName(tid);

                if ( taskName != null )
                    frame.setTitle(taskName);
                else
                    frame.setTitle("T_ID: " + tid);
            }
            else
                frame.setTitle("");
        }
        // TODO Auto-generated method stub

    }

    private void buildUI()
    {

        final GridLayout listLayout = new GridLayout(1, 1);
        table = new JTable(dataModel);
        TimelinePopupMenuMgr popMenuMgr = new TimelinePopupMenuMgr(this);
        table.addMouseListener(popMenuMgr);

        final JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.addMouseListener(popMenuMgr);

        // outer panel
        GridBagLayout gbl = new GridBagLayout();
        this.setLayout(gbl);
        GridBagConstraints cons = null;

        // toolbar panel
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 8));
            cons = new GridBagConstraints(0, 0, // gridx, y
        1, 1, // gridwidth, height
        1.0, 0.0, //weightx, y
        GridBagConstraints.CENTER, // anchor
        GridBagConstraints.HORIZONTAL, // fill
        new Insets(0, 0, 0, 0), // insets
    0, 0); // ipadx, y
        gbl.setConstraints(buttons, cons);
        this.add(buttons);

        // open button
        JButton openBtn = new JButton(actions.get("Open"));
        //openBtn.setMargin( new Insets( 5,5,5,5 ) );
        buttons.add(openBtn);

        JButton closeBtn = new JButton(actions.get("Close"));
        buttons.add(closeBtn);

        // new button
        JButton createBtn = new JButton(actions.get("Create"));
        buttons.add(createBtn);

        // task manager button
        JButton taskMgrBtn = new JButton(actions.get("Tasks"));
        buttons.add(taskMgrBtn);

        // type manager button
        JButton typeMgrBtn = new JButton(actions.get("Types"));
        buttons.add(typeMgrBtn);

        // report button

        //JButton reportBtn = new JButton(actions.get("Reports"));
        JButton reportBtn = new JButton("Report");
        reportBtn.addMouseListener(popMenuMgr);
        buttons.add(reportBtn);

        // new button
        JButton currentBtn = new JButton(actions.get("Current"));
        buttons.add(currentBtn);

        // start button
        JButton startBtn = new JButton(actions.get("Start"));
        buttons.add(startBtn);

        // launch button
        JButton launchBtn = new JButton(actions.get("Launch"));
        //launchBtn.setMargin( new Insets( 5,5,5,5 ) );
        buttons.add(launchBtn);

        // add button
        JButton addBtn = new JButton(actions.get("Add"));
        buttons.add(addBtn);
        //	addBtn.setMargin( new Insets( 5,5,5,5 ) );

        // stop button
        JButton stopBtn = new JButton(actions.get("Stop"));
        buttons.add(stopBtn);

        // scroll pane housing list
            cons = new GridBagConstraints(0, 1, // gridx, y
        1, 1, // gridwidth, height
        1.0, 1.0, //weightx, y
        GridBagConstraints.CENTER, // anchor
        GridBagConstraints.BOTH, // fill
        new Insets(0, 0, 0, 0), // insets
        	0, 0); // ipadx, y
        JScrollPane pane = scrollPane;
        gbl.setConstraints(pane, cons);
        this.add(pane);
    }

    public static void main(String[] s)
    {
        try
        {
            LOG.debug("###### Starting TRAX ######");

            ConfigurableApplicationContext ctx = SpringApplication.run(TraxApplication.class, s);
            Holder configHolder = ctx.getBean(Holder.class);
            Map cfg = configHolder.getConfig();

            JFrame frame = new JFrame();
            frame.setDefaultLookAndFeelDecorated(true);
            String imagePath = (String) cfg.get("window.classpath.image.file");
            ClassLoader cl = TimelineView.class.getClassLoader();
            URL url = cl.getResource(imagePath);

            if (url == null) {
                LOG.debug("IMAGE {} unavailable via classloader.", imagePath);
            }
            else {
                LOG.debug("IMAGE {} found at: {}", imagePath, url.toExternalForm());
                ImageIcon ico = new ImageIcon(url);
                Image img = ico.getImage();
                frame.setIconImage(img);
            }
            TimelineView tv = new TimelineView(frame);
            frame.setSize(700, 400);
            frame.getContentPane().add(tv);
            frame.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent we)
                {
                    System.exit(0);
                }
            });
            frame.setVisible(true);
        }
        catch(Throwable e)
        {
            LOG.error("Exception occurred.", e);
            System.exit(0);
        }
    }
    private UI_TaskManager getTaskManager()
    {
        if (taskManager == null)
        {
            taskManager = new UI_TaskManager(frame);
            taskManager.setSize(400, 600);
        }

        return taskManager;
    }
    private TypeManagerDialog getTypeManager()
    {
        if (typeManager == null)
        {
            typeManager = new TypeManagerDialog(frame);
        }

        return typeManager;
    }
    private PeriodSelectionView getPeriodSelector()
    {
        if (periodSelector == null)
        {
            periodSelector = new PeriodSelectionView(frame);
            periodSelector.setSize(220, 220);
        }

        return periodSelector;
    }
    private R_TimeByType getTypeSummaryReporter()
    {
        if (typeSummaryReporter == null)
        {
            typeSummaryReporter = new R_TimeByType(frame);
            typeSummaryReporter.setSize(300, 400);
        }

        return typeSummaryReporter;
    }
    private R_TimeByTask getTaskSummaryReporter()
    {
        if (taskSummaryReporter == null)
        {
            taskSummaryReporter = new R_TimeByTask(frame);
            taskSummaryReporter.setSize(300, 400);
        }

        return taskSummaryReporter;
    }
    private R_TimeCompositeByTask getTaskCompositeSummaryReporter()
    {
        if (taskCompositeSummaryReporter == null)
        {
            taskCompositeSummaryReporter = new R_TimeCompositeByTask(frame);
            taskCompositeSummaryReporter.setSize(460, 400);
        }

        return taskCompositeSummaryReporter;
    }
    private TimesliceView getTimesliceEditor()
    {
        if (timesliceEditor == null)
        {
            timesliceEditor = new TimesliceView(getTaskManager(), frame);
            timesliceEditor.setSize(220, 250);
        }

        return timesliceEditor;
    }
    private ContinueSliceDialog getContinuationDialog()
    {
        if (continuationDialog == null)
        {
            continuationDialog =
                new ContinueSliceDialog(getTaskManager(), frame);
            continuationDialog.setSize(200, 300);
        }

        return continuationDialog;
    }

}
