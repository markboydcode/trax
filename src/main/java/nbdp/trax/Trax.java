package nbdp.trax;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

/*
 * Created on Dec 22, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author mboyd
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Trax {

	private static PrintWriter cLog = null;
    private static String cCfgLoc = null;
    private static Properties cCfg = null;
    private static final String TRAX_PATH = "traxClasses";
    private static final String XALAN_PATH = "xalan.jar";
    private static final String XERCES_PATH = "xerces.jar";
    private static final String APIS_PATH = "xml-apis.jar";
    private static final String DB_PATH = "database";

	public static void main(String[] args) throws Exception {
        cLog = new PrintWriter(new FileWriter("trax.log"));
        try
        {
            processArguments(args);
            System.out.println("1");
            Runtime rt = Runtime.getRuntime();
            /* 
            Process proc =
                rt.exec(
                    "java -Xdebug "
                        + "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=11112 "
                        + "-cp D:/jars/xalan-j_2_4_1/bin/xercesImpl.jar;"
                        + "D:/jars/xalan-j_2_4_1/bin/xml-apis.jar;"
                        + "D:/jars/xalan-j_2_4_1/bin/xalan.jar;"
                        + "d:/mboyd/eclipseWS/workspace/Trax "
                        + "TimelineView "
                        + "-db d:/mboyd/time/db");
            */
            String command = "java -Xdebug "
                    + "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=11112 "
                    + "-cp \"" + cCfg.getProperty(XERCES_PATH) + ";"
                    + cCfg.getProperty(APIS_PATH) + ";"
                    + cCfg.getProperty(XALAN_PATH) + ";"
                    + cCfg.getProperty(TRAX_PATH) + "\" " + "TimelineView "
                    + "-db \"" + cCfg.getProperty(DB_PATH) + "\"";
            cLog.println("Launching process: " + command);
            cLog.flush(); 
            Process proc = rt.exec(command);
                        
            Spooler ErrSpooler = new Spooler(proc.getErrorStream(), "ERROR");
            Thread errThread = new Thread(ErrSpooler);
            errThread.start();

            Spooler OutputSpooler = new Spooler(proc.getInputStream(), "OUTPUT");
            Thread outThread = new Thread(OutputSpooler);
            outThread.start();

            proc.waitFor();
            errThread.interrupt();
            outThread.interrupt();

            writeLog(
                (proc.exitValue() == 0
                    ? "Application exited normally."
                    : "Application exited with code " + proc.exitValue()),
                "LAUNCHER");
        }
        catch(Throwable t)
        {
            do
            {
                t.printStackTrace(cLog);
                t = t.getCause();
                if (t != null)
                    cLog.println("---- cause ----");
            } while (t != null);
            
            cLog.flush();
        }
	}
    private static void processArguments(String[] args)
    throws Exception
    {
        for(int i=0; i<args.length; i++)
        {
            String value = getOptionValue(args, i, "-cfg", "d:/x/y/z.props");
            if ( value != null )
                cCfgLoc = value;
        }
        try
        {
            if (cCfgLoc == null)
                throw new IllegalArgumentException("Missing command line " +
                        "option -cfg.");
            validateConfiguration();
        }
        catch(Exception e)
        {
            throw new Exception("Command line " +
                    "option '-cfg' is required and must " +
                    "be a valid path to a file conforming to that used by " +
                    "java.util.Properties that must include the following " +
                    "property declarations:\n\n" +
                    TRAX_PATH + "=<fullPathToCompiledTraxClasses>\n" +
                    XALAN_PATH + "=<full path to xalan jar file>\n" +
                    XERCES_PATH + "=<full path to xerces jar file>\n" +
                    APIS_PATH + "=<full path to xml-apis jar file>\n" +
                    DB_PATH + "=<full path to trax data directory>\n\n" +
                    "All but the data directory must be valid paths to " +
                    "existing files. If not found or empty the data directory" +
                    " will be created and initialized.\n\n", e);
        }
    }
    
    private static void validateConfiguration() throws Exception
    {
        File fPath = new File(cCfgLoc);
        if (fPath.exists() == false)
            throw new FileNotFoundException("'" + fPath.getAbsolutePath()
                    + "' declared for '-cfg'.");
        FileInputStream cfg = new FileInputStream(cCfgLoc);
        cCfg = new Properties();
        cCfg.load(cfg);
        
        verifyPath(TRAX_PATH);
        verifyPath(XALAN_PATH);
        verifyPath(XERCES_PATH);
        verifyPath(APIS_PATH);
        
        if (cCfg.get(DB_PATH) == null)
            throw new IllegalArgumentException("Missing path " + TRAX_PATH);
    }
    
    private static void verifyPath(String path)
    throws Exception
    {
        String pathVal = cCfg.getProperty(path);
        if ( pathVal == null)
            throw new IllegalArgumentException("Missing path " + TRAX_PATH);

        File fPath = new File(pathVal);
        if (fPath.exists() == false)
            throw new FileNotFoundException("'" + fPath.getAbsolutePath()
                    + "' declared for '" + TRAX_PATH + "'.");
    }
    
    private static String getOptionValue(String[] args,
            int optionIdx,
            String optionIndicator,
            String sampleValue)
    {
        if(args[optionIdx].equals(optionIndicator))
        {
            if ( optionIdx+1 >= args.length)
            {
                throw new IllegalArgumentException("Command line " +
                        "option '" + optionIndicator + "' must have form '" +
                        optionIndicator + " \"" + sampleValue + "\".");
            }
            return args[optionIdx+1];
        }
        return null;
    }

	private static class Spooler implements Runnable {
		private String mMoniker = null;
		private InputStream mStream = null;
		private Exception mException = null;

		Spooler(InputStream stream, String moniker) {
			this.mStream = stream;
			this.mMoniker = moniker;
		}

		/* Spool any input from a input stream to the writeLog method of the 
		 * containing class with the assigned moniker indicating the type of the
		 * infomation being spooled.
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			byte[] bytes = new byte[4096];
			boolean active = true;

			while (active) {
				int bytesRead = -1;
				try {
					bytesRead = mStream.read(bytes, 0, bytes.length);
				} catch (Exception e) {
					active = false;
					mException = e;
				}
				if (active && bytesRead == -1)
					active = false;
				else if (active && bytesRead > 0)
					writeLog(mMoniker, new String(bytes, 0, bytesRead));
			}
			if (mException == null)
				writeLog(mMoniker, "Spooler Exiting due end of stream.");
			else
				writeLog(
					mMoniker,
					"Spooler Exiting due to exception: " + mException);
		}
	}

	static synchronized void writeLog(String type, String entry) {
		cLog.println(type + ": " + entry);
	}
}
