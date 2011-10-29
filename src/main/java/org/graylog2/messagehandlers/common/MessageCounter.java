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

package org.graylog2.messagehandlers.common;

import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;

/**
 * MessageCounter.java: Sep 20, 2011 6:47:42 PM
 *
 * Singleton holding the number of received messages for streams,
 * hosts and a total.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class MessageCounter {
    private static MessageCounter instance;

    private int total;
    private Map<ObjectId, Integer> streams;
    private Map<String, Integer> hosts;

    private MessageCounter() {
        // Initialize.
        this.resetAllCounts();
    }

    /**
     * @return MessageCounter singleton instance
     */
    public static synchronized MessageCounter getInstance() {
        if (instance == null) {
            instance = new MessageCounter();
        }

        return instance;
    }

    public int getTotalCount() {
        return this.total;
    }

    public Map<ObjectId, Integer> getStreamCounts() {
        return this.streams;
    }

    public Map<String, Integer> getHostCounts() {
        return this.hosts;
    }

    public void resetAllCounts() {
        this.resetTotal();
        this.resetStreamCounts();
        this.resetHostCounts();
    }

    public void resetHostCounts() {
        this.hosts = new HashMap<String, Integer>();
    }

    public void resetStreamCounts() {
        this.streams = new HashMap<ObjectId, Integer>();
    }

    public void resetTotal() {
        this.total = 0;
    }

    /**
     * Increment total count by 1.
     */
    public void incrementTotal() {
        this.countUpTotal(1);
    }

    /**
     * Count up the total count.
     *
     * @param x The value to add on top of current total count.
     */
    public void countUpTotal(int x) {
        this.total += x;
    }

    /**
     * Increment stream count by 1.
     *
     * @param streamId The ID of the stream which count to increment.
     */
    public void incrementStream(ObjectId streamId) {
        this.countUpStream(streamId, 1);
    }

    /**
     * Count up the count of a stream.
     *
     * @param streamId The ID of the stream which count to increment.
     * @param x The value to add on top of the current stream count.
     */
    public void countUpStream(ObjectId streamId, int x) {
        if (this.streams.containsKey(streamId)) {
            // There already is an entry. Increment.
            int oldCount = this.streams.get(streamId);
            this.streams.put(streamId, oldCount+x); // Overwrites old entry.
        } else {
            // First entry for this stream.
            this.streams.put(streamId, x);
        }
    }

    /**
     * Increment host count by 1.
     *
     * @param hostname The name of the host which count to increment.
     */
    public void incrementHost(String hostname) {
        this.countUpHost(hostname, 1);
    }

    /**
     * Count up the count of a host.
     *
     * @param hostname The name of the host which count to increment.
     * @param x The value to add on top of the current host count.
     */
    public void countUpHost(String hostname, int x) {
        if (this.hosts.containsKey(hostname)) {
            // There already is an entry. Increment.
            int oldCount = this.hosts.get(hostname);
            this.hosts.put(hostname, oldCount+x); // Overwrites old entry.
        } else {
            // First entry for this stream.
            this.hosts.put(hostname, x);
        }
    }

}