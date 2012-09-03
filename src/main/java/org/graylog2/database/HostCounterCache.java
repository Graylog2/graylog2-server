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

package org.graylog2.database;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Acts as cache for count updates in the hosts collection. Written to MongoDB
 * by a periodically running thread.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class HostCounterCache {

    private ConcurrentMap<String, AtomicInteger> cache = new ConcurrentHashMap<String, AtomicInteger>();

    /**
     * Increment counter cache by 1 for a host.
     *
     * @param hostname The host of which the counter to increment.
     */
    public void increment(String hostname) {
        AtomicInteger count = this.cache.putIfAbsent(hostname, new AtomicInteger(0));
        if (count != null) {
            count.incrementAndGet();
        }

    }

    /**
     * Get the current message count for host and reset counter.
     *
     * @param hostname The host for which the count to get.
     */
    public int getCountAndReset(String hostname) {
        AtomicInteger zero = new AtomicInteger(0);
        if (this.cache.remove(hostname, zero)) {
            return 0;
        } else {
            return this.cache.put(hostname, zero).get();
        }
    }

    /**
     * Get all hostnames that are currently in the cache.
     */
    public Set<String> getAllHosts() {
        return this.cache.keySet();
    }

}
