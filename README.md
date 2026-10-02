# Trax

A desktop time-tracking application built with Java Swing. Trax lets you record daily work activity organized by timelines (sessions), timeslices (intervals), tasks (hierarchical), and activity types (Coding, Meeting, Email, etc.), and generate reports summarizing time by type or task. It also includes an [MCP server](#mcp-server) that exposes time data to Claude Code and other MCP clients.

## Prerequisites

- Java 17+
- Maven 3.6+

## Setup

### Configuration

Copy the template and fill in your database credentials:

```bash
cp application.properties.template application.properties
```

Edit `application.properties`:

```properties
spring.datasource.url=jdbc:h2:file:./db/trax;AUTO_SERVER=TRUE
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.h2.Driver
```

This file is excluded from version control since it contains credentials.

> **Note:** The macOS app bundle and MCP server both require an **absolute** datasource path since they don't run from the trax directory. See [Registering with Claude Code](#registering-with-claude-code) below.

### Database

Trax uses an embedded H2 database stored in `db/trax.mv.db`. The database directory is excluded from version control since it contains personal time data. On first run, the database and tables are created automatically.

## Building

```bash
mvn clean package
```

## Running

### Swing UI

#### macOS (recommended)

Double-click `Trax.app` in the project directory, or launch from the terminal:

```bash
open Trax.app
```

This shows "Trax" as the app name in the Dock and app switcher with a proper icon. To keep it accessible:

- **Dock** — right-click the running Trax icon in the Dock and choose "Options > Keep in Dock"
- **Applications** — drag `Trax.app` to `/Applications` or `~/Applications`

#### Command line

For non-macOS platforms or if you prefer the terminal:

```bash
./start.sh
```

> **Note:** When launched this way, macOS shows "java" as the app name in the Dock and app switcher instead of "Trax".

For detailed usage instructions including report field explanations, see the [User Guide](docs/user-guide.md).

### MCP Server

The MCP server exposes time-tracking data as tools for Claude Code or other MCP clients.

Read tools: `get_time_entries`, `get_time_by_type`, `get_time_by_task`, `get_time_by_task_hierarchy`, `get_tasks`, `get_types` and `get_current_slice`.

Write tools let several Claude Code sessions keep the current timeline's notes naming the Jira tickets they work on:

| Tool | Effect |
|------|--------|
| `tag_current_slice(ticket)` | Adds a ticket key to the running slice's note unless it is already there |
| `tag_slice(time, index, ticket)` | Same, for the slice running at a past time |
| `start_slice(task, type, note)` | Ends the running slice and starts a new one now |
| `continue_slice(time, index)` | Like trax's Continue: starts a new slice now copying the task, type and note of the slice running at `time` |
| `stop_slice()` | Ends the running slice by going Off-line |
| `insert_slice(start, task, type, note, end)` | Inserts a forgotten slice on any past day; with `end`, the interrupted slice resumes afterward, or the day goes Off-line. On a day with no timeline, creates one (`end` required), e.g. for a vacation day |
| `delete_slice(time, index)` | Deletes a slice; the slice before it covers the gap |
| `edit_slice(time, index, task, type, note, start)` | Changes the slice running at `time`; only the fields passed change |

Times are named to the minute, as the UI shows them. When several slices start in the named minute, the tools that pick a slice refuse and list the candidates oldest first; pass `index` (1 for the oldest) to pick one.

The server's instructions tell sessions that only tagging the current slice may happen without asking; the other write tools change the timeline and need the user's say-so. Each write is one transaction that locks the timeline, so concurrent sessions and the Swing UI never overwrite one another, and the UI picks up outside changes within 10 seconds.

```bash
./start-mcp.sh
```

Or manually:

```bash
java -Dloader.main=nbdp.trax.mcp.TraxMcpServer -jar target/trax-6.0.0.jar
```

#### Registering with Claude Code

Claude Code does not honor the `cwd` field in MCP configuration, so the server process may not run from the trax directory. To work around this, the `-Dspring.config.additional-location` JVM argument tells Spring Boot where to find `application.properties`, and the datasource URL in that file must use an absolute path.

1. Ensure your `application.properties` uses an **absolute** datasource path:

   ```properties
   spring.datasource.url=jdbc:h2:file:/path/to/trax/db/trax;AUTO_SERVER=TRUE
   ```

2. Add the following to `~/.mcp.json` (create the file if it doesn't exist), replacing `/path/to/trax` with your actual trax installation directory:

   ```json
   {
     "mcpServers": {
       "trax": {
         "command": "java",
         "args": [
           "-Dloader.main=nbdp.trax.mcp.TraxMcpServer",
           "-Dspring.config.additional-location=file:/path/to/trax/application.properties",
           "-jar",
           "/path/to/trax/target/trax-6.0.0.jar"
         ]
       }
     }
   }
   ```

3. Restart Claude Code (not just `/mcp` reconnect) for the configuration to take effect.

The `db/` directory and H2 database file are created automatically on first run — no need to create them in advance.

## Data Model

- **Timeline** — a work session with start/stop timestamps
- **Timeslice** — a time interval within a timeline, with duration, task, activity type, and optional note
- **Task** — a hierarchical work item (supports parent/child nesting)
- **Type** — activity category (Coding, Meeting, Email, Review, Analysis, Design, etc.)

## Reports

- **Time by Type** — hours aggregated by activity category
- **Time by Task** — hours aggregated by task
- **Time by Task (hierarchical)** — nested breakdown showing direct and indirect time per task