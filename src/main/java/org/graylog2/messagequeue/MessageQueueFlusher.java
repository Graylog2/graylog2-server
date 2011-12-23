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
package org.graylog2.messagequeue;

import java.util.List;
import org.apache.log4j.Logger;
import org.graylog2.indexer.Indexer;
import org.graylog2.messagehandlers.gelf.GELFMessage;

/**
 * MessageQueueFlusher.java: Nov 17, 2011 7:02:40 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageQueueFlusher extends Thread {

    private static final Logger LOG = Logger.getLogger(MessageQueueFlusher.class);

    @Override
    public void run() {
        try {
            LOG.info("Message queue flusher called!");
            
            LOG.info("Closing message queue.");
            MessageQueue.getInstance().close();
            
            List<GELFMessage> messages = MessageQueue.getInstance().readAll();
            LOG.info("Flushing all " + messages.size() + " messages to indexer.");
            Indexer.bulkIndex(messages);
        } catch (Exception e) {
            LOG.warn("Error while flushing messages from queue: " + e.getMessage(), e);
        } finally {
            LOG.info("Finalizing message queue flushing.");
        }
    }

}