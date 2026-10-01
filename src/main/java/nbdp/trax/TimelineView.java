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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import nbdp.trax.cfg.Holder;
import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.Period;
import nbdp.trax.data.TicketKeys;
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
    private static final int REFRESH_MILLIS = 10_000;
    private static final Comparator<Object> BY_START =
            Comparator.comparing(o -> ((I_Timeslice) o).getStart());
    private int editsInProgress = 0;
    private int knownLatestLineId = I_Timeline.NO_LINE_ID;

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

        I_Timeline latest = ServiceLocator.getInstance().getDAO().getLatestTimeline();
        if (latest != null)
            knownLatestLineId = latest.getId();
        new Timer(REFRESH_MILLIS, e -> refresh()).start();
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

                    openTimeline(tlFileOpener.getSelectedTimeline());
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
        actions.put("Start", editingAction("Start Rec.", this::startRecording));
        actions.put("Launch", editingAction("Launch", this::launch));
        actions.put("Delete", editingAction("Delete", this::deleteSelected));
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

        actions.put("Add", editingAction("Add", this::add));
        actions.put("Edit", editingAction("Edit", this::editSelected));
        actions.put("Insert", editingAction("Insert", this::insertBeforeSelected));
        actions.put("Continue", editingAction("Continue", this::continueSelected));
    }

    /**
     * Wraps a timeline edit so that the periodic refresh leaves the timeline
     * alone while the edit's dialogs are open, and so that a rejected edit is
     * reported and discarded.
     */
    private Action editingAction(String name, Runnable edit)
    {
        return new AbstractAction(name)
        {
            public void actionPerformed(ActionEvent ae)
            {
                editsInProgress++;
                try
                {
                    edit.run();
                } catch (IllegalArgumentException e)
                {
                    LOG.error(e.getMessage(), e);
                    JOptionPane.showMessageDialog(frame, e.getMessage(), name,
                            JOptionPane.ERROR_MESSAGE);
                    reload();
                } catch (Exception e)
                {
                    LOG.error("Exception occurred.", e);
                } finally
                {
                    editsInProgress--;
                }
            }
        };
    }

    private void startRecording()
    {
        // see if we need a new timeline
        if (currentLine != null)
            return;
        list.clear(); // should already be clear
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        long time = System.currentTimeMillis();
        setCurrentTimeline(dao.createTimeline(new Timestamp(time)));
        knownLatestLineId = currentLine.getId();
        I_Timeslice newSlice = dao.newTimeslice(currentLine.getId(), new Timestamp(time));
        list.add(newSlice);
        save();
        dataModel.fireTableRowsInserted(list.size() - 1, list.size() - 1);

        // now open editor for specifics now that time is recording
        Timestamp recorded = newSlice.getStart();
        TimesliceView tv = getTimesliceEditor();
        tv.show(newSlice);

        if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
        {
            dao.deleteTimeline(currentLine);
            setCurrentTimeline(null);
            list.clear();
            dataModel.fireTableDataChanged();
            return;
        }
        applyEdit(recorded, null, newSlice);
    }

    private void launch()
    {
        if (currentLine == null)
            return;
        reload();
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        I_Timeslice newSlice = dao.newTimeslice(currentLine.getId(),
                new Timestamp(System.currentTimeMillis()));
        list.add(newSlice);
        sortSlices();
        updateDurations();
        save();

        // now open editor for specifics now that time is recording
        Timestamp recorded = newSlice.getStart();
        TimesliceView tv = getTimesliceEditor();
        tv.show(newSlice);

        if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
            return;
        applyEdit(recorded, null, newSlice);
    }

    private void deleteSelected()
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
        if (resp != JOptionPane.YES_OPTION)
            return;

        // reloading keeps the selection on the same slices
        reload();
        indices = table.getSelectedRows();
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

    private void add()
    {
        if (currentLine == null)
            return;
        reload();
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        // take last slice, or else the line, as proto for new one
        Timestamp start = list.isEmpty() ? currentLine.getStart()
                : ((I_Timeslice) list.getLast()).getStart();
        I_Timeslice newSlice = dao.newTimeslice(currentLine.getId(), start);
        TimesliceView tv = getTimesliceEditor();
        tv.show(newSlice);

        if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
            return;
        insertSlice(newSlice);
    }

    private void editSelected()
    {
        if (currentLine == null)
            return;
        reload();
        int idx = table.getSelectedRow();
        if (idx == -1)
            return;
        I_Timeslice real = (I_Timeslice) list.get(idx);
        I_Timeslice copy = real.copy();
        TimesliceView tv = getTimesliceEditor();
        tv.show(copy);

        if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
            return;
        applyEdit(real.getStart(), real.getNote(), copy);
    }

    /**
     * Opens the editor on a new slice and adds it to the timeline only when
     * OK is pressed, so the timeline is never saved with the slice out of order.
     */
    private void insertBeforeSelected()
    {
        if (currentLine == null)
            return;
        reload();
        int idx = table.getSelectedRow();
        if (idx == -1)
            return;

        // take slice under mouse as proto for new one
        I_Timeslice selected = (I_Timeslice) list.get(idx);
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        I_Timeslice newSlice = dao.newTimeslice(currentLine.getId(), selected.getStart());
        TimesliceView tv = getTimesliceEditor();
        tv.show(newSlice);

        if (tv.getButtonPressed() == TimesliceView.CANCEL_PRESSED)
            return;
        insertSlice(newSlice);
    }

    private void continueSelected()
    {
        if (currentLine == null)
            return;
        reload();
        if (table.getSelectedRow() < 0)
            return;

        // take selected slice as proto for new one
        I_Timeslice selected = (I_Timeslice) list.get(table.getSelectedRow());
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        I_Timeslice newSlice = dao.newTimeslice(currentLine.getId(),
                new Timestamp(System.currentTimeMillis()));

        newSlice.setNote(selected.getNote());
        newSlice.setTaskId(selected.getTaskId());
        newSlice.setTypeId(selected.getTypeId());
        list.add(newSlice);
        sortSlices();
        updateDurations();
        save();
    }

    /**
     * Applies a slice edited in a dialog to the timeline as it now is in the
     * database. Ticket keys that another process, such as a Claude Code session,
     * added to the slice's note while the dialog was open are kept.
     */
    private void applyEdit(Timestamp originalStart, String noteBefore, I_Timeslice edited)
    {
        reload();
        int idx = indexOfStart(originalStart);
        if (idx < 0)
        {
            // removed elsewhere while being edited; keep the user's version
            insertSlice(edited);
            return;
        }
        I_Timeslice target = (I_Timeslice) list.get(idx);
        I_Timeslice prev = idx > 0 ? (I_Timeslice) list.get(idx - 1) : null;
        I_Timeslice next = idx + 1 < list.size() ? (I_Timeslice) list.get(idx + 1) : null;
        long start = edited.getStart().getTime();
        if ((prev != null && prev.getStart().getTime() >= start)
                || (next != null && next.getStart().getTime() <= start))
        {
            throw new IllegalArgumentException(
                    "Start time of a time slice must be after " +
                    "the immediately preceding timeslice and " +
                    "before immediately following timeslice.");
        }
        target.setStart(edited.getStart());
        target.setTaskId(edited.getTaskId());
        target.setTypeId(edited.getTypeId());
        target.setNote(TicketKeys.mergeExternal(edited.getNote(), noteBefore, target.getNote()));
        updateDurations();
        save();
    }

    /** Adds a slice at its start time, recomputing its neighbours' durations. */
    private void insertSlice(I_Timeslice slice)
    {
        reload();
        if (indexOfStart(slice.getStart()) >= 0)
        {
            throw new IllegalArgumentException("A slice already starts at "
                    + timeF.format(slice.getStart()) + ".");
        }
        list.add(slice);
        list.sort(BY_START);
        updateDurations();
        save();
        int idx = list.indexOf(slice);
        table.getSelectionModel().setSelectionInterval(idx, idx);
    }

    private void sortSlices()
    {
        list.sort(BY_START);
        for (int i = 1; i < list.size(); i++)
        {
            Timestamp start = ((I_Timeslice) list.get(i)).getStart();
            if (start.equals(((I_Timeslice) list.get(i - 1)).getStart()))
            {
                throw new IllegalArgumentException("Two slices start at "
                        + timeF.format(start) + ".");
            }
        }
    }

    private int indexOfStart(Timestamp start)
    {
        for (int i = 0; i < list.size(); i++)
        {
            if (((I_Timeslice) list.get(i)).getStart().equals(start))
                return i;
        }
        return -1;
    }

    private void openTimeline(I_Timeline line)
    {
        setCurrentTimeline(line);
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        dao.getSlices(currentLine);
        list.clear();
        list.addAll(currentLine.getSlices());
        currentLine.setSlices(list);
        dataModel.fireTableStructureChanged();
    }

    /**
     * Re-reads the open timeline so that edits start from what other processes,
     * such as the MCP server, have written. Keeps the selection on the same
     * slices.
     */
    private void reload()
    {
        if (currentLine == null)
            return;
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        dao.getSlices(currentLine);
        List fresh = currentLine.getSlices();
        currentLine.setSlices(list);
        if (sameSlices(list, fresh))
            return;

        List<Timestamp> selected = new ArrayList<>();
        for (int row : table.getSelectedRows())
            selected.add(((I_Timeslice) list.get(row)).getStart());
        list.clear();
        list.addAll(fresh);
        dataModel.fireTableDataChanged();
        for (Timestamp start : selected)
        {
            int row = indexOfStart(start);
            if (row >= 0)
                table.getSelectionModel().addSelectionInterval(row, row);
        }
    }

    private static boolean sameSlices(List a, List b)
    {
        if (a.size() != b.size())
            return false;
        for (int i = 0; i < a.size(); i++)
        {
            I_Timeslice x = (I_Timeslice) a.get(i);
            I_Timeslice y = (I_Timeslice) b.get(i);
            if (!x.getStart().equals(y.getStart())
                    || x.getTaskId() != y.getTaskId()
                    || x.getTypeId() != y.getTypeId()
                    || x.getDuration() != y.getDuration()
                    || !Objects.equals(x.getNote(), y.getNote()))
                return false;
        }
        return true;
    }

    /**
     * Picks up changes made outside the UI. Opens a timeline another process
     * started, unless an older timeline is being viewed.
     */
    private void refresh()
    {
        if (editsInProgress > 0)
            return;
        try
        {
            I_TraxDao dao = ServiceLocator.getInstance().getDAO();
            I_Timeline latest = dao.getLatestTimeline();
            if (latest != null && latest.getId() != knownLatestLineId)
            {
                boolean viewingLatest = currentLine == null
                        || currentLine.getId() == knownLatestLineId;
                knownLatestLineId = latest.getId();
                if (viewingLatest && latest.getStart().toLocalDateTime()
                        .toLocalDate().equals(LocalDate.now()))
                {
                    openTimeline(latest);
                    return;
                }
            }
            reload();
        } catch (Exception e)
        {
            LOG.error("Unable to refresh the timeline.", e);
        }
    }

    private void updateDurations()
    {
        for (int i = 0; i < list.size() - 1; i++)
        {
            I_Timeslice current = (I_Timeslice) list.get(i);
            I_Timeslice next = (I_Timeslice) list.get(i + 1);
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
                if (java.awt.Taskbar.isTaskbarSupported()) {
                    java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                    try {
                        taskbar.setIconImage(img);
                    } catch (UnsupportedOperationException e) {
                        LOG.debug("Taskbar icon not supported on this platform.");
                    }
                }
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
            timesliceEditor.setSize(220, 260);
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
