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
 * SyslogEventHandler.java: Lennart Koopmann <lennart@scopeport.org> | May 17, 2010 8:58:18 PM
 */

package org.graylog2.messagehandlers.syslog;

import org.graylog2.Log;
import org.graylog2.Main;
import org.graylog2.periodical.SystemStatistics;
import org.graylog2.Tools;
import org.graylog2.database.MongoBridge;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;

public class SyslogEventHandler implements SyslogServerEventHandlerIF {
    
    @Override public void event(SyslogServerIF syslogServer, SyslogServerEventIF event) {
        if (Main.debugMode) {
            Log.info("Received message: " + event.getMessage());
            Log.info("Host: " + event.getHost());
            Log.info("Facility: " + event.getFacility() + " (" + Tools.syslogFacilityToReadable(event.getFacility()) + ")");
            Log.info("Level: " + event.getLevel() + " (" + Tools.syslogLevelToReadable(event.getLevel()) + ")");
            Log.info("=======");
        }

         // Insert into database.
        try {
            // Connect to database.
            MongoBridge m = new MongoBridge();


            m.insert(event);
        } catch (Exception e) {
            Log.crit("Could not insert syslog event into database: " + e.toString());
        }

        // Count up for statistics.
        SystemStatistics.getInstance().countUpHandledSyslogEvents();
    }

}
