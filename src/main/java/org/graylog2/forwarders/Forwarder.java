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

package org.graylog2.forwarders;

import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.streams.Stream;

/**
 * Forwarder.java: Apr 2, 2011 11:27:02 PM
 *
 * Forwards messages to other endpoints. (i.e. other GELF or syslog servers)
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Forwarder {

    private static final Logger LOG = Logger.getLogger(Forwarder.class);

    /**
     * Forward a GELF message to it's streams forward endpoints.
     *
     * @param message The GELF message to forward.
     * @return Number of endpoints the message was successfully forwarded to.
     * @throws Exception
     */
    public static int forward(GELFMessage message) throws Exception {
        int succeeded = 0;
        for (Stream stream : message.getStreams()) {
            try {
                for (ForwardEndpoint endpoint : stream.getForwardedTo()) {
                    MessageForwarderIF launchPad = endpoint.getForwarder();
                    launchPad.forward(message);

                    if (launchPad.succeeded()) {
                        succeeded++;
                    }
                }
            } catch (Exception e) {
                LOG.warn("Skipping forwarding of message for a stream: " + e.getMessage(), e);
                continue;
            }
        }

        return succeeded;
    }

}