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

package org.graylog2;

import org.bson.types.ObjectId;
import java.util.Map;
import com.google.common.collect.Maps;

/**
 * Singleton holding the number of received messages for streams,
 * hosts and a total.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class MessageCounter {

    private int total;
    private final Map<String, Integer> streams = Maps.newConcurrentMap();
    private final Map<String, Integer> hosts = Maps.newConcurrentMap();

    private int throughput = 0;
    private int highestThroughput = 0;

    public int getTotalCount() {
        return this.total;
    }

    public Map<String, Integer> getStreamCounts() {
        return this.streams;
    }

    public Map<String, Integer> getHostCounts() {
        return this.hosts;
    }

    public int getThroughput() {
        return this.throughput;
    }

    public int getHighestThroughput() {
        return this.highestThroughput;
    }

    public void resetAllCounts() {
        this.resetTotal();
        this.resetStreamCounts();
        this.resetHostCounts();
    }

    public void resetHostCounts() {
        this.hosts.clear();
    }

    public void resetStreamCounts() {
        this.streams.clear();
    }

    public void resetTotal() {
        this.total = 0;
    }

    public void resetThroughput() {
        this.throughput = 0;
    }

    /**
     * Increment total count by 1.
     */
    public void incrementTotal() {
        this.countUpTotal(1);
    }

    /**
     * Increment five second throughput by 1.
     */
    public void incrementThroughput() {
        this.countUpThroughput(1);
    }

    /**
     * Count up the total count.
     *
     * @param x The value to add on top of current total count.
     */
    public void countUpTotal(final int x) {
        this.total += x;
    }

    /**
     * Counts up the five second througput counter which is handled and reset by
     * the ServerValueWriterThread.
     *
     * @param x The value to add on top of five second throuput.
     */
    public void countUpThroughput(final int x) {
        this.throughput += x;

        if (this.throughput > this.highestThroughput) {
            this.highestThroughput = this.throughput;
        }
    }

    /**
     * Increment stream count by 1.
     *
     * @param streamId The ID of the stream which count to increment.
     */
    public void incrementStream(final ObjectId streamId) {
        this.countUpStream(streamId, 1);
    }

    /**
     * Count up the count of a stream.
     *
     * @param streamId The ID of the stream which count to increment.
     * @param x The value to add on top of the current stream count.
     */
    public synchronized void countUpStream(final ObjectId streamId, final int x) {
        if (this.streams.containsKey(streamId.toString())) {
            // There already is an entry. Increment.
            final int oldCount = this.streams.get(streamId.toString());
            this.streams.put(streamId.toString(), oldCount+x); // Overwrites old entry.
        } else {
            // First entry for this stream.
            this.streams.put(streamId.toString(), x);
        }
    }

    /**
     * Increment host count by 1.
     *
     * @param hostname The name of the host which count to increment.
     */
    public void incrementHost(final String hostname) {
        this.countUpHost(hostname, 1);
    }

    /**
     * Count up the count of a host.
     *
     * @param hostname The name of the host which count to increment.
     * @param x The value to add on top of the current host count.
     */
    public synchronized void countUpHost(String hostname, final int x) {
        hostname = Tools.encodeBase64(hostname);
        if (this.hosts.containsKey(hostname)) {
            // There already is an entry. Increment.
            final int oldCount = this.hosts.get(hostname);
            this.hosts.put(hostname, oldCount+x); // Overwrites old entry.
        } else {
            // First entry for this stream.
            this.hosts.put(hostname, x);
        }
    }

}