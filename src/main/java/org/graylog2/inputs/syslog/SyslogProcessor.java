/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.inputs.syslog;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.logmessage.LogMessage;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;
import org.productivity.java.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;

/**
 * SyslogProcessor.java: 30.04.2012 12:16:17
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogProcessor {

    private static final Logger LOG = Logger.getLogger(SyslogProcessor.class);
    private GraylogServer server;

    public SyslogProcessor(GraylogServer server) {
        this.server = server;
    }

    public void messageReceived(String msg, InetAddress remoteAddress) throws Exception {
        // Convert to LogMessage
        LogMessage lm = parse(msg, remoteAddress);

        if (!lm.isComplete()) {
            LOG.debug("Skipping incomplete message.");
        }

        // Add to process buffer.
        LOG.debug("Adding received syslog message <" + lm.getId() +"> to process buffer: " + lm);
        server.getProcessBuffer().insert(lm);
    }

    private LogMessage parse(String msg, InetAddress remoteAddress) throws UnknownHostException {
        LogMessage lm = new LogMessage();

        SyslogServerEvent e = new SyslogServerEvent(msg, remoteAddress);
        System.out.println(e.getHost());
        System.out.println(e.getMessage());

        return lm;
    }

}
