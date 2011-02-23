/**
 * Copyright 2010, 2011 Lennart Koopmann <lennart@socketfeed.com>
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

import java.net.SocketAddress;
import org.graylog2.Log;
import org.graylog2.Main;
import org.graylog2.Tools;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.messagehandlers.gelf.SimpleGELFClientHandler;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.SyslogServerSessionlessEventHandlerIF;

/**
 * SyslogEventHandler.java: May 17, 2010 8:58:18 PM
 *
 * Handles incoming Syslog messages
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogEventHandler implements SyslogServerSessionlessEventHandlerIF {
    
    /**
     * Handle an incoming syslog message: Output if in debug mode, store in MongoDB, ReceiveHooks
     *
     * @param syslogServer The syslog server
     * @param event The event to handle
     */
    public void event(SyslogServerIF syslogServer, SocketAddress socketAddress, SyslogServerEventIF event) {

        GELFMessage gelf = new GELFMessage();

        // Print out debug information.
        if (Main.debugMode) {
            if (event instanceof GraylogSyslogServerEvent) {
                GraylogSyslogServerEvent glEvent = (GraylogSyslogServerEvent) event;
                Log.info("Received syslog message (via AMQP): " + event.getMessage());
                Log.info("AMQP queue: " + glEvent.getAmqpReceiverQueue());

                gelf.addAdditionalData("_amqp_queue", glEvent.getAmqpReceiverQueue());
            } else {
                Log.info("Received syslog message: " + event.getMessage());
            }
            Log.info("Host: " + event.getHost());
            Log.info("Facility: " + event.getFacility() + " (" + Tools.syslogFacilityToReadable(event.getFacility()) + ")");
            Log.info("Level: " + event.getLevel() + " (" + Tools.syslogLevelToReadable(event.getLevel()) + ")");
            Log.info("Raw: " + new String(event.getRaw()));
            Log.info("=======");
        }
        
        // Convert SyslogServerEventIF to GELFMessage and pass to SimpleGELFClientHandler
        gelf.setVersion("1.0");
        gelf.setShortMessage(event.getMessage());
        gelf.setHost(event.getHost());
        gelf.setFacility(Tools.syslogFacilityToReadable(event.getFacility()));
        gelf.setLevel(event.getLevel());
        gelf.setFullMessage(new String(event.getRaw()));
        
        try {
            SimpleGELFClientHandler gelfHandler = new SimpleGELFClientHandler(gelf);
            gelfHandler.handle();
        } catch ( Exception e ) {
                // I don't care
        }
    }

    public void exception(SyslogServerIF syslogServer, SocketAddress socketAddress, Exception exception) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void initialize(SyslogServerIF syslogServer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void destroy(SyslogServerIF syslogServer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
