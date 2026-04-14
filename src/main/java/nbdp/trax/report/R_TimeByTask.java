package nbdp.trax.report;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import nbdp.trax.Constants;

import java.util.*;
import java.util.List;
import java.text.*;

public class R_TimeByTask
    extends JDialog
{
    private ReportResult result = null;
    private JLabel periodLabel = null;
    private AbstractTableModel dataModel = null;

    public R_TimeByTask(JFrame frame)
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
        ReportEngine engine = new ReportEngine();
        result = engine.summarizeByTask(slices);

        if (!result.isEmpty())
        {
            String totalHours = Constants.DECIMAL_FORMATTER.format(result.getTotalHours());
            periodLabel.setText("" + totalHours + " hours from "
                    + Constants.TIMESTAMP_FORMATTER.format(new Date(result.getEarliest())) + " to "
                    + Constants.TIMESTAMP_FORMATTER.format(new Date(result.getLatest())));
        } else
            periodLabel.setText("No Timeline Values to Display");
        dataModel.fireTableStructureChanged();
        super.show();
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
                if (result == null || result.isEmpty())
                    return 0;
                return result.getEntries().size();
            }

            public String getColumnName(int col)
            {
                switch (col)
                {
                case 0:
                    return "Task";
                case 1:
                    return "Hours Applied";
                }
                return "";
            }

            public Object getValueAt(int row, int col)
            {
                if (result == null || result.isEmpty())
                    return "";

                ReportEntry entry = result.getEntries().get(row);
                if (col == 0)
                {
                    return entry.getLabel();
                } else if (col == 1)
                {
                    return Constants.DECIMAL_FORMATTER.format(entry.getHours());
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