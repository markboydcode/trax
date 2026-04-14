package nbdp.trax;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;

import java.util.*;

public class UI_TaskEditorDialog
    extends JDialog
{
    private boolean result = false;
    
    private static final int SAVE = 0;
    private static final int CANCEL = 1;
    
    private int action = -1;
    
    private JTextField name = new JTextField();
    private JTextArea  description = new JTextArea();
    private JComboBox type = new JComboBox();
    private JScrollPane descriptionScrollpane = new JScrollPane( description );
    private JButton    saveBtn = new JButton( "Save" );
    private JButton    cancelBtn = new JButton( "Cancel" );
    private JCheckBox  completedCheckBox = new JCheckBox("Completed");
    private I_Type miscType = null;
    
    private static JFrame owner = new JFrame(); 
    // may have to change this Frame approach if alt-tab
    // doesn't set focus properly for modal dialog.

    public UI_TaskEditorDialog()
    {
        super( owner, "" );
        
        setModal( true );
	type.setFont( name.getFont() );
    
    I_TraxDao dao = ServiceLocator.getInstance().getDAO();
    I_Type[] types = dao.getTypes();
    
    for( int i=0; i<types.length; i++)
    {
        if (types[i].getId() == I_Type.MISC_TYPE_ID)
            miscType = types[i];
        type.addItem( types[i] );
    }
    if (miscType != null)
    type.setSelectedItem(miscType);
    
	description.setLineWrap( true );
	descriptionScrollpane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        getContentPane().add( createDialogPanel() );
	setSize( 150, 200 );
    }

    public I_Task createSubTaskIn( I_Task parentTask )
    {
        name.setText( "" );
	    type.setSelectedItem( miscType );
        description.setText( "" );
	
	    if ( parentTask == null )
	        setTitle( "Create Task" );
	    else
	        setTitle( "Create Subtask" );
        this.completedCheckBox.setSelected(false);
    
        // the following line blocks until the dialog is closed    
        this.show();
        
        if ( action == CANCEL )
            return null;

        I_Task t = null;
        I_Type selectedType = null;
        selectedType = (I_Type) type.getSelectedItem();
        int typeId = selectedType.getId();
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
	
        if (parentTask == null) // top level task
        {
            t = dao.createTask(I_Task.ROOT_TASK_PARENT_ID, typeId, name.getText(), description.getText());
        }
        else // subtask
        {
            t = dao.createTask(parentTask.getId(), typeId, name.getText(), description.getText());
        }
        return t;
    }

    public I_Task editTask( I_Task task )
    {
        final Point top = new Point( 0,0 );
        
        name.setText( task.getName() );
        int typeId = task.getTypeId();
        I_TraxDao dao = ServiceLocator.getInstance().getDAO();
        type.setSelectedItem( dao.getTypeById( typeId ) );

        description.setText( task.getDescription() );
        setTitle( "Edit Task" );
        descriptionScrollpane.getViewport().setViewPosition( top );
        completedCheckBox.setSelected(task.getIsCompleted());
							   
        this.show();
        
        if ( action == CANCEL )
            return null;

        task.setName( name.getText() );
        task.setDescription( description.getText() );
        I_Type theType = (I_Type) type.getSelectedItem();
        task.setTypeId( theType.getId() );
        task.setIsCompleted(completedCheckBox.isSelected());
        dao.updateTask(task);
        return task;
    }

    private JPanel createDialogPanel()
    {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cons = new GridBagConstraints();
        
        JPanel p = new JPanel();
        p.setLayout( layout );
        
        // add name label and name text field

        cons.gridx = 0;
        cons.gridy = 0;
        cons.gridwidth = 3;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.NONE;
        cons.weightx = 0.0;
        cons.weighty = 0.0;
        cons.anchor = GridBagConstraints.NORTHWEST;
        JLabel label = new JLabel( "Name:" );
        layout.setConstraints( label, cons );
        p.add( label );
        
        cons.gridx = 0;
        cons.gridy = 1;
        cons.gridwidth = 3;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.weightx = 1.0;
        cons.weighty = 0.0;
        cons.anchor = GridBagConstraints.NORTHWEST;
        layout.setConstraints( name, cons );
        p.add( name );
        
        // add type label and type text field

        cons.gridx = 0;
        cons.gridy = 2;
        cons.gridwidth = 3;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.NONE;
        cons.weightx = 0.0;
        cons.weighty = 0.0;
        cons.anchor = GridBagConstraints.NORTHWEST;
        label = new JLabel( "Type:" );
        layout.setConstraints( label, cons );
        p.add( label );
        
        cons.gridx = 0;
        cons.gridy = 3;
        cons.gridwidth = 3;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.weightx = 1.0;
        cons.weighty = 0.0;
        cons.anchor = GridBagConstraints.NORTHWEST;
        layout.setConstraints( type, cons );
        p.add( type );
        type.setRenderer(new TypeFieldRenderer());
        
        // now add description and its label

        cons.gridx = 0;
        cons.gridy = 4;
        cons.gridwidth = 3;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.NONE;
        cons.weightx = 0.0;
        cons.weighty = 0.0;
        cons.anchor = GridBagConstraints.NORTHWEST;
        label = new JLabel( "Description:" );
        layout.setConstraints( label, cons );
        p.add( label );
        
        cons.gridx = 0;
        cons.gridy = 5;
        cons.gridwidth = 3;
        cons.gridheight = 5;
        cons.fill = GridBagConstraints.BOTH;
        cons.weightx = 1.0;
        cons.weighty = 1.0;
        cons.anchor = GridBagConstraints.NORTHWEST;
        layout.setConstraints( descriptionScrollpane, cons );
        p.add( descriptionScrollpane );
        
        // now add completed checkbox
        
        cons.gridx = 0;
        cons.gridy = 11;
        cons.gridwidth = 3;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.NONE;
        cons.weightx = 1.0;
        cons.weighty = 1.0;
        cons.anchor = GridBagConstraints.NORTHWEST;
        layout.setConstraints( completedCheckBox, cons );
        p.add( completedCheckBox );
        
        // now add save and cancel buttons

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout( new GridLayout( 1, 2 ) );
        btnPanel.add( saveBtn );
        btnPanel.add( cancelBtn );
        
        cons.gridx = 0;
        cons.gridy = 12;
        cons.gridwidth = 3;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.NONE;
        cons.weightx = 0.0;
        cons.weighty = 0.0;
        cons.anchor = GridBagConstraints.NORTHEAST;
        layout.setConstraints( btnPanel, cons );
        p.add( btnPanel );
        
        this.setSize( new Dimension( cancelBtn.getSize().width * 3,
                                     cancelBtn.getSize().height * 10 ) );
        
        saveBtn.addActionListener( new ActionListener() 
            {
                public void actionPerformed( ActionEvent ae )
                {
                    action = SAVE;
                    UI_TaskEditorDialog.this.hide();
                }
            });
        cancelBtn.addActionListener( new ActionListener() 
            {
                public void actionPerformed( ActionEvent ae )
                {
                    action = CANCEL;
                    UI_TaskEditorDialog.this.hide();
                }
            });
        return p;
    }

}
