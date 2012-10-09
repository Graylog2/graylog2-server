/**
 * Copyright 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
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
import org.graylog2.logmessage.LogMessageImpl;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFMessageForwarder extends UDPForwarder implements MessageForwarderIF {

    private static final Logger LOG = Logger.getLogger(GELFMessageForwarder.class);
    
    private boolean succeeded = false;

    public GELFMessageForwarder(String host, int port) {
        this.setHost(host);
        this.setPort(port);
    }

    public boolean forward(LogMessageImpl message) throws MessageForwarderConfigurationException {
 /*       if (this.host.isEmpty() || this.port <= 0) {
            throw new MessageForwarderConfigurationException("Host is empty or port is invalid.");
        }
        
        if (message.isChunked()) {
            LOG.info("Forwarding a chunked message.");
            for (GELFClientChunk chunk : message.getMessageChunks().values()) {
                LOG.info("Forwarding chunked GELF message chunk:" + chunk.toString());
                this.send(chunk.getRaw());
            }
        } else {
            if (!message.convertedFromSyslog()) {
                this.succeeded = this.send(message.getRaw());
            } else {
                this.succeeded = this.send(message.compress());
            }
        }*/

        return this.succeeded;
    }

    public boolean succeeded() {
        return this.succeeded;
    }

}
