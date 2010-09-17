/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
 * 
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2;

import java.util.Date;

/**
 * Log.java: May 17, 2010 9:29:29 PM
 *
 * This has to die. Use Log4j or another logging framework
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public final class Log {
    
    /**
     * INFO
     */
    public static final int SEVERITY_INFO = 1;
    /**
     * WARNING
     */
    public static final int SEVERITY_WARN = 2;
    /**
     * CRITICAL
     */
    public static final int SEVERITY_CRIT = 3;
    /**
     * EMERGENCY
     */
    public static final int SEVERITY_EMERG = 4;

    private Log() { }
    
    /**
     * Log a message with severity INFO
     * @param logMessage The message to log
     */
    public static void info(String logMessage) {
        Log.toStdOut(logMessage, Log.SEVERITY_INFO);
    }

    /**
     * Log a message with severity WARNING
     * @param logMessage The message to log
     */
    public static void warn(String logMessage) {
        Log.toStdOut(logMessage, SEVERITY_WARN);
    }

    /**
     * Log a message with severity CRITICAL
     * @param logMessage The message to log
     */
    public static void crit(String logMessage) {
        Log.toStdOut(logMessage, Log.SEVERITY_CRIT);
    }

    /**
     * Log a message with severity EMERGENCY
     * @param logMessage The message to log
     */
    public static void emerg(String logMessage) {
        Log.toStdOut(logMessage, Log.SEVERITY_EMERG);
    }

    /**
     * Log a message to STDOUT with given severity.
     * @param logMessage The message to log
     * @param Severity The severity of this message
     */
    public static void toStdOut(String logMessage, int severity) {
        if (Main.debugMode) {
            String finalMessage = new Date().toString() + " - " + Log.severityToString(severity) + " - "+ logMessage;
            System.out.println(finalMessage);
        }
    }
    
    /**
     * Get the human readable name of a severity
     * @param Severity The severity
     * @return The name of the severity
     */
    public static String severityToString(int severity) {
        String severityString = "UNSPECIFIED";
        switch(severity) {
            case Log.SEVERITY_INFO:
                severityString = "INFO";
                break;
            case Log.SEVERITY_WARN:
                severityString = "WARNING";
                break;
            case Log.SEVERITY_CRIT:
                severityString = "CRITICAL";
                break;
            case Log.SEVERITY_EMERG:
                severityString = "EMERGENCY";
                break;
        }
        return severityString;
    }

}
