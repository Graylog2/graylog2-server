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
 * Tools.java: Lennart Koopmann <lennart@scopeport.org> | May 17, 2010 9:46:31 PM
 */

package org.graylog2;

public class Tools {

    /*
     * Converts integer syslog loglevel to human readable string
     *
     * @param level The level to convert
     * @return The human readable level
     */
    public static String syslogLevelToReadable(int level) {
        switch (level) {
            case 0:
                return "Emergency";
            case 1:
                return "Alert";
            case 2:
                return"Critical";
            case 3:
                return "Error";
            case 4:
                return "Warning";
            case 5:
                return "Notice";
            case 6:
                return "Informational";
            case 7:
                return "Debug";
        }

        return "Invalid";
    }

    /*
     * Converts integer syslog facility to human readable string
     *
     * @param facility The facility to convert
     * @return The human readable facility
     */
    public static String syslogFacilityToReadable(int facility) {
        switch (facility) {
            case 0:  return "kernel";
            case 1:  return "user-level";
            case 2:  return "mail";
            case 3:  return "system daemon";
            case 4:  return "security/authorization";
            case 5:  return "syslogd";
            case 6:  return "line printer";
            case 7:  return "network news";
            case 8:  return "UUCP";
            case 9:  return "clock";
            case 10: return "security/authorization";
            case 11: return "FTP";
            case 12: return "NTP";
            case 13: return "log audit";
            case 14: return "log alert";
            case 15: return "clock";

            // TODO: Make user definable?
            case 16: return "local0";
            case 17: return "local1";
            case 18: return "local2";
            case 19: return "local3";
            case 20: return "local4";
            case 21: return "local5";
            case 22: return "local6";
            case 23: return "local7";
        }

        return "Unknown";
    }

    /*
     * 
     * @return Descriptive string of JRE and OS
     */
    public static String getSystemInformation() {
        String ret = System.getProperty("java.vendor");
        ret += " " + System.getProperty("java.version");
        ret += " on " + System.getProperty("os.name");
        ret += " " + System.getProperty("os.version");
        return ret;
    }

}
