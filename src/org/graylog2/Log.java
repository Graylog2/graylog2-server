/**
 * Copyright 2010 Lennart Koopmann <lennart@scopeport.org>
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


/**
 * Log.java: Lennart Koopmann <lennart@scopeport.org> | May 17, 2010 9:29:29 PM
 */

package org.graylog2;

import java.util.Date;

public class Log {
    
    public static final int SEVERITY_INFO = 1;
    public static final int SEVERITY_WARN = 2;
    public static final int SEVERITY_CRIT = 3;
    public static final int SEVERITY_EMERG = 4;
    
    public static void info(String logMessage) {
        Log.toStdOut(logMessage, Log.SEVERITY_INFO);
    }

    public static void warn(String logMessage) {
        Log.toStdOut(logMessage, SEVERITY_WARN);
        Log.toMongo(logMessage, SEVERITY_WARN);
    }

    public static void crit(String logMessage) {
        Log.toStdOut(logMessage, Log.SEVERITY_CRIT);
        Log.toMongo(logMessage, SEVERITY_CRIT);
    }

    public static void emerg(String logMessage) {
        Log.toStdOut(logMessage, Log.SEVERITY_EMERG);
        Log.toMongo(logMessage, SEVERITY_EMERG);
    }

    public static void toStdOut(String logMessage, int Severity) {
        if (Main.debugMode) {
            String finalMessage = new Date().toString() + " - " + Log.severityToString(Severity) + " - "+ logMessage;
            System.out.println(finalMessage);
        }
    }
    
    public static String severityToString(int Severity) {
        String severityString = "UNSPECIFIED";
        switch(Severity) {
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

    private static void toMongo(String logMessage, int severity) {
        // TODO: Log to Mongo
        //MongoMapper m = new MongoMapper();
        //m.log(logMessage, severity);
    }

}
