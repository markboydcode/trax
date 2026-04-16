package nbdp.trax;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;

public class TypeManagerDialog extends JDialog
{
    private I_Type[] types;
    private JTable table;
    private TypeTableModel tableModel;
    private JButton renameBtn;
    private JButton deleteBtn;

    public TypeManagerDialog(JFrame owner)
    {
        super(owner, "Manage Types", true);
        buildUI();
        setSize(350, 400);
        setLocationRelativeTo(owner);
    }

    private void buildUI()
    {
        setLayout(new BorderLayout(5, 5));

        tableModel = new TypeTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);

        table.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                    renameSelected();
            }
        });
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                boolean hasSelection = table.getSelectedRow() >= 0;
                renameBtn.setEnabled(hasSelection);
                deleteBtn.setEnabled(hasSelection);
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        JButton addBtn = new JButton("Add");
        renameBtn = new JButton("Rename");
        deleteBtn = new JButton("Delete");
        JButton closeBtn = new JButton("Close");

        renameBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        addBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) { addType(); }
        });
        renameBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) { renameSelected(); }
        });
        deleteBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) { deleteSelected(); }
        });
        closeBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) { setVisible(false); }
        });

        buttons.add(addBtn);
        buttons.add(renameBtn);
        buttons.add(deleteBtn);
        buttons.add(closeBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    public void showDialog()
    {
        refreshTypes();
        setVisible(true);
    }

    private void refreshTypes()
    {
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        types = dao.getTypes();
        tableModel.fireTableDataChanged();
    }

    private void addType()
    {
        String name = JOptionPane.showInputDialog(this, "Type name:", "Add Type",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty())
            return;

        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        dao.createType(name.trim());
        refreshTypes();
    }

    private void renameSelected()
    {
        int row = table.getSelectedRow();
        if (row < 0)
            return;

        I_Type type = types[row];
        if (isProtectedType(type))
        {
            JOptionPane.showMessageDialog(this,
                    "The '" + type.getName() + "' type cannot be renamed.",
                    "Protected Type", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = (String) JOptionPane.showInputDialog(this, "New name:",
                "Rename Type", JOptionPane.PLAIN_MESSAGE, null, null, type.getName());
        if (name == null || name.trim().isEmpty() || name.trim().equals(type.getName()))
            return;

        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        type.setName(name.trim());
        dao.updateType(type);
        refreshTypes();
    }

    private void deleteSelected()
    {
        int row = table.getSelectedRow();
        if (row < 0)
            return;

        I_Type type = types[row];
        if (isProtectedType(type))
        {
            JOptionPane.showMessageDialog(this,
                    "The '" + type.getName() + "' type cannot be deleted.",
                    "Protected Type", JOptionPane.WARNING_MESSAGE);
            return;
        }

        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        if (dao.isTypeReferenced(type))
        {
            JOptionPane.showMessageDialog(this,
                    "'" + type.getName() + "' is in use by timeslices or tasks and cannot be deleted.",
                    "Type In Use", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete type '" + type.getName() + "'?",
                "Delete Type", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION)
        {
            dao.deleteType(type);
            refreshTypes();
        }
    }

    private boolean isProtectedType(I_Type type)
    {
        return type.getId() == I_Type.MISC_TYPE_ID || type.getId() == I_Type.OFFLINE_TYPE_ID;
    }

    private class TypeTableModel extends AbstractTableModel
    {
        private final String[] columns = {"ID", "Name"};

        public int getRowCount()
        {
            return types != null ? types.length : 0;
        }

        public int getColumnCount()
        {
            return columns.length;
        }

        public String getColumnName(int col)
        {
            return columns[col];
        }

        public Object getValueAt(int row, int col)
        {
            I_Type type = types[row];
            return col == 0 ? type.getId() : type.getName();
        }

        public Class<?> getColumnClass(int col)
        {
            return col == 0 ? Integer.class : String.class;
        }
    }
}