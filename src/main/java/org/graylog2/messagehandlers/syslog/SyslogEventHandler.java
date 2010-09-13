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

package org.graylog2.messagehandlers.syslog;

import org.graylog2.Log;
import org.graylog2.Main;
import org.graylog2.Tools;
import org.graylog2.database.MongoBridge;
import org.graylog2.messagehandlers.common.MessageCounterHook;
import org.graylog2.messagehandlers.common.ReceiveHookManager;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;

/**
 * SyslogEventHandler.java: May 17, 2010 8:58:18 PM
 *
 * Handles incoming Syslog messages
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogEventHandler implements SyslogServerEventHandlerIF {
    
    /**
     * Handle an incoming syslog message: Output if in debug mode, store in MongoDB, ReceiveHooks
     *
     * @param syslogServer The syslog server
     * @param event The event to handle
     */
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

            // This is doing the upcounting for RRD.
            ReceiveHookManager.postProcess(new MessageCounterHook());
        } catch (Exception e) {
            Log.crit("Could not insert syslog event into database: " + e.toString());
        }

    }

}
