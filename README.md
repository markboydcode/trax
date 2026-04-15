# Trax

A desktop time-tracking application built with Java Swing. Trax lets you record daily work activity organized by timelines (sessions), timeslices (intervals), tasks (hierarchical), and activity types (Coding, Meeting, Email, etc.), and generate reports summarizing time by type or task.

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

## Data Model

- **Timeline** — a work session with start/stop timestamps
- **Timeslice** — a time interval within a timeline, with duration, task, activity type, and optional note
- **Task** — a hierarchical work item (supports parent/child nesting)
- **Type** — activity category (Coding, Meeting, Email, Review, Analysis, Design, etc.)

## Reports

- **Time by Type** — hours aggregated by activity category
- **Time by Task** — hours aggregated by task
- **Time by Task (hierarchical)** — nested breakdown showing direct and indirect time per task