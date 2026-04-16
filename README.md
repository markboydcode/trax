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

> **Note:** The relative datasource path (`./db/trax`) works when running from the trax directory (Swing UI). If you plan to register the MCP server with Claude Code or another MCP client, use an absolute path instead — see [Registering with Claude Code](#registering-with-claude-code) below.

### Database

Trax uses an embedded H2 database stored in `db/trax.h2.db`. The database directory is excluded from version control since it contains personal time data. On first run, the database and tables are created automatically.

## Building

```bash
mvn clean package
```

## Running

### Swing UI

```bash
./start.sh
```

Or manually:

```bash
java -jar target/trax-6.0.0.jar
```

### MCP Server

The MCP server exposes time-tracking data as tools for Claude Code or other MCP clients:

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