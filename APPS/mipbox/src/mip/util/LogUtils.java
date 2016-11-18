/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 *
 * @author ju
 */
public class LogUtils {

    public static final Logger LOGGER = Logger.getGlobal();

    private static Level logLevel = Level.ALL;

    static {
        // Remove all the default handlers (usually just one console handler)
        Logger rootLogger = Logger.getLogger("");
        Handler[] rootHandlers = rootLogger.getHandlers();
        for (Handler handler : rootHandlers) {
            rootLogger.removeHandler(handler);
        }

        // Add our own handler
        StreamHandler sh = new StreamHandler(System.out, new SingleLineFormatter());
        sh.setLevel(logLevel);
        LOGGER.addHandler(sh);
        LOGGER.setLevel(logLevel);
        Locale.setDefault(Locale.UK);
    }

    public static void main(String[] args) {
        LogUtils.setLogLevel(Level.ALL);
        LOGGER.severe("severe");
        LOGGER.warning("warning");
        LOGGER.config("config");
        LOGGER.fine("fine");
        LOGGER.finer("finer");
        LOGGER.finest("finest");
    }

    public static void setLogLevel(Level newLogLevel) {
        logLevel = newLogLevel;
        for (Handler handler : LOGGER.getHandlers()) {
            handler.setLevel(newLogLevel);
        }
        LogUtils.LOGGER.setLevel(newLogLevel);
    }
}

class SingleLineFormatter extends Formatter {

    Date dat = new Date();
    private final static String format = "{0,date} {0,time}";
    private MessageFormat formatter;
    private Object args[] = new Object[1];

    // Line separator string.  This is the value of the line.separator
    // property at the moment that the SimpleFormatter was created.
    //private String lineSeparator = (String) java.security.AccessController.doPrivileged(
    //        new sun.security.action.GetPropertyAction("line.separator"));
    private String lineSeparator = "\n";

    /**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {

        StringBuilder sb = new StringBuilder();

        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;

        // Date and time 
        StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(format);
        }
        formatter.format(args, text, null);
        sb.append(text);
        sb.append(" ");

        // Class name 
        if (record.getSourceClassName() != null) {
            sb.append(record.getSourceClassName());
        } else {
            sb.append(record.getLoggerName());
        }

        // Method name 
        if (record.getSourceMethodName() != null) {
            sb.append(" ");
            sb.append(record.getSourceMethodName());
        }
        sb.append(" - "); // lineSeparator

        String message = formatMessage(record);

        // Level
        sb.append(String.format("%-7s\t: ", record.getLevel().getLocalizedName()));

        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }
        return sb.toString();
    }
}
