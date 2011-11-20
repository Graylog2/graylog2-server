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

import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.gelf.GELFMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MessageQueue.java: Nov 16, 2011 5:28:26 PM
 * <p/>
 * Singleton holding messages to be flushed to ElasticSearch.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageQueue {

    public static final int SIZE_LIMIT_UNLIMITED = 0;

    private static final Logger LOG = Logger.getLogger(MessageQueue.class);

    private static MessageQueue instance;

    private Queue<GELFMessage> queue = new ConcurrentLinkedQueue<GELFMessage>();
    private boolean closed = false;

    private int sizeLimit = SIZE_LIMIT_UNLIMITED;

    private MessageQueue() {}

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
     * Adds a new {@link GELFMessage} to the tail of the queue.
     *
     * @param message The {@link GELFMessage} to enqueue
     * @return {@literal true} in case of success
     * @throws QueueClosedException       if the queue has already been closed
     * @throws QueueLimitReachedException if the maximum size of the queue has been reached
     */
    public boolean add(GELFMessage message) throws QueueClosedException, QueueLimitReachedException {
        if (this.closed) {
            throw new QueueClosedException();
        }

        if (this.sizeLimit != SIZE_LIMIT_UNLIMITED) {
            if (this.queue.size() >= this.sizeLimit) {
                throw new QueueLimitReachedException();
            }
        }

        return queue.add(message);
    }

    /**
     * Retrieves and removes the head of the queue.
     *
     * @return the first {@link GELFMessage} in the queue.
     */
    public GELFMessage poll() {
        return queue.poll();
    }

    /**
     * You can set a size (# of messages) to which the queue can grow as maximum.
     * If no limit is set, the queue size is unlimited.
     * <p/>
     * Set this to {@link #SIZE_LIMIT_UNLIMITED} if you don't want to limit it.
     *
     * @param maximumSize The number of maximum messages
     */
    public void setMaximumSize(int maximumSize) {
        this.sizeLimit = maximumSize;
    }

    /**
     * Get the number of elements in the queue
     *
     * @return the total number of elements in the queue.
     */
    public int getSize() {
        return queue.size();
    }

    /**
     * Closes the queue so that no more messages can be added.
     * <p/>
     * Can be used before flushing whole queue at shutdown.
     *
     * @see #open()
     */
    public void close() {
        this.closed = true;
    }

    /**
     * Reopens a formerly closed queue
     *
     * @see #close()
     */
    public void open() {
        this.closed = false;
    }

    /**
     * Reads a batch of messages from the queue.
     *
     * IMPORTANT: The messages are
     * removed from the queue while reading. Make sure to actually handle them
     * in some way.
     *
     * @param batchSize The number of messages to read from the queue
     * @return A {@link List} of messages.
     */
    public List<GELFMessage> readBatch(int batchSize) {
        List<GELFMessage> messages = new ArrayList<GELFMessage>();

        for (int i = 0; !queue.isEmpty() && i < batchSize; i++) {
            messages.add(poll());
        }

        LOG.debug("Read " + messages.size() + " messages from queue.");

        return messages;
    }

    /**
     * Reads all messages from the queue.
     * <p/>
     * IMPORTANT: The messages are removed from the queue while reading. Make sure to actually handle them in some way.
     * <p/>
     * It is also a good idea to call {@link #close()} on the queue before calling this.
     *
     * @return A {@link List} containing all {@link GELFMessage}s in the queue.
     */
    public List<GELFMessage> readAll() {

        return readBatch(getSize());
    }

}