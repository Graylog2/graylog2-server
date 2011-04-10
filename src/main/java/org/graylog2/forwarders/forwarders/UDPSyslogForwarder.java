/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.forwarders.forwarders;

import org.apache.log4j.Logger;
import org.graylog2.forwarders.MessageForwarderIF;
import org.graylog2.messagehandlers.gelf.GELFMessage;

/**
 * SyslogForwarder.java: Apr 5, 2011 8:23:14 PM
 *
 * Forwards syslog messages to other syslog endpoints.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class UDPSyslogForwarder extends UDPForwarder implements MessageForwarderIF {

    private static final Logger LOG = Logger.getLogger(LogglyForwarder.class);
    
    private boolean succeeded = false;

    public UDPSyslogForwarder(String host, int port) {
        this.setHost(host);
        this.setPort(port);
    }

    public boolean forward(GELFMessage message) throws MessageForwarderConfigurationException {
        if (this.host.isEmpty() || this.port <= 0) {
            throw new MessageForwarderConfigurationException("Host is empty or port is invalid.");
        }

        if (message.convertedFromSyslog()) {
            this.succeeded = this.send(message.getRaw());
        } else {
            LOG.info("Not forwarding GELF messages via UDP syslog.");
            this.succeeded = false;
        }
        
        return this.succeeded();
    }

    /**
     * Indicates if the last forward has succeeded. This is not guaranteeing
     * delivery for the SyslogForwarder as it it sending UDP.
     *
     * @return
     */
    public boolean succeeded() {
        return this.succeeded;
    }

}
