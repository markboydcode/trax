# Trax

A desktop time-tracking application built with Java Swing. Trax lets you record daily work activity organized by timelines (sessions), timeslices (intervals), tasks (hierarchical), and activity types (Coding, Meeting, Email, etc.), and generate reports summarizing time by type or task.

## Prerequisites

- Java (originally targeting JDK 5+, runs on modern JVMs)
- Maven 2.0+
- Local JAR dependencies in `jars/` (not committed — see `pom.xml` for system-scoped references)

## Setup

### Spring Configuration

Trax uses a Spring XML configuration file (`spring-cfg.xml`) to wire the data source and MyBatis DAO. This file is excluded from version control because it contains local database credentials.

You must create `spring-cfg.xml` in the project root with the following structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN"
    "http://www.springframework.org/dtd/spring-beans.dtd">

<beans>
    <bean id="configHolder" class="nbdp.trax.cfg.Holder">
        <property name="config">
            <map>
                <entry key="window.classpath.image.file"
                       value="images/alarmClockLrg.gif"/>
            </map>
        </property>
    </bean>

    <bean id="dataSource"
          class="org.apache.commons.dbcp.BasicDataSource"
          destroy-method="close">
        <property name="driverClassName" value="org.h2.Driver"/>
        <property name="url" value="jdbc:h2:file:./db/trax"/>
        <property name="username" value="YOUR_USERNAME"/>
        <property name="password" value="YOUR_PASSWORD"/>
    </bean>

    <bean id="sqlSessionFactory"
          class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="mapperLocations" value="classpath:mybatis-traxDao.xml"/>
    </bean>

    <bean id="traxDao" class="nbdp.trax.data.ibatis.TraxDao">
        <property name="sqlSessionFactory" ref="sqlSessionFactory"/>
    </bean>
</beans>
```

Replace `YOUR_USERNAME` and `YOUR_PASSWORD` with your chosen H2 credentials. On first run, the database and tables are created automatically.

### Database

Trax uses an embedded H2 database stored in `db/trax.h2.db`. The database directory is excluded from version control since it contains personal time data.

## Running

```bash
./start.sh
```

Or manually:

```bash
java -cp <classpath> nbdp.trax.TimelineView -cfg spring-cfg.xml
```

## Building

```bash
mvn clean package
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