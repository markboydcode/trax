package nbdp.trax.report;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.xml.datatype.Duration;

import nbdp.trax.Constants;
import nbdp.trax.ServiceLocator;
import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;

import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.text.*;

public class R_TimeByType
    extends JDialog
{
    private Cell[] summaries = null;
    private JLabel periodLabel = null;
    private AbstractTableModel dataModel = null;

    public R_TimeByType(JFrame frame)
    {
        super(frame);
        setModal(true);
        buildUI();
    }

    public void show()
    {
        throw new UnsupportedOperationException();
    }
    public void show(List slices)
    {
        summaries = null;
        long earliest = -1;
        long latest = -1;

        if (slices.size() != 0)
        {
            TreeMap table = new TreeMap();
            ListIterator iter = slices.listIterator();
            long total = 0;
            //ReportGroupings groupings = ReportGroupings.getInstance();
            I_TraxDao dao = ServiceLocator.getInstance().getDAO();

            while (iter.hasNext())
            {
                I_Timeslice slice = (I_Timeslice) iter.next();
                int typeId = slice.getTypeId();

                // skip offline time and empty time
                if (typeId == I_Type.OFFLINE_TYPE_ID)
                {
                    continue;
                }
                Integer typeIdObj = new Integer(typeId);

                if (earliest == -1)
                    earliest = slice.getStart().getTime();
                else if (slice.getStart().getTime() < earliest)
                    earliest = slice.getStart().getTime();
                if (slice.getStart().getTime() + slice.getDuration() > latest)
                    latest = slice.getStart().getTime() + slice.getDuration();

                total += slice.getDuration();

                // see if summary object already there.
                Long sum = (Long) table.get(typeIdObj);

                if (sum == null)
                {
                    sum = new Long(slice.getDuration());
                    table.put(typeIdObj, sum);
                }
                else
                {
                    sum = new Long(sum.longValue() + slice.getDuration());
                    table.put(typeIdObj, sum);
                }
            }
            // now get names of types and sort
            TreeMap sorted = new TreeMap();
            for(Iterator itr = table.entrySet().iterator(); itr.hasNext();)
            {
                Map.Entry entry = (Entry) itr.next();
                Integer typeObjId = (Integer) entry.getKey();
                Long sum = (Long) entry.getValue();
                Cell cell = new Cell();
                I_Type type = dao.getTypeById(typeObjId.intValue());
                cell.label = type.getName();
                cell.sum = sum.longValue();
                sorted.put(cell.label, cell);
            }
            summaries = (Cell[]) sorted.values().toArray(new Cell[] {});
            // set up title
            double hours = total/3600000.0;
            String totalHours = Constants.DECIMAL_FORMATTER.format(hours);

            periodLabel.setText("" + totalHours + " hours from "
                    + Constants.TIMESTAMP_FORMATTER.format(new Date(earliest)) + " to "
                    + Constants.TIMESTAMP_FORMATTER.format(new Date(latest)));
        } else
            periodLabel.setText("No Timeline Values to Display");
        dataModel.fireTableStructureChanged();
        super.show();
    }

    private class Cell
    {
        String label = null;
        long sum = 0;
    }

    private void buildUI()
    {
        dataModel = new AbstractTableModel()
        {
            public int getColumnCount()
            {
                return 2;
            }

            public int getRowCount()
            {
                if (summaries == null)
                    return 0;
                return summaries.length;
            }

            public String getColumnName(int col)
            {
                switch (col)
                {
                case 0:
                    return "Type";
                case 1:
                    return "Hours Applied";
                }
                return "";
            }

            public Object getValueAt(int row, int col)
            {
                if (summaries == null)
                    return "";

                if (col == 0)
                {
                    //if ( summaries[row].grouping != null )
                    //    return summaries[row].grouping.getName() + " +";
                    //else
                    return summaries[row].label;
                } else if (col == 1)
                {
                    double hours = summaries[row].sum/3600000.0;
                    return Constants.DECIMAL_FORMATTER.format(hours);
                }
                return "";
            }

        };
        final JTable table = new JTable(dataModel);
        final JScrollPane scrollPane = new JScrollPane(table);

        // outer panel
        GridBagLayout gbl = new GridBagLayout();
        this.getContentPane().setLayout(gbl);
        GridBagConstraints cons = null;

        // period label
        cons = new GridBagConstraints(0, 0, // gridx, y
                1, 1, // gridwidth, height
                1.0, 0.0, //weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.HORIZONTAL, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        periodLabel = new JLabel("");
        gbl.setConstraints(periodLabel, cons);
        this.getContentPane().add(periodLabel);

        // scroll pane housing list
        cons = new GridBagConstraints(0, 1, // gridx, y
                1, 1, // gridwidth, height
                1.0, 1.0, //weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.BOTH, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        gbl.setConstraints(scrollPane, cons);
        this.getContentPane().add(scrollPane);

        // toolbar panel
        JPanel buttons = new JPanel();
        cons = new GridBagConstraints(0, 2, // gridx, y
                1, 1, // gridwidth, height
                1.0, 0.0, //weightx, y
                GridBagConstraints.CENTER, // anchor
                GridBagConstraints.HORIZONTAL, // fill
                new Insets(0, 0, 0, 0), // insets
                0, 0); // ipadx, y
        gbl.setConstraints(buttons, cons);
        this.getContentPane().add(buttons);

        // ok button
        JButton okBtn = new JButton("OK");
        buttons.add(okBtn);
        okBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                hide();
            }
        });
    }
}
