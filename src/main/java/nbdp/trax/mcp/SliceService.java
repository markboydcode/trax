package nbdp.trax.mcp;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;
import nbdp.trax.data.TicketKeys;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reads and changes the user's current timeline on behalf of MCP clients.
 * Every write runs in one transaction that first locks the config row, which
 * serializes the MCP servers of concurrent sessions, and then the timeline's
 * row, which serializes them with saves from the Swing UI.
 *
 * <p>Errors the caller should report to the user are thrown as
 * {@link IllegalArgumentException} or {@link IllegalStateException}.
 *
 * @author Mark Boyd
 */
public class SliceService
{
    private static final int ATTEMPTS = 3;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a", Locale.US);
    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
        formatter("H:mm"), formatter("h:mm a"), formatter("h:mma"));
    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
        formatter("yyyy-MM-dd H:mm"), formatter("yyyy-MM-dd h:mm a"), formatter("yyyy-MM-dd h:mma"));

    private final I_TraxDao dao;
    private final TransactionTemplate tx;
    private final Clock clock;

    public SliceService(I_TraxDao dao, TransactionTemplate tx, Clock clock)
    {
        this.dao = dao;
        this.tx = tx;
        this.clock = clock;
    }

    /** Describes the running slice, or says why nothing is running. */
    public String getCurrentSlice()
    {
        I_Timeline line = dao.getLatestTimeline();
        if (line == null)
            return "No timelines exist.";
        List<I_Timeslice> slices = slicesOf(line);
        String reason = notRunningReason(line, slices);
        if (reason != null)
            return reason;

        I_Timeslice s = slices.get(slices.size() - 1);
        long minutes = ChronoUnit.MINUTES.between(s.getStart().toLocalDateTime(), now());
        return "Task:    " + taskName(s.getTaskId()) + "\n"
            + "Type:    " + dao.getTypeById(s.getTypeId()).getName() + "\n"
            + "Started: " + TIME.format(s.getStart().toLocalDateTime())
            + " (" + minutes / 60 + "h " + minutes % 60 + "m ago)\n"
            + "Note:    " + (s.getNote() == null ? "" : s.getNote());
    }

    /** Lists the activity types a slice may have. */
    public String getTypes()
    {
        return java.util.Arrays.stream(dao.getTypes())
            .map(I_Type::getName)
            .collect(Collectors.joining("\n"));
    }

    /** Adds a ticket key to the running slice's note unless it is already there. */
    public String tagCurrentSlice(String ticket)
    {
        String key = TicketKeys.normalize(ticket);
        return write(() -> {
            I_Timeline line = lockLatest();
            List<I_Timeslice> slices = slicesOf(line);
            String reason = notRunningReason(line, slices);
            if (reason != null)
                throw new IllegalStateException(reason + " Nothing was tagged.");
            return tag(slices.get(slices.size() - 1), key);
        });
    }

    /**
     * Adds a ticket key to the note of the slice that was running at a given
     * time. {@code index} picks among several slices starting in that minute.
     */
    public String tagSlice(String time, Integer index, String ticket)
    {
        String key = TicketKeys.normalize(ticket);
        LocalDateTime at = parseTime(time, null);
        return write(() -> {
            if (at.isAfter(now()))
                throw new IllegalArgumentException(describe(at) + " is in the future.");
            I_Timeline line = lockTimelineAt(at);
            List<I_Timeslice> slices = slicesOf(line);
            int i = selectSlice(slices, at, index);
            I_Timeslice s = slices.get(i);
            if (s.getTypeId() == I_Type.OFFLINE_TYPE_ID)
                throw new IllegalArgumentException("The slice at " + describe(at) + " is Off-line.");
            return tag(s, key);
        });
    }

    /** Ends the running slice now and starts a new one. */
    public String startSlice(String task, String type, String note)
    {
        I_Task t = resolveTask(task);
        I_Type ty = resolveType(type, t);
        if (ty.getId() == I_Type.OFFLINE_TYPE_ID)
            throw new IllegalArgumentException("Use stop_slice to go Off-line.");
        return write(() -> append(t, ty, note));
    }

    /**
     * Ends the running slice now and starts one with the task, type and note
     * of the slice that was running at a given time, like the UI's Continue.
     */
    public String continueSlice(String time, Integer index)
    {
        LocalDateTime at = parseTime(time, null);
        return write(() -> {
            if (at.isAfter(now()))
                throw new IllegalArgumentException(describe(at) + " is in the future.");
            I_Timeline line = lockTimelineAt(at);
            List<I_Timeslice> slices = slicesOf(line);
            int i = selectSlice(slices, at, index);
            I_Timeslice source = slices.get(i);
            if (source.getTypeId() == I_Type.OFFLINE_TYPE_ID)
                throw new IllegalArgumentException("The slice at " + describe(at) + " is Off-line; use stop_slice.");
            I_Task task = source.getTaskId() == I_Task.NO_TASK_ID ? null : dao.getTask(source.getTaskId());
            return append(task, dao.getTypeById(source.getTypeId()), source.getNote())
                + " Continued from the slice at " + describe(minute(source)) + ".";
        });
    }

    /** Ends the running slice now by starting an Off-line slice. */
    public String stopSlice()
    {
        return write(() -> {
            I_Timeline line = lockLatest();
            String reason = notRunningReason(line, line == null ? List.of() : slicesOf(line));
            if (reason != null)
                return reason + " Nothing was changed.";
            return append(null, dao.getTypeById(I_Type.OFFLINE_TYPE_ID), null);
        });
    }

    /**
     * Inserts a slice at a past time. The slice it lands in is cut short.
     * Without an end the new slice runs until the next slice; with one, the
     * interrupted slice resumes at the end, or an Off-line slice starts there
     * when nothing was interrupted (before a day's first slice, or on a day
     * with no timeline, which is then created and requires an end).
     */
    public String insertSlice(String start, String task, String type, String note, String end)
    {
        LocalDateTime from = parseTime(start, null);
        LocalDateTime until = end == null || end.isBlank() ? null : parseTime(end, from.toLocalDate());
        I_Task t = resolveTask(task);
        I_Type ty = resolveType(type, t);

        return write(() -> {
            LocalDateTime now = now();
            if (from.isAfter(now))
                throw new IllegalArgumentException(describe(from) + " is in the future; use start_slice.");
            if (until != null && !until.isAfter(from))
                throw new IllegalArgumentException("The end must be after the start.");
            if (until != null && until.isAfter(now))
                throw new IllegalArgumentException("The end " + describe(until) + " is in the future.");

            Timestamp fromTs = Timestamp.valueOf(from);
            I_Timeline line = findTimeline(from);
            List<I_Timeslice> slices;
            String result = "";
            if (line == null)
            {
                if (until == null)
                {
                    throw new IllegalArgumentException("No timeline exists for " + from.toLocalDate()
                        + "; pass an end to create one, and the day goes Off-line at the end.");
                }
                line = dao.createTimeline(fromTs);
                slices = new ArrayList<>();
                result = "Created the timeline for " + from.toLocalDate() + ". ";
            }
            else
            {
                line = lock(line);
                slices = slicesOf(line);
            }
            for (I_Timeslice s : slices)
            {
                if (minute(s).equals(from))
                {
                    throw new IllegalArgumentException("A slice already starts at "
                        + describe(from) + "; use edit_slice instead.");
                }
            }
            int hostIdx = indexAt(slices, from);
            I_Timeslice host = hostIdx >= 0 ? slices.get(hostIdx) : null;
            I_Timeslice next = hostIdx + 1 < slices.size() ? slices.get(hostIdx + 1) : null;
            boolean fill = until != null;
            if (fill && next != null)
            {
                if (until.isAfter(minute(next)))
                {
                    throw new IllegalArgumentException("The end " + describe(until) + " runs past the next slice at "
                        + describe(minute(next)) + "; delete or move that slice first.");
                }
                // ending where the next slice begins leaves a gap of nothing
                fill = until.isBefore(minute(next));
            }

            if (host != null)
            {
                host.setDuration(fromTs.getTime() - host.getStart().getTime());
                dao.updateTimeslice(host);
            }
            I_Timeslice inserted = dao.newTimeslice(line.getId(), fromTs);
            setFields(inserted, t, ty, note);
            if (fill)
                inserted.setDuration(Timestamp.valueOf(until).getTime() - fromTs.getTime());
            else if (next != null)
                inserted.setDuration(next.getStart().getTime() - fromTs.getTime());
            dao.insertTimeslice(inserted);
            slices.add(hostIdx + 1, inserted);
            result += "Inserted " + summary(inserted) + " at " + describe(from) + ".";

            if (fill)
            {
                // resume what was interrupted, or go Off-line when nothing was
                I_Timeslice after = host != null ? host.copy() : dao.newTimeslice(line.getId(), null);
                if (host == null)
                    setFields(after, null, dao.getTypeById(I_Type.OFFLINE_TYPE_ID), null);
                after.setTimelineId(line.getId());
                after.setStart(Timestamp.valueOf(until));
                after.setDuration(next == null ? 0 : next.getStart().getTime() - after.getStart().getTime());
                dao.insertTimeslice(after);
                slices.add(hostIdx + 2, after);
                result += host != null
                    ? " " + summary(host) + " resumes at " + describe(until) + "."
                    : " Off-line from " + describe(until) + ".";
            }
            updateStop(line, slices);
            return result;
        });
    }

    /**
     * Removes the slice that was running at a given time. The slice before it
     * grows to cover the gap, as in the UI; removing a day's first slice makes
     * the timeline start at the next one.
     */
    public String deleteSlice(String time, Integer index)
    {
        LocalDateTime at = parseTime(time, null);
        return write(() -> {
            if (at.isAfter(now()))
                throw new IllegalArgumentException(describe(at) + " is in the future.");
            I_Timeline line = lockTimelineAt(at);
            List<I_Timeslice> slices = slicesOf(line);
            int i = selectSlice(slices, at, index);
            I_Timeslice s = slices.get(i);
            if (slices.size() == 1)
            {
                throw new IllegalArgumentException("The " + summary(s) + " slice is the only one in its timeline; "
                    + "delete the timeline in trax instead.");
            }
            I_Timeslice prev = i > 0 ? slices.get(i - 1) : null;
            I_Timeslice next = i + 1 < slices.size() ? slices.get(i + 1) : null;
            dao.deleteTimeslice(s);
            slices.remove(i);

            String result = "Deleted the " + summary(s) + " slice at " + describe(minute(s)) + ".";
            if (prev != null)
            {
                prev.setDuration(next == null ? 0 : next.getStart().getTime() - prev.getStart().getTime());
                dao.updateTimeslice(prev);
                result += next == null
                    ? " " + summary(prev) + " is now the last slice."
                    : " " + summary(prev) + " now runs until " + describe(minute(next)) + ".";
            }
            else
            {
                result += " The timeline now starts at " + describe(minute(next)) + ".";
            }
            updateStop(line, slices);
            return result;
        });
    }

    /**
     * Changes the slice that was running at a given time. Only the arguments
     * that are not null change; an empty note clears the note. Changing the
     * task leaves the type alone unless a type is given too.
     */
    public String editSlice(String time, Integer index, String task, String type, String note, String start)
    {
        LocalDateTime at = parseTime(time, null);
        LocalDateTime newStart = start == null || start.isBlank() ? null : parseTime(start, at.toLocalDate());
        I_Task t = task == null ? null : resolveTask(task);
        I_Type ty = type == null || type.isBlank() ? null : resolveType(type, t);
        if (task == null && ty == null && note == null && newStart == null)
            throw new IllegalArgumentException("Nothing to change: pass a task, type, note or start.");

        return write(() -> {
            LocalDateTime now = now();
            if (at.isAfter(now))
                throw new IllegalArgumentException(describe(at) + " is in the future.");
            I_Timeline line = lockTimelineAt(at);
            List<I_Timeslice> slices = slicesOf(line);
            int i = selectSlice(slices, at, index);
            I_Timeslice s = slices.get(i);
            I_Timeslice prev = i > 0 ? slices.get(i - 1) : null;
            I_Timeslice next = i + 1 < slices.size() ? slices.get(i + 1) : null;
            List<String> changes = new ArrayList<>();

            if (task != null && (t == null ? I_Task.NO_TASK_ID : t.getId()) != s.getTaskId())
            {
                changes.add("task " + taskName(s.getTaskId()) + " -> " + (t == null ? I_Task.UNASSIGNED_TASK_LABEL : t.getName()));
                s.setTaskId(t == null ? I_Task.NO_TASK_ID : t.getId());
            }
            if (ty != null && ty.getId() != s.getTypeId())
            {
                changes.add("type " + dao.getTypeById(s.getTypeId()).getName() + " -> " + ty.getName());
                s.setTypeId(ty.getId());
            }
            if (note != null)
            {
                String n = note.isBlank() ? null : note.trim();
                if (n != null && n.length() > TicketKeys.MAX_NOTE_LENGTH)
                    throw new IllegalArgumentException("The note exceeds " + TicketKeys.MAX_NOTE_LENGTH + " characters.");
                if (!java.util.Objects.equals(n, s.getNote()))
                {
                    changes.add("note '" + (n == null ? "" : n) + "'");
                    s.setNote(n);
                }
            }
            if (newStart != null && !newStart.equals(minute(s)))
            {
                if (newStart.isAfter(now))
                    throw new IllegalArgumentException("The start " + describe(newStart) + " is in the future.");
                if (prev != null && !newStart.isAfter(minute(prev)))
                {
                    throw new IllegalArgumentException("The start must be after the previous slice's start at "
                        + describe(minute(prev)) + ".");
                }
                if (next != null && !newStart.isBefore(minute(next)))
                {
                    throw new IllegalArgumentException("The start must be before the next slice's start at "
                        + describe(minute(next)) + ".");
                }
                if (prev == null && !newStart.toLocalDate().equals(at.toLocalDate()))
                    throw new IllegalArgumentException("The start must stay on " + at.toLocalDate() + ".");
                Timestamp to = Timestamp.valueOf(newStart);
                changes.add("start " + describe(minute(s)) + " -> " + describe(newStart));
                dao.moveTimeslice(line.getId(), s.getStart(), to);
                s.setStart(to);
                if (prev != null)
                {
                    prev.setDuration(to.getTime() - prev.getStart().getTime());
                    dao.updateTimeslice(prev);
                }
                if (next != null)
                    s.setDuration(next.getStart().getTime() - to.getTime());
            }
            if (changes.isEmpty())
                return "Nothing changed: the slice at " + describe(minute(s)) + " already matches.";
            dao.updateTimeslice(s);
            updateStop(line, slices);
            return "Edited the " + taskName(s.getTaskId()) + " slice at " + describe(minute(s)) + ": " + String.join("; ", changes) + ".";
        });
    }

    // ---- writes ----

    /** Ends the running slice, if any, and appends a slice at the current time. */
    private String append(I_Task task, I_Type type, String note)
    {
        Timestamp now = Timestamp.valueOf(now());
        I_Timeline line = lockLatest();
        List<I_Timeslice> slices;
        if (line == null || !isToday(line))
        {
            line = dao.createTimeline(now);
            slices = new ArrayList<>();
        }
        else
        {
            slices = slicesOf(line);
        }
        String ended = "";
        if (!slices.isEmpty())
        {
            I_Timeslice last = slices.get(slices.size() - 1);
            if (!last.getStart().before(now))
                throw new IllegalStateException("The last slice starts at or after the current time.");
            last.setDuration(now.getTime() - last.getStart().getTime());
            dao.updateTimeslice(last);
            if (last.getTypeId() != I_Type.OFFLINE_TYPE_ID)
                ended = "Ended " + summary(last) + ". ";
        }
        I_Timeslice slice = dao.newTimeslice(line.getId(), now);
        setFields(slice, task, type, note);
        dao.insertTimeslice(slice);
        slices.add(slice);
        updateStop(line, slices);

        if (type.getId() == I_Type.OFFLINE_TYPE_ID)
            return ended + "Off-line since " + TIME.format(now.toLocalDateTime()) + ".";
        return ended + "Started " + summary(slice) + " at " + TIME.format(now.toLocalDateTime()) + ".";
    }

    private String tag(I_Timeslice s, String key)
    {
        if (TicketKeys.contains(s.getNote(), key))
            return "Already tagged: " + s.getNote();
        s.setNote(TicketKeys.add(s.getNote(), key));
        dao.updateTimeslice(s);
        return "Tagged " + taskName(s.getTaskId()) + " slice from "
            + TIME.format(s.getStart().toLocalDateTime()) + ": " + s.getNote();
    }

    private void updateStop(I_Timeline line, List<I_Timeslice> slices)
    {
        I_Timeslice first = slices.get(0);
        I_Timeslice last = slices.get(slices.size() - 1);
        line.setStart(first.getStart());
        line.setStop(new Timestamp(last.getStart().getTime() + last.getDuration()));
        dao.updateTimeline(line);
    }

    /** Runs a write in a transaction, retrying when another writer holds a lock too long. */
    private String write(Supplier<String> work)
    {
        for (int attempt = 1; ; attempt++)
        {
            try
            {
                return tx.execute(status -> {
                    dao.lockConfig();
                    return work.get();
                });
            }
            catch (ConcurrencyFailureException e)
            {
                if (attempt == ATTEMPTS)
                    throw new IllegalStateException("Trax's database stayed locked; try again.", e);
            }
        }
    }

    // ---- lookups ----

    private I_Timeline lockLatest()
    {
        I_Timeline latest = dao.getLatestTimeline();
        return latest == null ? null : lock(latest);
    }

    private I_Timeline lockTimelineAt(LocalDateTime at)
    {
        I_Timeline line = findTimeline(at);
        if (line == null)
            throw new IllegalArgumentException("No timeline exists for " + at.toLocalDate() + ".");
        return lock(line);
    }

    /**
     * Returns the timeline of the day a time falls on: the last one begun by
     * that minute, or else the day's first one, for a time before it began.
     */
    private I_Timeline findTimeline(LocalDateTime at)
    {
        LocalDate day = at.toLocalDate();
        // times are named to the minute, so include a timeline begun later in that minute
        I_Timeline line = dao.getTimelineAt(Timestamp.valueOf(at.plusMinutes(1).minusNanos(1)));
        if (line != null && line.getStart().toLocalDateTime().toLocalDate().equals(day))
            return line;
        I_Timeline later = dao.getTimelineAt(Timestamp.valueOf(day.plusDays(1).atStartOfDay().minusNanos(1)));
        if (later != null && later.getStart().toLocalDateTime().toLocalDate().equals(day))
            return later;
        return null;
    }

    private I_Timeline lock(I_Timeline line)
    {
        I_Timeline locked = dao.lockTimeline(line.getId());
        if (locked == null)
            throw new ConcurrencyFailureException("Timeline " + line.getId() + " vanished while locking it.");
        return locked;
    }

    private List<I_Timeslice> slicesOf(I_Timeline line)
    {
        dao.getSlices(line);
        List<I_Timeslice> slices = new ArrayList<>();
        for (Object o : line.getSlices())
            slices.add((I_Timeslice) o);
        slices.sort(Comparator.comparing(I_Timeslice::getStart));
        return slices;
    }

    /** Returns why the timeline has no running slice, or null if it has one. */
    private String notRunningReason(I_Timeline line, List<I_Timeslice> slices)
    {
        if (line == null || slices.isEmpty())
            return "No slice is running.";
        if (!isToday(line))
            return "No slice is running: the latest timeline is from "
                + line.getStart().toLocalDateTime().toLocalDate() + ".";
        I_Timeslice last = slices.get(slices.size() - 1);
        if (last.getTypeId() == I_Type.OFFLINE_TYPE_ID)
            return "No slice is running: Off-line since " + TIME.format(last.getStart().toLocalDateTime()) + ".";
        return null;
    }

    /**
     * Returns the index of the slice a user names by a minute. When several
     * slices start in that minute, {@code index} (1 for the oldest) must pick
     * one; a minute in which no slice starts names the slice running then.
     */
    private int selectSlice(List<I_Timeslice> slices, LocalDateTime at, Integer index)
    {
        List<Integer> starting = new ArrayList<>();
        for (int i = 0; i < slices.size(); i++)
        {
            if (minute(slices.get(i)).equals(at))
                starting.add(i);
        }
        if (index != null)
        {
            if (starting.isEmpty())
            {
                throw new IllegalArgumentException("No slice starts at " + describe(at)
                    + "; index only picks among slices starting in the named minute.");
            }
            if (index < 1 || index > starting.size())
            {
                throw new IllegalArgumentException("Index " + index + " is out of range. "
                    + candidates(slices, starting, at));
            }
            return starting.get(index - 1);
        }
        if (starting.size() > 1)
            throw new IllegalArgumentException(candidates(slices, starting, at) + " Pass index to pick one.");

        int i = indexAt(slices, at);
        if (i < 0)
            throw new IllegalArgumentException("No slice was running at " + describe(at) + ".");
        return i;
    }

    private String candidates(List<I_Timeslice> slices, List<Integer> starting, LocalDateTime at)
    {
        StringBuilder sb = new StringBuilder(starting.size() + " slices start at " + describe(at)
            + ", oldest first:");
        for (int n = 0; n < starting.size(); n++)
        {
            I_Timeslice s = slices.get(starting.get(n));
            sb.append("\n  ").append(n + 1).append(". ").append(summary(s));
            if (s.getNote() != null)
                sb.append(" '").append(s.getNote()).append("'");
        }
        return sb.toString();
    }

    /**
     * Returns the index of the slice running at a minute, comparing slice starts
     * to the minute because users name times without seconds.
     */
    private static int indexAt(List<I_Timeslice> slices, LocalDateTime at)
    {
        int found = -1;
        for (int i = 0; i < slices.size(); i++)
        {
            if (!minute(slices.get(i)).isAfter(at))
                found = i;
        }
        return found;
    }

    private I_Task resolveTask(String spec)
    {
        String s = spec == null ? "" : spec.trim();
        if (s.isEmpty() || s.equalsIgnoreCase(I_Task.UNASSIGNED_TASK_LABEL))
            return null;
        if (s.matches("\\d+"))
        {
            I_Task t = dao.getTask(Integer.parseInt(s));
            if (t == null)
                throw new IllegalArgumentException("No task has id " + s + ". Use get_tasks to list tasks.");
            return t;
        }
        List<I_Task> named = new ArrayList<>();
        for (Object o : dao.getAllTasks())
        {
            I_Task t = (I_Task) o;
            if (t.getName().trim().equalsIgnoreCase(s))
                named.add(t);
        }
        if (named.isEmpty())
            throw new IllegalArgumentException("No task is named '" + s + "'. Use get_tasks to list tasks.");
        List<I_Task> open = named.stream().filter(t -> !t.getIsCompleted()).toList();
        if (open.isEmpty())
            throw new IllegalArgumentException("Task '" + s + "' is marked completed.");
        if (open.size() > 1)
        {
            throw new IllegalArgumentException("Several tasks are named '" + s + "'; pass one's id: "
                + open.stream().map(this::taskPath).collect(Collectors.joining(", ")));
        }
        return open.get(0);
    }

    /** Resolves a type by name or id, defaulting to the task's type. */
    private I_Type resolveType(String spec, I_Task task)
    {
        String s = spec == null ? "" : spec.trim();
        if (s.isEmpty())
            return dao.getTypeById(task == null ? I_Type.MISC_TYPE_ID : task.getTypeId());
        for (I_Type t : dao.getTypes())
        {
            if (t.getName().equalsIgnoreCase(s) || String.valueOf(t.getId()).equals(s))
                return t;
        }
        throw new IllegalArgumentException("No type is named '" + s + "'. Valid types:\n" + getTypes());
    }

    private String taskPath(I_Task t)
    {
        StringBuilder path = new StringBuilder(t.getName());
        I_Task p = t.getParentId() == I_Task.ROOT_TASK_PARENT_ID ? null : dao.getTask(t.getParentId());
        while (p != null)
        {
            path.insert(0, p.getName() + " > ");
            p = p.getParentId() == I_Task.ROOT_TASK_PARENT_ID ? null : dao.getTask(p.getParentId());
        }
        return path + " (id:" + t.getId() + ")";
    }

    private String taskName(int taskId)
    {
        if (taskId == I_Task.NO_TASK_ID)
            return I_Task.UNASSIGNED_TASK_LABEL;
        String name = dao.getTaskName(taskId);
        return name != null ? name : "T_ID: " + taskId;
    }

    private String summary(I_Timeslice s)
    {
        return "'" + taskName(s.getTaskId()) + "' (" + dao.getTypeById(s.getTypeId()).getName() + ")";
    }

    private static void setFields(I_Timeslice s, I_Task task, I_Type type, String note)
    {
        s.setTaskId(task == null ? I_Task.NO_TASK_ID : task.getId());
        s.setTypeId(type.getId());
        s.setNote(note == null || note.isBlank() ? null : note.trim());
        if (s.getNote() != null && s.getNote().length() > TicketKeys.MAX_NOTE_LENGTH)
            throw new IllegalArgumentException("The note exceeds " + TicketKeys.MAX_NOTE_LENGTH + " characters.");
    }

    // ---- time ----

    private LocalDateTime now()
    {
        return LocalDateTime.now(clock);
    }

    private boolean isToday(I_Timeline line)
    {
        return line.getStart().toLocalDateTime().toLocalDate().equals(now().toLocalDate());
    }

    private static LocalDateTime minute(I_Timeslice s)
    {
        return s.getStart().toLocalDateTime().truncatedTo(ChronoUnit.MINUTES);
    }

    private String describe(LocalDateTime t)
    {
        return t.toLocalDate().equals(now().toLocalDate()) ? TIME.format(t) : DATE_TIME.format(t);
    }

    /**
     * Parses "13:00", "1:00 PM" or "1:00pm" on {@code day} (today if null), or
     * the same preceded by a "yyyy-MM-dd " date.
     */
    LocalDateTime parseTime(String text, LocalDate day)
    {
        String s = text == null ? "" : text.trim();
        for (DateTimeFormatter f : DATE_TIME_FORMATS)
        {
            try
            {
                return LocalDateTime.parse(s, f);
            }
            catch (DateTimeParseException e)
            {
                // try the next format
            }
        }
        for (DateTimeFormatter f : TIME_FORMATS)
        {
            try
            {
                return LocalTime.parse(s, f).atDate(day != null ? day : now().toLocalDate());
            }
            catch (DateTimeParseException e)
            {
                // try the next format
            }
        }
        throw new IllegalArgumentException("Cannot read the time '" + text
            + "'; use 13:00, 1:00 PM, or 2026-10-01 13:00.");
    }

    private static DateTimeFormatter formatter(String pattern)
    {
        return new DateTimeFormatterBuilder().parseCaseInsensitive()
            .appendPattern(pattern).toFormatter(Locale.US);
    }
}
