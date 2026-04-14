package nbdp.trax;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.event.ListDataListener;

import nbdp.trax.data.I_Type;

/**
 * TODO add this in as the model for JComboBox used in TimesliceView and
 * UI_TaskEditorDialog
 * TODO 1) set the JComboBox's renderer to to RecentTypeFieldRenderer
 * TODO 2) add in DAO support for setting the recently used value for types within
 * task and for the null task.
 * TODO 3) set recently used types in locations in item 1.
 *
 * @author Mark Boyd
 * @copyright: Copyright, 2008, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class TypeComboboxModel implements ComboBoxModel
{
    private static class TypeImpl implements I_Type, Comparable<TypeImpl> {
        private String name = null;

        TypeImpl(String name) {
            this.name = name;
        }

        public int getId()
        {
            return 0;
        }

        public String getName()
        {
            return name;
        }

        public void setId(int id)
        {
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public boolean equals(Object o) {
            if (o instanceof TypeImpl) {
                TypeImpl t = (TypeImpl) o;
                if (t.getName().equals(this.name)) {
                    return true;
                }
            }
            return false;
        }

        public int compareTo(TypeImpl o)
        {
            return this.name.compareTo(o.getName());
        }
    }
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(200, 200);
        TypeComboboxModel model = new TypeComboboxModel();

        List allowed = new ArrayList();
        allowed.add(new TypeImpl("epsilon"));
        allowed.add(new TypeImpl("alpha"));
        allowed.add(new TypeImpl("zeta"));
        allowed.add(new TypeImpl("beta"));
        allowed.add(new TypeImpl("meta"));
        allowed.add(new TypeImpl("nota"));
        allowed.add(new TypeImpl("hetta"));
        allowed.add(new TypeImpl("filo"));
        allowed.add(new TypeImpl("jella"));
        allowed.add(new TypeImpl("terra"));
        allowed.add(new TypeImpl("serta"));
        allowed.add(new TypeImpl("gamma"));
        allowed.add(new TypeImpl("delta"));

        model.setContents(allowed);

        List used = new ArrayList();
        used.add(new TypeImpl("meta"));
        used.add(new TypeImpl("ZZZ"));
        used.add(new TypeImpl("epsilon"));
        used.add(new TypeImpl("nota"));
        used.add(new TypeImpl("terra"));

        model.setRecentlyUsedLimit(3);
        model.setRecentlyUsed(used);

        JComboBox box = new JComboBox(model);
        box.setRenderer(new RecentTypeFieldRenderer(model));
        frame.getRootPane().getContentPane().add(box);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private Object selectedItem = null;
    private List<ListDataListener> listeners = new ArrayList<ListDataListener>();
    private List allowedContents = new ArrayList();
    private List recentlyUsed = new ArrayList();
    private int maxRecentlyUsedItems = 0;

    public void setRecentlyUsedLimit(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("Must be greater than zero.");
        }
        if (max < recentlyUsed.size()) {
            for(int i=recentlyUsed.size()-1; i>=max; i--) {
                recentlyUsed.remove(i);
            }
        }
        maxRecentlyUsedItems = max;
    }

    public boolean isLastInRecentlyUsedList(Object o) {
        return recentlyUsed.size() > 0 && recentlyUsed.indexOf(o) == recentlyUsed.size()-1;
    }
    public void setContents(List contents) {
        this.allowedContents.clear();
        Collections.sort(contents);
        this.allowedContents.addAll(contents);
        setRecentlyUsed(null);
    }

    /**
     * Clears the list of recently used objects from the contents list and
     * accepts a new list of such objects. Null is accepted and is interpreted
     * as clearing the set of recently used items. If passed-in items
     * are not found in the contents list they are discarded. If there are more
     * items than the recentlyUsedLimit those items at or above that limit
     * are discarded.
     *
     * @param objects
     */
    public void setRecentlyUsed(List objects)
    {
        recentlyUsed.clear();

        if (objects != null && maxRecentlyUsedItems > 0)
        {
            int count = 0;
            for (Object obj : objects)
            {
                if (allowedContents.contains(obj))
                {
                    if(! recentlyUsed.contains(obj)) {
                        recentlyUsed.add(obj);
                        if (++count == maxRecentlyUsedItems) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public Object getSelectedItem()
    {
        return selectedItem;
    }

    public void setSelectedItem(Object anItem)
    {
        this.selectedItem = anItem;
    }

    public void addListDataListener(ListDataListener l)
    {
        listeners.add(l);
    }

    /**
     * Returns any elements in the recently used list first (if recently used
     * is supported as determined by a value of recentlyUsedLimit greater than
     * zero) followed by the remaining items in the content list. The recently
     * used items are ordered from most recent at the top to lease recently used
     * at the bottom. The remaining content items are sorted alphabetically.
     */
    public Object getElementAt(int index)
    {
        if (index < recentlyUsed.size()) {
            return recentlyUsed.get(index);
        }
        else if (index < allowedContents.size()){
            int perceivedIdx = recentlyUsed.size();

            for(Object obj : allowedContents) {
                if(! recentlyUsed.contains(obj)) {
                    if(perceivedIdx == index) {
                        return obj;
                    }
                    perceivedIdx++;
                }
            }
            return null;
        }
        throw new ArrayIndexOutOfBoundsException("Index " + index
                + " must be less that size of contents list "
                + allowedContents.size());
    }

    public int getSize()
    {
        int size = allowedContents.size();
        return size;
    }

    public void removeListDataListener(ListDataListener l)
    {
        listeners.remove(l);
    }
}
