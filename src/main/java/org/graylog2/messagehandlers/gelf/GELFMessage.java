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
 * GELFMessage.java: Lennart Koopmann <lennart@scopeport.org> | Jul 20, 2010 6:57:28 PM
 */
package org.graylog2.messagehandlers.gelf;

public class GELFMessage {

    // This is more a dummy class. Maybe it will be extended later.
    public String shortMessage = null;
    public String fullMessage = null;
    public int    level = 0;
    public int    type = 0;
    public String host = null;
    public String file = null;
    public int    line = 0;


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
