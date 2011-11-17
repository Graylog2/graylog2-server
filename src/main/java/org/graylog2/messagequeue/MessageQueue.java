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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.gelf.GELFMessage;

/**
 * MessageQueue.java: Nov 16, 2011 5:28:26 PM
 *
 * Singleton holding messages to be flushed to ElasticSearch.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageQueue {

    private static final Logger LOG = Logger.getLogger(MessageQueue.class);

    private static MessageQueue instance;

    public Queue<GELFMessage> queue = new ConcurrentLinkedQueue<GELFMessage>();
    private boolean closed = false;

    private MessageQueue() { }

    /**
     * @return MessageCache singleton instance
     */
    public static synchronized MessageQueue getInstance() {
        if (instance == null) {
            instance = new MessageQueue();
        }

        return instance;
    }

    /**
     * Adds a new GELF message to the tail of the queue.
     *
     * @param message The GELF message to add
     * @return true in case of success
     */
    public boolean add(GELFMessage message) throws QueueClosedException {
        if (this.closed) {
            throw new QueueClosedException();
        }
        
        return queue.add(message);
    }

    /**
     * Retrieves and removes the head of the queue.
     *
     * @return the first GELFMessage in the queue.
     */
    public GELFMessage poll() {
        return queue.poll();
    }
    
    /**
     * @return the total size of the queue.
     */
    public int getSize() {
        return queue.size();
    }

    /**
     * Closes the queue - No more messages can be added.
     * Can be used before flushing whole queue at shutdown.
     */
    public void close() {
        this.closed = true;
    }

    /**
     * See close()
     */
    public void open() {
        this.closed = false;
    }

    /**
     * Reads a batch of messages from the queue. IMPORTANT: The messags are
     * removed from the queue while reading. Make sure to actually handle them
     * in some way.
     *
     * @param batchSize
     * @return A list of messages.
     */
    public List<GELFMessage> readBatch(int batchSize) {
        List<GELFMessage> messages = new ArrayList<GELFMessage>();
        for(int i = 0; i < batchSize; i++) {
            GELFMessage msg = this.poll();

            if (msg == null) {
                LOG.info("Reached end of message queue at element #" + i + " - Not reading any further.");
                break;
            } else {
                messages.add(msg);

                if (i == batchSize-1) {
                    LOG.info("Read " + batchSize + " messages from queue.");
                }
            }
        }

        return messages;
    }

    /**
     * Reads all messages from the queue. IMPORTANT: The messags are
     * removed from the queue while reading. Make sure to actually handle them
     * in some way.
     *
     * It is also a good idea to close() the queue before calling this.
     *
     * @return A list containing all messages in the queue.
     */
    public List<GELFMessage> readAll() {
        return this.readBatch(this.getSize());
    }

}