package nbdp.trax.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nbdp.trax.TraxApplication;
import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = TraxApplication.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:traxtest;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class SliceServiceTest
{
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 14, 0);
    private static final int CODING = 5;
    private static final int MEETING = 2;

    @Autowired
    private I_TraxDao dao;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private JdbcTemplate jdbc;

    private SliceService service;
    private I_Task taz;
    private I_Task bs;

    @BeforeEach
    void setUp()
    {
        jdbc.update("DELETE FROM timeslice");
        jdbc.update("DELETE FROM timelines");
        jdbc.update("DELETE FROM tasks");
        service = new SliceService(dao, tx, Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
        taz = dao.createTask(I_Task.ROOT_TASK_PARENT_ID, CODING, "TAZ", null);
        bs = dao.createTask(I_Task.ROOT_TASK_PARENT_ID, CODING, "BS", null);
    }

    @Test
    void tagsCurrentSliceOnce()
    {
        timeline(NOW.minusHours(2), slice("12:00:37", taz, CODING, "home-5122 nullaway"));

        assertThat(service.tagCurrentSlice("HOME-5156")).contains("home-5122 home-5156 nullaway");
        assertThat(service.tagCurrentSlice("home-5156")).startsWith("Already tagged");
        assertThat(notes()).containsExactly("home-5122 home-5156 nullaway");
    }

    @Test
    void concurrentTagsAreAllKept() throws Exception
    {
        timeline(NOW.minusHours(2), slice("12:00", taz, CODING, "work"));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<String>> tags = new ArrayList<>();
        for (int i = 0; i < 8; i++)
        {
            String key = "home-" + (6000 + i);
            tags.add(() -> service.tagCurrentSlice(key));
        }
        for (Future<String> f : pool.invokeAll(tags))
            f.get();
        pool.shutdown();

        String note = notes().get(0);
        for (int i = 0; i < 8; i++)
            assertThat(note).contains("home-" + (6000 + i));
    }

    @Test
    void refusesToTagWhenNothingIsRunning()
    {
        timeline(NOW.minusHours(2), slice("12:00", taz, CODING, null), slice("13:00", null, I_Type.OFFLINE_TYPE_ID, null));
        assertThatThrownBy(() -> service.tagCurrentSlice("home-1")).hasMessageContaining("Off-line since 1:00 PM");

        jdbc.update("DELETE FROM timeslice");
        jdbc.update("DELETE FROM timelines");
        timeline(NOW.minusDays(1), slice(NOW.minusDays(1), taz, CODING, null));
        assertThatThrownBy(() -> service.tagCurrentSlice("home-1")).hasMessageContaining("2026-09-30");
    }

    @Test
    void startEndsCurrentSliceAndAppends()
    {
        I_Timeline line = timeline(NOW.minusHours(2), slice("12:00", taz, CODING, "home-5122"));

        assertThat(service.startSlice("bs", null, "home-5134")).contains("Ended 'TAZ'").contains("Started 'BS'");

        List<I_Timeslice> slices = slices(line);
        assertThat(slices).hasSize(2);
        assertThat(slices.get(0).getDuration()).isEqualTo(2 * 3_600_000L);
        assertThat(slices.get(1).getTaskId()).isEqualTo(bs.getId());
        assertThat(slices.get(1).getNote()).isEqualTo("home-5134");
        assertThat(dao.getLatestTimeline().getStop()).isEqualTo(Timestamp.valueOf(NOW));
    }

    @Test
    void startCreatesTodaysTimeline()
    {
        timeline(NOW.minusDays(1), slice(NOW.minusDays(1), taz, CODING, null));

        service.startSlice("TAZ", "meeting", null);

        I_Timeline today = dao.getLatestTimeline();
        assertThat(today.getStart()).isEqualTo(Timestamp.valueOf(NOW));
        assertThat(slices(today)).singleElement().extracting(I_Timeslice::getTypeId).isEqualTo(MEETING);
    }

    @Test
    void continueCopiesEarlierSlice()
    {
        I_Timeline line = timeline(NOW.minusHours(3),
            slice("11:00", taz, MEETING, "home-5122 nullaway"), slice("12:00", bs, CODING, null),
            slice("13:00", null, I_Type.OFFLINE_TYPE_ID, null));

        assertThat(service.continueSlice("11:30")).contains("Started 'TAZ' (Meeting) at 2:00 PM")
            .contains("Continued from the slice at 11:00 AM");

        I_Timeslice resumed = slices(line).get(3);
        assertThat(resumed.getTaskId()).isEqualTo(taz.getId());
        assertThat(resumed.getTypeId()).isEqualTo(MEETING);
        assertThat(resumed.getNote()).isEqualTo("home-5122 nullaway");
        assertThat(slices(line).get(2).getDuration()).isEqualTo(3_600_000L);
        assertThatThrownBy(() -> service.continueSlice("13:30")).hasMessageContaining("Off-line");
    }

    @Test
    void stopGoesOffline()
    {
        I_Timeline line = timeline(NOW.minusHours(1), slice("13:00", taz, CODING, null));

        assertThat(service.stopSlice()).contains("Off-line since 2:00 PM");
        assertThat(slices(line).get(1).getTypeId()).isEqualTo(I_Type.OFFLINE_TYPE_ID);
        assertThat(service.stopSlice()).contains("Nothing was changed");
    }

    @Test
    void validatesTasksAndTypes()
    {
        timeline(NOW.minusHours(1), slice("13:00", taz, CODING, null));
        dao.createTask(taz.getId(), CODING, "BS", null);
        I_Task done = dao.createTask(I_Task.ROOT_TASK_PARENT_ID, CODING, "Old", null);
        done.setIsCompleted(true);
        dao.updateTask(done);

        assertThatThrownBy(() -> service.startSlice("BS", null, null)).hasMessageContaining("TAZ > BS");
        assertThatThrownBy(() -> service.startSlice("Old", null, null)).hasMessageContaining("completed");
        assertThatThrownBy(() -> service.startSlice("Nope", null, null)).hasMessageContaining("get_tasks");
        assertThatThrownBy(() -> service.startSlice("TAZ", "Napping", null)).hasMessageContaining("Coding");
        assertThatThrownBy(() -> service.startSlice("TAZ", "Off-line", null)).hasMessageContaining("stop_slice");
    }

    @Test
    void insertWithoutEndRunsToNextSlice()
    {
        I_Timeline line = timeline(NOW.minusHours(5),
            slice("09:00", taz, CODING, null), slice("12:00:20", bs, CODING, null));

        service.insertSlice("10:30", "Unassigned", "Meeting", "standup", null);

        List<I_Timeslice> s = slices(line);
        assertThat(s).extracting(I_Timeslice::getDuration)
            .containsExactly(90 * 60_000L, 90 * 60_000L + 20_000L, 0L);
        assertThat(s.get(1).getTypeId()).isEqualTo(MEETING);
    }

    @Test
    void insertWithEndResumesInterruptedSlice()
    {
        I_Timeline line = timeline(NOW.minusHours(2), slice("12:00", taz, CODING, "home-5122"));

        String result = service.insertSlice("1:00 PM", "", "meeting", null, "1:20pm");

        assertThat(result).contains("'TAZ' (Coding) resumes at 1:20 PM");
        List<I_Timeslice> s = slices(line);
        assertThat(s).extracting(I_Timeslice::getDuration).containsExactly(3_600_000L, 20 * 60_000L, 0L);
        assertThat(s.get(2).getTaskId()).isEqualTo(taz.getId());
        assertThat(s.get(2).getNote()).isEqualTo("home-5122");
    }

    @Test
    void insertRejectsConflicts()
    {
        timeline(NOW.minusHours(5), slice("09:00", taz, CODING, null), slice("12:00:45", bs, CODING, null));

        assertThatThrownBy(() -> service.insertSlice("12:00", "TAZ", null, null, null))
            .hasMessageContaining("already starts");
        assertThatThrownBy(() -> service.insertSlice("11:00", "TAZ", null, null, "12:30"))
            .hasMessageContaining("runs past the next slice");
        assertThatThrownBy(() -> service.insertSlice("8:00", "TAZ", null, null, null))
            .hasMessageContaining("before the timeline's first slice");
        assertThatThrownBy(() -> service.insertSlice("15:00", "TAZ", null, null, null))
            .hasMessageContaining("future");
        assertThatThrownBy(() -> service.insertSlice("2026-09-29 10:00", "TAZ", null, null, null))
            .hasMessageContaining("No timeline exists for 2026-09-29");
    }

    @Test
    void tagsSliceRunningAtAMinute()
    {
        I_Timeline line = timeline(NOW.minusHours(5),
            slice("09:00", taz, CODING, null), slice("12:00:45", bs, CODING, "home-5134"));

        service.tagSlice("12:00", "home-9");
        service.tagSlice("11:59", "home-8");

        assertThat(slices(line)).extracting(I_Timeslice::getNote).containsExactly("home-8", "home-5134 home-9");
    }

    @Test
    void editChangesOnlyFieldsPassed()
    {
        I_Timeline line = timeline(NOW.minusHours(5),
            slice("09:00", taz, MEETING, "home-5122 nullaway"), slice("12:00", bs, CODING, null));

        String result = service.editSlice("10:15", "BS", null, null, null);

        assertThat(result).contains("task TAZ -> BS");
        I_Timeslice edited = slices(line).get(0);
        assertThat(edited.getTaskId()).isEqualTo(bs.getId());
        assertThat(edited.getTypeId()).isEqualTo(MEETING);
        assertThat(edited.getNote()).isEqualTo("home-5122 nullaway");

        service.editSlice("10:15", null, "Coding", "", null);
        assertThat(slices(line).get(0).getTypeId()).isEqualTo(CODING);
        assertThat(slices(line).get(0).getNote()).isNull();
        assertThat(service.editSlice("10:15", "BS", null, null, null)).startsWith("Nothing changed");
    }

    @Test
    void editMovesStartBetweenNeighbours()
    {
        I_Timeline line = timeline(NOW.minusHours(5),
            slice("09:00", taz, CODING, null), slice("11:00", bs, CODING, null), slice("12:00", taz, CODING, null));

        service.editSlice("11:30", null, null, null, "10:30");

        assertThat(slices(line)).extracting(I_Timeslice::getDuration)
            .containsExactly(90 * 60_000L, 90 * 60_000L, 0L);
        assertThatThrownBy(() -> service.editSlice("11:00", null, null, null, "12:00"))
            .hasMessageContaining("before the next slice's start at 12:00 PM");
        assertThatThrownBy(() -> service.editSlice("11:00", null, null, null, "9:00"))
            .hasMessageContaining("after the previous slice's start at 9:00 AM");
        assertThatThrownBy(() -> service.editSlice("11:00", null, null, null, null))
            .hasMessageContaining("Nothing to change");
    }

    @Test
    void editMovesFirstSliceAndTimelineStart()
    {
        I_Timeline line = timeline(NOW.minusHours(5), slice("09:00", taz, CODING, null), slice("12:00", bs, CODING, null));

        service.editSlice("9:00", null, null, null, "8:30");

        assertThat(slices(line).get(0).getDuration()).isEqualTo(210 * 60_000L);
        assertThat(dao.getLatestTimeline().getStart()).isEqualTo(Timestamp.valueOf(NOW.withHour(8).withMinute(30)));
    }

    // ---- fixtures ----

    private I_Timeline timeline(LocalDateTime start, I_Timeslice... slices)
    {
        I_Timeline line = dao.createTimeline(Timestamp.valueOf(start));
        List<I_Timeslice> list = new ArrayList<>(List.of(slices));
        for (int i = 0; i < list.size(); i++)
        {
            list.get(i).setTimelineId(line.getId());
            if (i + 1 < list.size())
                list.get(i).setDuration(list.get(i + 1).getStart().getTime() - list.get(i).getStart().getTime());
        }
        line.setSlices(list);
        dao.saveTimeline(line);
        return line;
    }

    private I_Timeslice slice(String time, I_Task task, int type, String note)
    {
        String t = time.length() == 5 ? time + ":00" : time;
        return slice(LocalDateTime.of(NOW.toLocalDate(), java.time.LocalTime.parse(t)), task, type, note);
    }

    private I_Timeslice slice(LocalDateTime start, I_Task task, int type, String note)
    {
        I_Timeslice s = dao.newTimeslice(I_Timeline.NO_LINE_ID, Timestamp.valueOf(start));
        s.setTaskId(task == null ? I_Task.NO_TASK_ID : task.getId());
        s.setTypeId(type);
        s.setNote(note);
        return s;
    }

    private List<I_Timeslice> slices(I_Timeline line)
    {
        dao.getSlices(line);
        List<I_Timeslice> result = new ArrayList<>();
        for (Object o : line.getSlices())
            result.add((I_Timeslice) o);
        return result;
    }

    private List<String> notes()
    {
        return jdbc.queryForList("SELECT note FROM timeslice ORDER BY start", String.class);
    }
}
