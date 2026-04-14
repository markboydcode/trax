package nbdp.trax.data.ibatis;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import nbdp.trax.data.I_Task;
import nbdp.trax.data.I_Timeline;
import nbdp.trax.data.I_Timeslice;
import nbdp.trax.data.I_TraxDao;
import nbdp.trax.data.I_Type;
import nbdp.trax.data.Period;
import nbdp.trax.data.ViewAspect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.jdbc.BadSqlGrammarException;

/**
 * Provides access to trax DB using IBatis constructs.
 *
 * @author mboyd
 */
public class TraxDao extends SqlSessionDaoSupport implements I_TraxDao
{
	private static Log cLog = LogFactory.getLog(TraxDao.class);

	
    @Override
	protected void initDao() throws Exception {
		super.initDao();
		
		SqlSession session = this.getSqlSession();
		try {
			cLog.info("------------ verifying DB is loaded!");
			String name = (String) session.selectOne("trax.verifyTypesTableInstalledAndLoaded");
			cLog.info("------------ DB is ready for business!");
		} catch (BadSqlGrammarException bge) {
			// if thrown then the types table does not exist and we need to load the DB
			cLog.info("DETECTED TRAX DATABASE NOT YET LOADED...lOADING");
			for(int i=1; i<=6; i++) {
				session.update("create-" + i);
			}
			session.update("init-cfg");
			session.update("init-types");
			
			// verify load completed properly
			String name = (String) session.selectOne("trax.verifyTypesTableInstalledAndLoaded");
			if (name != null && name.equals("Misc.")) {
				cLog.info("------------ DB is ready for business!");
			}
			else {
				throw new IllegalStateException("Unable to verify that DB initialized properly.");
			}
		}
		/*
		Connection conn = session.getConnection();
		if (conn.isClosed()) {
			throw new IllegalStateException("Received CLOSED connection from SqlSession.");
		}
		DatabaseMetaData dmd = conn.getMetaData();
		ResultSet rs = dmd.getTables(null, null, "timeslice", new String[] {"table"});
		
		if (! rs.next()) {
			// db needs to be loaded
			ClassLoader cl = this.getClass().getClassLoader();
			InputStream is = cl.getResourceAsStream("tables.sql");
			
			if (is == null) {
				throw new IllegalStateException("Unable to initialize database due to missing classpath resource tables.sql.");
			}
			BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
			StringWriter sql = new StringWriter();
			String line = rdr.readLine();
			
			while (line != null) {
				line = line.trim();

				if (line.length() != 0) {
					sql.append(line);
				}
				if (line.endsWith(";")) {
					Statement s = conn.createStatement();
					s.execute(sql.toString());
					s.close();
					sql = new StringWriter();
				}
				line = rdr.readLine();
			};
		}
		*/
	}

	/* (non-Javadoc)
     * @see nbdp.trax.data.I_TimelineDao#getTimeline(int)
     */
    public I_Timeline getTimeline(int timelineId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    private int getNextLineId(SqlSession session)
    {
        Integer wrap = (Integer) session.selectOne(
                "trax.nextLineId");
        int id = wrap.intValue();
        session.update("trax.updateNextLineId", id + 1);
        return id;
    }

    public I_Timeline createTimeline(Timestamp start)
    {
        DbTimeline line = new DbTimeline();
        line.setStart(start);
        line.setStop(start);
        // see if a timeline exists with the same start timestamp
        SqlSession session = this.getSqlSession();
        DbTimeline dbTl = (DbTimeline) session.selectOne("trax.getTimelineByStart",
                line);
        if (dbTl != null)
        {
            return dbTl;
        }
        // nope. so generate a new line id.
        line.setId(getNextLineId(session));

        // now insert into DB
        session.insert("trax.createTimeline", line);
        return line;
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TimelineDao#getCurrentTimeline()
     */
    public I_Timeline getCurrentTimeline()
    {
        SqlSession session = this.getSqlSession();
        Integer id = (Integer) session.selectOne("trax.getCurrentTimeline");

        return null;
    }

    public I_Timeline createCurrentTimeline()
    {
        long time = System.currentTimeMillis();
        Timestamp start = new Timestamp(time);
        I_Timeline line = createTimeline(start);
        // now set as current
        SqlSession session = this.getSqlSession();
        session.update("trax.updateCurrentTimeline", line.getId());
        return line;
    }

    public void concludeCurrentTimeline()
    {
        I_Timeline current = getCurrentTimeline();

        if (current != null) {
            long time = System.currentTimeMillis();
            Timestamp start = new Timestamp(time);
            I_Timeslice newSlice = createTimeslice(current.getId(), start, I_Type.OFFLINE_TYPE_ID);
            // now clear current
            SqlSession session = this.getSqlSession();
            session.update("trax.updateCurrentTimeline", null);
        }
    }

    public void clearCurrentTimeline()
    {
        SqlSession session = this.getSqlSession();
        session.update("trax.updateCurrentTimeline", null);
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#createTimeslice(int, java.sql.Timestamp)
     */
    public I_Timeslice createTimeslice(int lineId, Timestamp start, int type)
    {
        SqlSession session = this.getSqlSession();
        DbTimeslice slice = new DbTimeslice();
        slice.setTypeId(type);
        slice.setStart(start);
        slice.setTimelineId(lineId);
        session.insert("trax.createTimeslice", slice);
        return slice;
    }


    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TimelineDao#saveTimeline(nbdp.trax.data.DbTimeline)
     */
    public void saveTimeline(I_Timeline t)
    {
        SqlSession session = this.getSqlSession();
        if (t.getId() == -1) // possibly unpersisted line
        {
            // see if a timeline exists with the same start timestamp
            DbTimeline dbTl = (DbTimeline) session.selectOne("trax.getTimelineByStart",
                    t);
            if (dbTl != null)
            {
                SimpleDateFormat formatter = new SimpleDateFormat(
                        "yyyy.MM.dd_hh.mm.ss_SSS.zzz");

                System.out.println("Found db timeline "
                        + dbTl.toString(formatter) + ". Updating...");
                t.setId(dbTl.getId());
            } else
            {
                t.setId(getNextLineId(session));
            }
        }
        // now clear out the old before inserting anew if applicable
        session.delete("trax.deleteTimeline", t);
        session.delete("trax.deleteSlicesForTimeline", t);

        // now insert into DB
        session.insert("trax.createTimeline", t);
        List slices = t.getSlices();
        if (slices != null && slices.size() > 0)
        {
            for (Iterator itr = slices.iterator(); itr.hasNext();)
            {
                DbTimeslice s = (DbTimeslice) itr.next();
                s.setTimelineId(t.getId());
                if (s != null)
                    session.insert("trax.createTimeslice", s);
            }
        }
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TimelineDao#getSlices(nbdp.trax.data.DbTimeline)
     */
    public void getSlices(I_Timeline t)
    {
        SqlSession session = this.getSqlSession();
        List slices = session.selectList("trax.getSlicesForTimeline", t);
        t.setSlices(slices);
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#getTimelinesInPeriod(java.util.Date, java.util.Date)
     */
    public List getTimelinesInPeriod(Period period)
    {
        SqlSession session = this.getSqlSession();
        return session.selectList("trax.getTimelinesInPeriod", period);
    }

    /** Return the set of supported types.
     * @see nbdp.trax.data.I_TraxDao#getTypes()
     */
    public I_Type[] getTypes()
    {
        SqlSession session = this.getSqlSession();
        List types = session.selectList("trax.getTypes", null);
        return (I_Type[]) types.toArray(new DbType[] {});
    }

    public I_Type getTypeById(int typeId)
    {
        SqlSession session = this.getSqlSession();
        return (I_Type) session.selectOne("trax.getTypeById", new Integer(typeId));
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#setTypes(nbdp.trax.data.Type[])
     */
    public void setTypes(I_Type[] types)
    {
        SqlSession session = this.getSqlSession();
        for (int i=0; i<types.length; i++)
            session.insert("trax.createType", types[i]);
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#getSlicesInPeriod(nbdp.trax.data.Period)
     */
    public List getSlicesInPeriod(Period p)
    {
        SqlSession session = this.getSqlSession();
        return session.selectList("trax.slicesInPeriod", p);
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#getTaskName(int)
     */
    public String getTaskName(int taskId)
    {
        SqlSession session = this.getSqlSession();
        return (String) session.selectOne("trax.taskNameById", new Integer(taskId));
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#getTask(int)
     */
    public I_Task getTask(int taskId)
    {
        SqlSession session = this.getSqlSession();
        return (I_Task) session.selectOne("trax.taskById", new Integer(taskId));
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#getSubTasks(int)
     */
    public List getSubTasks(int taskId)
    {
        SqlSession session = this.getSqlSession();
        return session.selectList("trax.subtasksByParentId", new Integer(taskId));
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#createTask(int, int, int, java.lang.String, java.lang.String)
     */
    public I_Task createTask(int parentId, int typeId, String name, String description)
    {
        DbTask t = getTaskObj(parentId, typeId, name, description);
        SqlSession session = this.getSqlSession();
        Integer wrap = (Integer) session.selectOne(
                "trax.nextTaskId", null);
        t.setId(wrap.intValue());
        session.update("trax.updateNextTaskId", new Integer(t.getId() + 1));
        session.insert("trax.createTask", t);
        return t;
    }

    private DbTask getTaskObj(int parentId, int typeId, String name, String description)
    {
        DbTask t = new DbTask();
        t.setDescription(description);
        t.setParentId(parentId);
        t.setIsCompleted(false);
        t.setName(name);
        t.setTypeId(typeId);
        return t;
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#updateTask(nbdp.trax.data.I_Task)
     */
    public void updateTask(I_Task task)
    {
        SqlSession session = this.getSqlSession();
        session.update("trax.updateTask", task);
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#migrateLegacyTask(int, int, int, java.lang.String, java.lang.String)
     */
    public I_Task migrateLegacyTask(int id, int parentId, int typeId,
            String name, String description, boolean isCompleted)
    {
        DbTask t = getTaskObj(parentId, typeId, name, description);
        t.setId(id);
        t.setIsCompleted(isCompleted);
        SqlSession session = this.getSqlSession();
        session.insert("trax.createTask", t);
        return t;
    }

    /**
     * Update the next task id, the id that will be used for a new task.
     * @see nbdp.trax.data.I_TraxDao#migrateLegacyNextTaskId(int)
     */
    public void migrateLegacyNextTaskId(int nextTaskId)
    {
        SqlSession session = this.getSqlSession();
        session.insert("trax.updateNextTaskId", new Integer(nextTaskId));
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#createTimeslice(int, java.sql.Timestamp)
     */
    public I_Timeslice createTimeslice(int lineId, Timestamp start)
    {
        return createTimeslice(lineId, start, I_Type.MISC_TYPE_ID);
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#deleteTimeline(int)
     */
    public void deleteTimeline(I_Timeline t)
    {
        SqlSession session = this.getSqlSession();
        session.delete("trax.deleteTimeline", t);
        session.delete("trax.deleteSlicesForTimeline", t);
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#deleteTask(nbdp.trax.data.I_Task)
     */
    public void deleteTask(I_Task task)
    {
        SqlSession session = this.getSqlSession();
        session.delete("trax.deleteTask", task);
        TaskIds replace = new TaskIds();
        replace.setNewTaskId(I_Task.NO_TASK_ID);
        replace.setOldTaskId(task.getId());
        session.update("trax.removeTaskReferences", replace);
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#isTaskReferenced(nbdp.trax.data.I_Task)
     */
    public boolean isTaskReferenced(I_Task task)
    {
        SqlSession session = this.getSqlSession();
        Integer count = (Integer) session.selectOne(
                "trax.getTaskReferenceCount", task);
        if (count != null)
        {
            return count.intValue()>0;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see nbdp.trax.data.I_TraxDao#moveTask(int, int)
     */
    public void moveTask(int task_id, int parent_id)
    {
        SqlSession session = this.getSqlSession();
        TaskIds ids = new TaskIds();
        ids.setTaskId(task_id);
        ids.setNewTaskId(parent_id);
        session.update("trax.reparentTask", ids);
    }

    public void deleteViewAspect(String viewId, String aspect)
    {
        SqlSession session = this.getSqlSession();
        ViewAspect a = new ViewAspect();
        a.setViewId(viewId);
        a.setAspect(aspect);
        session.delete("trax.removeViewAspect", a);
    }

    public List getViewAspects(String viewId)
    {
        SqlSession session = this.getSqlSession();
        return session.selectList("trax.viewAspectsByViewId", viewId);
    }

    public void setViewAspect(String viewId, String aspect)
    {
        SqlSession session = this.getSqlSession();
        ViewAspect a = new ViewAspect();
        a.setViewId(viewId);
        a.setAspect(aspect);
        session.insert("trax.setViewAspect", a);
    }
}
