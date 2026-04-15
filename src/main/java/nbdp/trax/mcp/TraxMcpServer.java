package nbdp.trax.mcp;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    public static void main(String[] args) throws InterruptedException
    {
        ConfigurableApplicationContext ctx = SpringApplication.run(TraxApplication.class, args);
        I_TraxDao dao = ctx.getBean(I_TraxDao.class);
        ReportEngine engine = ctx.getBean(ReportEngine.class);

        // Build MCP server
        StdioServerTransportProvider transport = new StdioServerTransportProvider(
            new io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper(
                tools.jackson.databind.json.JsonMapper.builder().build()));

        McpSyncServer server = McpServer.sync(transport)
            .serverInfo("trax", "1.0.0")
            .capabilities(ServerCapabilities.builder().tools(true).build())

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

            .build();

        // Block until stdin is closed (MCP client disconnects)
        Thread.currentThread().join();
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