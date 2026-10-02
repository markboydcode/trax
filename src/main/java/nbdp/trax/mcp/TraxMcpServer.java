package nbdp.trax.mcp;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.Clock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;
import nbdp.trax.data.Period;
import nbdp.trax.report.CompositeReportResult;
import nbdp.trax.report.CompositeTime;
import nbdp.trax.report.ReportEngine;
import nbdp.trax.report.ReportEntry;
import nbdp.trax.report.ReportResult;

import nbdp.trax.TraxApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MCP (Model Context Protocol) server for Trax. Exposes time-tracking
 * data and reports as tools that can be called by Claude Code or other
 * MCP clients via stdio transport.
 *
 * @author Mark Boyd
 */
public class TraxMcpServer
{
    private static final DecimalFormat DECIMAL = new DecimalFormat("0.00");
    private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("yyyy.MM.dd hh:mm a");

    private static final JsonSchema DATE_RANGE_SCHEMA = new JsonSchema(
        "object",
        Map.of(
            "startDate", Map.of("type", "string", "description", "Start date in YYYY-MM-DD format"),
            "endDate", Map.of("type", "string", "description", "End date in YYYY-MM-DD format")
        ),
        List.of("startDate", "endDate"),
        false, null, null
    );

    private static final JsonSchema EMPTY_SCHEMA = new JsonSchema(
        "object", Map.of(), List.of(), false, null, null
    );

    private static final String INSTRUCTIONS = """
        Trax holds the user's single time-tracking timeline, shared by all of their \
        Claude Code sessions. Each slice's note must name every Jira ticket worked \
        during it, because Jira time is later split across the tickets a note names.

        - tag_current_slice is safe: call it without asking. It is idempotent.
        - start_slice, continue_slice, stop_slice, insert_slice, edit_slice, delete_slice and tag_slice change the user's \
        timeline. Call them only when the user asks, or after asking and getting a yes.
        - The first time this session starts work on a Jira ticket, ask the user \
        whether to tag the current slice with it or start a new slice, then follow \
        that answer for the rest of the session. If the answer was to tag, call \
        tag_current_slice again whenever you resume work on the ticket, since the \
        user may have switched slices in the meantime.
        """;

    private static final Map<String, Object> TICKET_PROP = Map.of(
        "type", "string", "description", "Jira ticket key, e.g. home-5156");
    private static final Map<String, Object> TASK_PROP = Map.of(
        "type", "string", "description",
        "Task name (exact, case-insensitive) or numeric id from get_tasks; 'Unassigned' for none");
    private static final Map<String, Object> TYPE_PROP = Map.of(
        "type", "string", "description",
        "Activity type name from get_types; defaults to the task's type");
    private static final Map<String, Object> NOTE_PROP = Map.of(
        "type", "string", "description", "Note; start it with the ticket keys worked, e.g. 'home-5156 jspecify'");
    private static final Map<String, Object> INDEX_PROP = Map.of(
        "type", "integer", "description",
        "Only when several slices start in the named minute: which one, 1 for the oldest. "
            + "Without it such a minute is refused with the candidates listed in order.");
    private static final String TIME_FORMATS =
        "13:00, 1:00 PM, or with a date: 2026-10-01 13:00 (no date means today)";

    public static void main(String[] args) throws InterruptedException
    {
        ConfigurableApplicationContext ctx = SpringApplication.run(TraxApplication.class, args);
        I_TraxDao dao = ctx.getBean(I_TraxDao.class);
        ReportEngine engine = ctx.getBean(ReportEngine.class);
        SliceService sliceService = new SliceService(dao, ctx.getBean(TransactionTemplate.class),
            Clock.systemDefaultZone());

        // Build MCP server
        StdioServerTransportProvider transport = new StdioServerTransportProvider(
            new io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper(
                tools.jackson.databind.json.JsonMapper.builder().build()));

        McpSyncServer server = McpServer.sync(transport)
            .serverInfo("trax", "1.0.0")
            .capabilities(ServerCapabilities.builder().tools(true).build())
            .instructions(INSTRUCTIONS)

            // --- get_time_entries ---
            .toolCall(
                Tool.builder()
                    .name("get_time_entries")
                    .description("Get raw time entries for a date range. Returns each timeslice with date, duration, activity type, task, and note.")
                    .inputSchema(DATE_RANGE_SCHEMA)
                    .build(),
                (exchange, request) -> {
                    Period period = parsePeriod(request.arguments());
                    List slices = dao.getSlicesInPeriod(period);
                    String result = formatTimeEntries(slices, dao);
                    return CallToolResult.builder().content(List.of(new McpSchema.TextContent(result))).build();
                }
            )

            // --- get_time_by_type ---
            .toolCall(
                Tool.builder()
                    .name("get_time_by_type")
                    .description("Get time summary grouped by activity type (Coding, Meeting, Email, etc.) for a date range.")
                    .inputSchema(DATE_RANGE_SCHEMA)
                    .build(),
                (exchange, request) -> {
                    Period period = parsePeriod(request.arguments());
                    List slices = dao.getSlicesInPeriod(period);
                    ReportResult report = engine.summarizeByType(slices);
                    String result = formatReportResult("Activity Type", report);
                    return CallToolResult.builder().content(List.of(new McpSchema.TextContent(result))).build();
                }
            )

            // --- get_time_by_task ---
            .toolCall(
                Tool.builder()
                    .name("get_time_by_task")
                    .description("Get time summary grouped by task for a date range.")
                    .inputSchema(DATE_RANGE_SCHEMA)
                    .build(),
                (exchange, request) -> {
                    Period period = parsePeriod(request.arguments());
                    List slices = dao.getSlicesInPeriod(period);
                    ReportResult report = engine.summarizeByTask(slices);
                    String result = formatReportResult("Task", report);
                    return CallToolResult.builder().content(List.of(new McpSchema.TextContent(result))).build();
                }
            )

            // --- get_time_by_task_hierarchy ---
            .toolCall(
                Tool.builder()
                    .name("get_time_by_task_hierarchy")
                    .description("Get hierarchical time summary by task for a date range, showing direct time (on task itself) and indirect time (on subtasks).")
                    .inputSchema(DATE_RANGE_SCHEMA)
                    .build(),
                (exchange, request) -> {
                    Period period = parsePeriod(request.arguments());
                    CompositeReportResult report = engine.summarizeByTaskComposite(period);
                    String result = formatCompositeResult(report);
                    return CallToolResult.builder().content(List.of(new McpSchema.TextContent(result))).build();
                }
            )

            // --- get_tasks ---
            .toolCall(
                Tool.builder()
                    .name("get_tasks")
                    .description("List all tasks in the task hierarchy. Shows task names, IDs, and nesting structure.")
                    .inputSchema(EMPTY_SCHEMA)
                    .build(),
                (exchange, request) -> {
                    String result = formatTaskTree(dao);
                    return CallToolResult.builder().content(List.of(new McpSchema.TextContent(result))).build();
                }
            )

            // --- get_current_slice ---
            .toolCall(
                Tool.builder()
                    .name("get_current_slice")
                    .description("Get the slice the user is recording now: task, type, start time and note.")
                    .inputSchema(EMPTY_SCHEMA)
                    .build(),
                (exchange, request) -> run(sliceService::getCurrentSlice)
            )

            // --- get_types ---
            .toolCall(
                Tool.builder()
                    .name("get_types")
                    .description("List the activity types a slice may have.")
                    .inputSchema(EMPTY_SCHEMA)
                    .build(),
                (exchange, request) -> run(sliceService::getTypes)
            )

            // --- tag_current_slice ---
            .toolCall(
                Tool.builder()
                    .name("tag_current_slice")
                    .description("Add a Jira ticket key to the note of the slice being recorded now, "
                        + "unless the note already names it. Safe to call without asking the user; idempotent.")
                    .inputSchema(schema(Map.of("ticket", TICKET_PROP), "ticket"))
                    .build(),
                (exchange, request) -> run(() -> sliceService.tagCurrentSlice(arg(request.arguments(), "ticket")))
            )

            // --- tag_slice ---
            .toolCall(
                Tool.builder()
                    .name("tag_slice")
                    .description("Add a Jira ticket key to the note of the slice that was running at a past time. "
                        + "Changes the user's timeline: call only when the user asks or agrees.")
                    .inputSchema(schema(Map.of(
                        "time", Map.of("type", "string", "description", "A time within the slice: " + TIME_FORMATS),
                        "index", INDEX_PROP, "ticket", TICKET_PROP), "time", "ticket"))
                    .build(),
                (exchange, request) -> run(() -> sliceService.tagSlice(arg(request.arguments(), "time"),
                    intArg(request.arguments(), "index"), arg(request.arguments(), "ticket")))
            )

            // --- start_slice ---
            .toolCall(
                Tool.builder()
                    .name("start_slice")
                    .description("End the slice being recorded and start a new one now. Starts today's "
                        + "timeline if there is none. Changes the user's timeline: call only when the user asks or agrees.")
                    .inputSchema(schema(Map.of("task", TASK_PROP, "type", TYPE_PROP, "note", NOTE_PROP), "task"))
                    .build(),
                (exchange, request) -> run(() -> sliceService.startSlice(arg(request.arguments(), "task"),
                    arg(request.arguments(), "type"), arg(request.arguments(), "note")))
            )

            // --- continue_slice ---
            .toolCall(
                Tool.builder()
                    .name("continue_slice")
                    .description("Continue an earlier slice, like trax's Continue: end the slice being "
                        + "recorded and start a new one now with the task, type and note of the slice that "
                        + "was running at the given time. Changes the user's timeline: call only when the "
                        + "user asks or agrees.")
                    .inputSchema(schema(Map.of(
                        "time", Map.of("type", "string", "description",
                            "A time within the slice to continue: " + TIME_FORMATS),
                        "index", INDEX_PROP), "time"))
                    .build(),
                (exchange, request) -> run(() -> sliceService.continueSlice(arg(request.arguments(), "time"),
                    intArg(request.arguments(), "index")))
            )

            // --- stop_slice ---
            .toolCall(
                Tool.builder()
                    .name("stop_slice")
                    .description("End the slice being recorded by going Off-line now. "
                        + "Changes the user's timeline: call only when the user asks or agrees.")
                    .inputSchema(EMPTY_SCHEMA)
                    .build(),
                (exchange, request) -> run(sliceService::stopSlice)
            )

            // --- insert_slice ---
            .toolCall(
                Tool.builder()
                    .name("insert_slice")
                    .description("Insert a slice the user forgot to record, on any past day. "
                        + "The slice it lands in is cut short at the start. Without an end, the new slice "
                        + "runs until the next slice; with one, the interrupted slice resumes at the end, or "
                        + "the day goes Off-line there if nothing was interrupted (before the day's first "
                        + "slice). On a day with no timeline, one is created and an end is required, e.g. "
                        + "a vacation day from 8:00 to 16:00. "
                        + "Changes the user's timeline: call only when the user asks or agrees.")
                    .inputSchema(schema(Map.of(
                        "start", Map.of("type", "string", "description", "Start time: " + TIME_FORMATS),
                        "end", Map.of("type", "string", "description",
                            "Optional end time, after which the interrupted slice resumes: " + TIME_FORMATS),
                        "task", TASK_PROP, "type", TYPE_PROP, "note", NOTE_PROP), "start", "task"))
                    .build(),
                (exchange, request) -> run(() -> sliceService.insertSlice(arg(request.arguments(), "start"),
                    arg(request.arguments(), "task"), arg(request.arguments(), "type"),
                    arg(request.arguments(), "note"), arg(request.arguments(), "end")))
            )

            // --- delete_slice ---
            .toolCall(
                Tool.builder()
                    .name("delete_slice")
                    .description("Delete the slice that was running at a given time. The slice before it "
                        + "grows to cover the gap, as in trax's Delete. A timeline's only slice cannot be "
                        + "deleted. Changes the user's timeline: call only when the user asks or agrees.")
                    .inputSchema(schema(Map.of(
                        "time", Map.of("type", "string", "description", "A time within the slice: " + TIME_FORMATS),
                        "index", INDEX_PROP), "time"))
                    .build(),
                (exchange, request) -> run(() -> sliceService.deleteSlice(arg(request.arguments(), "time"),
                    intArg(request.arguments(), "index")))
            )

            // --- edit_slice ---
            .toolCall(
                Tool.builder()
                    .name("edit_slice")
                    .description("Change the slice that was running at a given time: its task, type, note, "
                        + "or start time. Only the fields passed change; an empty note clears it, and changing "
                        + "the task keeps the type unless a type is passed too. A new start must stay between "
                        + "the neighbouring slices' starts. Changes the user's timeline: call only when the "
                        + "user asks or agrees.")
                    .inputSchema(schema(Map.of(
                        "time", Map.of("type", "string", "description", "A time within the slice: " + TIME_FORMATS),
                        "index", INDEX_PROP,
                        "task", TASK_PROP,
                        "type", Map.of("type", "string", "description", "Activity type name from get_types"),
                        "note", Map.of("type", "string", "description", "Replacement note; empty clears it"),
                        "start", Map.of("type", "string", "description", "New start time: " + TIME_FORMATS)),
                        "time"))
                    .build(),
                (exchange, request) -> run(() -> sliceService.editSlice(arg(request.arguments(), "time"),
                    intArg(request.arguments(), "index"), arg(request.arguments(), "task"), arg(request.arguments(), "type"),
                    arg(request.arguments(), "note"), arg(request.arguments(), "start")))
            )

            .build();

        // Block until stdin is closed (MCP client disconnects)
        Thread.currentThread().join();
    }

    private static JsonSchema schema(Map<String, Object> properties, String... required)
    {
        return new JsonSchema("object", properties, List.of(required), false, null, null);
    }

    private static String arg(Map<String, Object> args, String name)
    {
        Object value = args == null ? null : args.get(name);
        return value == null ? null : value.toString();
    }

    private static Integer intArg(Map<String, Object> args, String name)
    {
        Object value = args == null ? null : args.get(name);
        if (value == null)
            return null;
        if (value instanceof Number n)
            return n.intValue();
        try
        {
            return (int) Double.parseDouble(value.toString().trim());
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(name + " must be a whole number.");
        }
    }

    /** Runs a tool, reporting validation failures to the client as tool errors. */
    private static CallToolResult run(Supplier<String> tool)
    {
        try
        {
            return CallToolResult.builder().content(List.of(new McpSchema.TextContent(tool.get()))).build();
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            return CallToolResult.builder().content(List.of(new McpSchema.TextContent(e.getMessage())))
                .isError(true).build();
        }
    }

    private static Period parsePeriod(Map<String, Object> args)
    {
        String startStr = (String) args.get("startDate");
        String endStr = (String) args.get("endDate");
        Timestamp start = Timestamp.valueOf(startStr + " 00:00:00");
        Timestamp end = Timestamp.valueOf(endStr + " 23:59:59.999");
        return new Period(start, end);
    }

    private static String formatTimeEntries(List slices, I_TraxDao dao)
    {
        if (slices == null || slices.isEmpty())
        {
            return "No time entries found for the specified period.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-22s  %-10s  %-15s  %-20s  %s\n",
            "Date", "Hours", "Type", "Task", "Note"));
        sb.append("-".repeat(90)).append("\n");

        for (Object obj : slices)
        {
            I_Timeslice slice = (I_Timeslice) obj;
            if (slice.getTypeId() == I_Type.OFFLINE_TYPE_ID)
                continue;

            String date = TIMESTAMP.format(new Date(slice.getStart().getTime()));
            String hours = DECIMAL.format(slice.getDuration() / 3600000.0);
            String type = dao.getTypeById(slice.getTypeId()).getName();
            String task;
            if (slice.getTaskId() == I_Task.NO_TASK_ID)
                task = "Unassigned";
            else
            {
                I_Task t = dao.getTask(slice.getTaskId());
                task = t != null ? t.getName() : "Unknown";
            }
            String note = slice.getNote() != null ? slice.getNote() : "";

            sb.append(String.format("%-22s  %-10s  %-15s  %-20s  %s\n",
                date, hours, type, task, note));
        }

        return sb.toString();
    }

    private static String formatReportResult(String groupLabel, ReportResult report)
    {
        if (report.isEmpty())
        {
            return "No time entries found for the specified period.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Period: ")
            .append(TIMESTAMP.format(new Date(report.getEarliest())))
            .append(" to ")
            .append(TIMESTAMP.format(new Date(report.getLatest())))
            .append("\n");
        sb.append("Total: ").append(DECIMAL.format(report.getTotalHours())).append(" hours\n\n");
        sb.append(String.format("%-30s  %s\n", groupLabel, "Hours"));
        sb.append("-".repeat(45)).append("\n");

        for (ReportEntry entry : report.getEntries())
        {
            sb.append(String.format("%-30s  %s\n", entry.getLabel(), DECIMAL.format(entry.getHours())));
        }

        return sb.toString();
    }

    private static String formatCompositeResult(CompositeReportResult report)
    {
        if (report.isEmpty())
        {
            return "No time entries found for the specified period.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Period: ")
            .append(TIMESTAMP.format(new Date(report.getEarliest())))
            .append(" to ")
            .append(TIMESTAMP.format(new Date(report.getLatest())))
            .append("\n");
        sb.append("Total: ").append(DECIMAL.format(report.getTotalHours())).append(" hours\n\n");

        CompositeTime root = report.getRoot();
        long allTime = root.getIndirectTime();

        for (Iterator it = root.iterator(); it.hasNext(); )
        {
            CompositeTime child = (CompositeTime) it.next();
            formatCompositeNode(sb, child, allTime, 0);
        }

        return sb.toString();
    }

    private static void formatCompositeNode(StringBuilder sb, CompositeTime ct, long allTime, int depth)
    {
        String indent = "  ".repeat(depth);
        String name = ct.getLabelBase();
        String total = DECIMAL.format(ct.getTotalTime() / 3600000.0);
        String percent = DECIMAL.format((100.0 * ct.getTotalTime()) / allTime);

        if (ct.getTotalTime() == ct.getDirectTime())
        {
            sb.append(String.format("%s%s hrs (%s%%)  %s  (%d entries)\n",
                indent, total, percent, name, ct.getDirectContributions()));
        }
        else
        {
            String direct = DECIMAL.format(ct.getDirectTime() / 3600000.0);
            String indirect = DECIMAL.format(ct.getIndirectTime() / 3600000.0);
            sb.append(String.format("%s%s hrs (%s%%)  %s  [direct: %s, indirect: %s, %d entries]\n",
                indent, total, percent, name, direct, indirect, ct.getDirectContributions()));
        }

        for (Iterator it = ct.iterator(); it.hasNext(); )
        {
            CompositeTime child = (CompositeTime) it.next();
            formatCompositeNode(sb, child, allTime, depth + 1);
        }
    }

    private static String formatTaskTree(I_TraxDao dao)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Task Hierarchy\n");
        sb.append("==============\n\n");

        List rootTasks = dao.getSubTasks(I_Task.ROOT_TASK_PARENT_ID);
        if (rootTasks == null || rootTasks.isEmpty())
        {
            return "No tasks found.";
        }

        for (Object obj : rootTasks)
        {
            I_Task task = (I_Task) obj;
            formatTaskNode(sb, task, dao, 0);
        }

        return sb.toString();
    }

    private static void formatTaskNode(StringBuilder sb, I_Task task, I_TraxDao dao, int depth)
    {
        String indent = "  ".repeat(depth);
        String completed = task.getIsCompleted() ? " [completed]" : "";
        sb.append(String.format("%s- %s (id:%d)%s\n", indent, task.getName(), task.getId(), completed));

        List children = dao.getSubTasks(task.getId());
        if (children != null)
        {
            for (Object obj : children)
            {
                I_Task child = (I_Task) obj;
                formatTaskNode(sb, child, dao, depth + 1);
            }
        }
    }
}