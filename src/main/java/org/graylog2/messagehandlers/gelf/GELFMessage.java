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

package org.graylog2.messagehandlers.gelf;

/**
 * GELFMessage.java: Jul 20, 2010 6:57:28 PM
 *
 * A GELF message
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFMessage {

    /**
     * Short message
     */
    public String shortMessage = null;

    /**
     * Full message. i.e. for Stacktracke and environment variables.
     */
    public String fullMessage = null;

    /**
     * The severity level. Follows BSD Syslog RFC
     */
    public int    level = 0;

    /**
     * Type. Currently not used.
     */
    public int    type = 0;

    /**
     * Hostname
     */
    public String host = null;

    /**
     * File
     */
    public String file = null;

    /**
     * Line of file
     */
    public int    line = 0;


    /**
     * @return Human readable, descriptive and formatted string of this GELF message.
     */
    @Override public String toString() {
        String str = "shortMessage: " + shortMessage + " | ";
        str += "fullMessage: " + fullMessage + " | ";
        str += "level: " + level + " | ";
        str += "type: " + type + " | ";
        str += "host: " + host + " | ";
        str += "file: " + file + " | ";
        str += "line: " + line;

        // Replace all newlines and tabs.
        String ret = str.replaceAll("\\n", "").replaceAll("\\t", "");

        // Cut to 100 chars if the message is too long.
        if (ret.length() > 150) {
            ret = ret.substring(0, 150);
            ret += " (...)";
        }

        return ret;
    }
}
