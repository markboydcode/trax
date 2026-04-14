

package nbdp.trax.calendar;
import java.awt.Color;

public interface I_CalendarEvaluator
{
    /**
       Returns true if the day represented by the dayOfMonth is value should
       be selectable. If the passed in dayOfMonth is out of range for this
       evaluator can choose to return false.
     */
    public boolean isDayEnabled( int dayOfMonth );

    /**
       Returns the color that should be used for the text on the button
       representing the passed in dayOfMonth. If the passed in dayOfMonth is
       out of range for the evaluator the color returned is undefined and can
       be any color applicable.
     */
    public Color getDayTextColor( int dayOfMonth );
}
