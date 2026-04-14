package nbdp.trax.calendar;
import java.util.Date;

/**
   Represents a listener of CalendarView Events.
 */
public interface I_CalendarListener
{
    public static final class Item
    {
	private Item()
	{
	}
    }
    public static final Item DAY = new Item();
    public static final Item MONTH = new Item();
    public static final Item YEAR = new Item();
    
    /**
       Called when the user selects an item in the CalendarView. The event
       indicates which type of object was selected. The
       dayOfMonth indicates the day of the month. The date object represents
       some point in time on that particular dayOfMonth.
     */
    public void itemSelected( Item item,
			      int dayOfMonth,
			      Date date );
}
