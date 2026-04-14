package nbdp.trax;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class Launch {

	private static final Log cLog = LogFactory.getLog(Launch.class);
    private static String cPropsLoctn = null;
    private static Properties cCfg = null;
    private static final String JVMARG_JDWP = "jvmJdwpArg";
    private static final String LOG4J_CONFIG_DIR = "log4j.cfg.dir";
    private static final String CFG_PATH = "configPath";
    private static final String CLASS = "classToExec";
    
    private static final String CMND_LINE_PROP_LOCTN_NAME = "-props";
    private static final String CMND_LINE_CLASSPATH_SEPARATOR = ":";
	private static final String PATH_ITEM_PREFIX = "pathItem.";

	public static void main(String[] args) throws Exception {
        try
        {
            if (cLog.isDebugEnabled())
            {
                cLog.debug("\n\n########################## Launching TRAX ##########################\n");
            }
            processArguments(args);
            Runtime rt = Runtime.getRuntime();
            /* 
            Process proc =
                rt.exec(
                    "java -Xdebug "
                        + "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=11112 "
                        + "-cp D:/jars/xalan-j_2_4_1/bin/xercesImpl.jar;"
                        + "D:/jars/xalan-j_2_4_1/bin/xml-apis.jar;"
                        + "D:/jars/xalan-j_2_4_1/bin/xalan.jar;"
                        + "d:/mboyd/eclipseWS/workspace/Trax "
                        + "TimelineView "
                        + "-db d:/mboyd/time/db");
            */
            String command = "java "
                    + (cCfg.getProperty(JVMARG_JDWP) != null ?
                            "-Xdebug " + cCfg.getProperty(JVMARG_JDWP) : "")
                    + " -cp \"";
            
            // tack on classpath pathItem entries from file
            Enumeration<Object> keys = cCfg.keys();
            while (keys.hasMoreElements()) {
            	String key = (String) keys.nextElement();
            	if (key.startsWith(PATH_ITEM_PREFIX)) {
                	command += addCpItem(cCfg.getProperty(key));
            	}
            }
            command +=  "\" " 
                    + cCfg.getProperty(CLASS) + " "
                    + "-cfg " + cCfg.getProperty(CFG_PATH);
            if (cLog.isDebugEnabled())
            {
                cLog.debug("Launching process:\n\n" + command + "\n");
            }
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
            cLog.fatal("A problem occurred while launching TRAX.", t);
        }
	}
    
	/**
	 * Conditionally returns the jar referred to plus a terminating semi-colon
	 * or returns an empty string if that jar path is not defined.
	 * 
	 * @param pathItem
	 * @return
	 */
    private static String addCpItem(String pathItem) {

    	if (pathItem != null && ! pathItem.equals("")) {
    		File f = new File(pathItem);
    		if (f.exists()) {
    	    	cLog.debug("FOUND:   classpath item '" + pathItem + "' is at " + f.getAbsolutePath() + ". Including in classpath.");
        		return pathItem + CMND_LINE_CLASSPATH_SEPARATOR;
    		}
    		else {
    	    	cLog.debug("MISSING: classpath item '" + pathItem + "'.");
    			return "";
    		}
    	}
		return "";
	}

	private static void processArguments(String[] args)
    throws Exception
    {
        for(int i=0; i<args.length; i++)
        {
            String value = getOptionValue(args, i, CMND_LINE_PROP_LOCTN_NAME,
					"d:/x/y/z.props");
            if ( value != null )
                cPropsLoctn = value;
        }
        try
        {
            if (cPropsLoctn == null)
                throw new IllegalArgumentException("Missing command line " +
                        "option " + CMND_LINE_PROP_LOCTN_NAME + ".");
            validateConfiguration();
        }
        catch(Exception e)
        {
            throw new Exception("Command line " +
                    "option '" + CMND_LINE_PROP_LOCTN_NAME + "' is required and must " +
                    "be a valid path to a file conforming to that used by " +
                    "java.util.Properties that must include the following " +
                    "property declarations:\n\n" 
                    + "pathItem.##=<full path to jar or directory> with ## being unique for each such entry\n" +
                    LOG4J_CONFIG_DIR + "=<full path to directory containing the log4j config file>\n\n" +
                    CFG_PATH + "=<full path to trax spring beans config file>\n\n" +
                    CLASS + "=<the java class to execute>\n\n" +
                    "All must be valid paths to existing files.\n\n", e);
        }
    }
    
    private static void validateConfiguration() throws Exception
    {
        File fPath = new File(cPropsLoctn);
        if (fPath.exists() == false)
            throw new FileNotFoundException("'" + fPath.getAbsolutePath()
                    + "' declared for '" + CMND_LINE_PROP_LOCTN_NAME + "'.");
        FileInputStream cfg = new FileInputStream(cPropsLoctn);
        cCfg = new Properties();
        cCfg.load(cfg);
        
        if (cCfg.get(CLASS) == null)
            throw new IllegalArgumentException("Missing " + CLASS);
    }
    
    private static String getOptionValue(String[] args,
            int optionIdx,
            String optionIndicator,
            String sampleValue)
    {
        if(args[optionIdx].trim().equalsIgnoreCase(optionIndicator))
        {
            if ( optionIdx+1 > args.length)
            {
            	String arguments = "";
            	for (int i=0; i<args.length; i++) {
            		arguments += args[i] + " ";
            	}
                throw new IllegalArgumentException("Command line " +
                        "option '" + optionIndicator + "' must have form '" +
                        optionIndicator + " \"" + sampleValue + "\". Only found " + args.length + " arguments: " + arguments);
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
					cLog.debug("incurred exception: ", e);
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
        if (cLog.isDebugEnabled())
        {
            cLog.debug(type + ": " + entry);
        }
	}
}
