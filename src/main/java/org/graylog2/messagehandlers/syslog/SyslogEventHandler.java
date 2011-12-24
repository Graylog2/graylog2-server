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

import org.apache.log4j.Logger;
import org.graylog2.Tools;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.messagehandlers.gelf.SimpleGELFClientHandler;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.SyslogServerSessionlessEventHandlerIF;

import java.net.SocketAddress;
import java.net.UnknownHostException;
import org.graylog2.Main;

/**
 * SyslogEventHandler.java: May 17, 2010 8:58:18 PM
 * <p/>
 * Handles incoming Syslog messages
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogEventHandler implements SyslogServerSessionlessEventHandlerIF {

    private static final Logger LOG = Logger.getLogger(SyslogEventHandler.class);

    /**
     * Handle an incoming syslog message: Output if in debug mode, store in MongoDB, ReceiveHooks
     *
     * @param syslogServer The syslog server
     * @param event        The event to handle
     */
    @Override
    public void event(SyslogServerIF syslogServer, SocketAddress socketAddress, SyslogServerEventIF event) {

        GELFMessage gelf = new GELFMessage();

        // Print out debug information.
        if (event instanceof GraylogSyslogServerEvent) {
            GraylogSyslogServerEvent glEvent = (GraylogSyslogServerEvent) event;
            LOG.debug("Received syslog message (via AMQP): " + event.getMessage());
            LOG.debug("AMQP queue: " + glEvent.getAmqpReceiverQueue());

            gelf.addAdditionalData("_amqp_queue", glEvent.getAmqpReceiverQueue());
        } else {
            LOG.debug("Received syslog message: " + event.getMessage());
        }
        LOG.debug("Host: " + event.getHost());
        LOG.debug("Facility: " + event.getFacility() + " (" + Tools.syslogFacilityToReadable(event.getFacility()) + ")");
        LOG.debug("Level: " + event.getLevel() + " (" + Tools.syslogLevelToReadable(event.getLevel()) + ")");
        LOG.debug("Raw: " + new String(event.getRaw()));
        LOG.debug("Host stripped from message? " + event.isHostStrippedFromMessage());

        // Manually check for provided date because it's necessary to parse the GELF message. Second check for completness later.
        if (event.getDate() == null) {
            LOG.info("Syslog message is missing date or could not be parsed. Not further handling. Message was: " + new String(event.getRaw()));
            return;
        }

        // Possibly overwrite host with RNDS if configured.
        String host = event.getHost();
        try {
            if (Main.configuration.getForceSyslogRdns()) {
                host = Tools.rdnsLookup(socketAddress);
            }
        } catch (UnknownHostException e) {
            LOG.warn("Reverse DNS lookup failed. Falling back to parsed hostname.", e);
        }

        try {
            gelf.setCreatedAt(Tools.getUTCTimestampWithMilliseconds(event.getDate().getTime()));
            gelf.setConvertedFromSyslog(true);
            gelf.setVersion("0");
            gelf.setShortMessage(event.getMessage());
            gelf.setFullMessage(new String(event.getRaw()));
            gelf.setHost(host);
            gelf.setFacility(Tools.syslogFacilityToReadable(event.getFacility()));
            gelf.setLevel(event.getLevel());
            gelf.setRaw(event.getRaw());
        } catch (Exception e) {
            LOG.info("Could not parse syslog message to GELF: " + e.toString(), e);
            return;
        }
        
        if (gelf.allRequiredFieldsSet()) {
            try {
                SimpleGELFClientHandler gelfHandler = new SimpleGELFClientHandler(gelf);
                gelfHandler.handle();
            } catch (Exception e) {
                LOG.debug("Couldn't process message with GELF handler", e);
            }
        } else {
            LOG.info("Broken or incomplete syslog message. Not further handling. Message was: " + event.getRaw());
        }
    }

    @Override
    public void exception(SyslogServerIF syslogServer, SocketAddress socketAddress, Exception exception) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initialize(SyslogServerIF syslogServer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void destroy(SyslogServerIF syslogServer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
