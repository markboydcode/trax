package nbdp.trax.data;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds and adds Jira ticket keys (e.g. "home-5156") in timeslice notes.
 * Only the keys that lead a note tag the slice, and its time is split across
 * them; a key later in the note is a reference, as in "filed HOME-5160".
 * Keys are compared case-insensitively and written in lower case.
 *
 * @author Mark Boyd
 */
public final class TicketKeys
{
    public static final int MAX_NOTE_LENGTH = 2048;

    private static final String KEY = "[A-Za-z][A-Za-z0-9]*-\\d+";
    private static final Pattern KEY_PATTERN = Pattern.compile("\\b" + KEY + "\\b");
    private static final Pattern WHOLE_KEY = Pattern.compile(KEY);
    private static final Pattern LEADING_KEYS =
        Pattern.compile("(?:\\s*" + KEY + "\\b[,;]?)*");

    private TicketKeys()
    {
    }

    /**
     * Validates a ticket key and returns it in lower case.
     *
     * @throws IllegalArgumentException if the value is not a ticket key
     */
    public static String normalize(String ticket)
    {
        String t = ticket == null ? "" : ticket.trim();
        if (!WHOLE_KEY.matcher(t).matches())
        {
            throw new IllegalArgumentException("'" + ticket
                + "' is not a ticket key; expected a form like home-5156.");
        }
        return t.toLowerCase(Locale.ROOT);
    }

    /** Returns the lower-cased ticket keys that lead a note, in order. */
    public static Set<String> find(String note)
    {
        Set<String> keys = new LinkedHashSet<>();
        if (note != null)
        {
            Matcher lead = LEADING_KEYS.matcher(note);
            lead.lookingAt();
            Matcher m = KEY_PATTERN.matcher(note.substring(0, lead.end()));
            while (m.find())
                keys.add(m.group().toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    public static boolean contains(String note, String key)
    {
        return find(note).contains(key.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the note with the key added after any keys that lead the note,
     * so "home-5149 fleet deploy" becomes "home-5149 home-5156 fleet deploy".
     * Returns the note unchanged if its leading keys already include the key.
     *
     * @throws IllegalStateException if the result would not fit the note column
     */
    public static String add(String note, String key)
    {
        String k = normalize(key);
        if (note == null || note.isBlank())
            return k;
        if (contains(note, k))
            return note;

        Matcher m = LEADING_KEYS.matcher(note);
        m.lookingAt();
        String head = note.substring(0, m.end()).trim();
        String tail = note.substring(m.end()).trim();
        String result = (head.isEmpty() ? k : head + " " + k)
            + (tail.isEmpty() ? "" : " " + tail);

        if (result.length() > MAX_NOTE_LENGTH)
        {
            throw new IllegalStateException("Adding " + k + " would exceed the "
                + MAX_NOTE_LENGTH + " character note limit.");
        }
        return result;
    }

    /**
     * Merges an edited note with keys another process added while it was being
     * edited: any key in {@code current} that was not in {@code before} is added
     * to {@code edited}. Keys the user removed during the edit stay removed.
     */
    public static String mergeExternal(String edited, String before, String current)
    {
        Set<String> added = find(current);
        added.removeAll(find(before));
        String result = edited;
        for (String key : added)
            result = add(result, key);
        return result;
    }
}
