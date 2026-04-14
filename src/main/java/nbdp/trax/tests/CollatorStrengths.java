/*
 * Created on May 23, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package nbdp.trax.tests;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author mboyd
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CollatorStrengths
{

    public static void main(String[] args)
    {
        Collator comp = Collator.getInstance();
        List list = getList();
        comp.setStrength(Collator.PRIMARY);
        Collections.sort(list, comp);
        System.out.println("\nPRIMARY");
        showList(list);

        list = getList();
        comp.setStrength(Collator.SECONDARY);
        Collections.sort(list, comp);
        System.out.println("\nSECONDARY");
        showList(list);

        list = getList();
        comp.setStrength(Collator.TERTIARY);
        Collections.sort(list, comp);
        System.out.println("\nTERTIARY");
        showList(list);

        list = getList();
        comp.setStrength(Collator.IDENTICAL);
        Collections.sort(list, comp);
        System.out.println("\nIDENTICAL");
        showList(list);

    }
    private static void showList(List list)
    {
        for(Iterator i=list.iterator(); i.hasNext();)
        {
            System.out.println(i.next());
        }
    }
    private static List getList()
    {
        List list = new ArrayList();
        list.add("one");
        list.add("??45");
        list.add("!kjkj");
        list.add("Two");
        list.add("two");
        list.add("TWo");
        list.add("tWo");
        list.add("345");
        list.add("25  lk");
        list.add("012 lkj");
        list.add("ccc");
        list.add("cccd");
        return list;
    }
}
