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

package org.graylog2.database;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HostCounterCache.java: Feb 21, 2010 4:57:13 PM
 *
 * Acts as cache for count updates in the hosts collection. Written to MongoDB
 * by a periodically running thread.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class HostCounterCache {
    private static HostCounterCache instance;

    private ConcurrentMap<String, AtomicInteger> cache = new ConcurrentHashMap<String, AtomicInteger>();

    private HostCounterCache() { }

    /**
     *
     * @return
     */
    public static synchronized HostCounterCache getInstance() {
        if (instance == null) {
            instance = new HostCounterCache();
        }
        return instance;
    }

    /**
     * Increment counter cache by 1 for a host.
     *
     * @param hostname The host of which the counter to increment.
     */
    public void increment(String hostname) {
        //http://stackoverflow.com/questions/2539654/java-concurrency-many-writers-one-reader/2539761#2539761

        AtomicInteger counter = cache.get(hostname);
        if (counter == null) {
            counter = cache.putIfAbsent(hostname, new AtomicInteger(1));
        }

        if (counter != null) {
            counter.incrementAndGet();
        }
    }

    /**
     * Get the current count of host and remove host from counter.
     *
     * @param hostname The host of which the count to get and counter to reset.
     * @return
     */
    public int getCountAndReset(String hostname) {
        //http://www.javamex.com/tutorials/synchronization_concurrency_8_hashmap2.shtml
        //(section: "Truly atomic updates")

        int count = 0;

        while (true) {
            AtomicInteger counter = cache.get(hostname);

            if (counter == null) {
                break; //_counter_ not found or removed. Exit loop with _count_ unmodified.
            }

            if (!cache.remove(hostname, counter)) {
                continue; //Another thread removed _counter_. Retry loop.
            }

            count = counter.get();
            break; //Successfully removed _counter_. Exit loop with new _count_ value.
        }

        return count;
    }

    /**
     * Get all hostnames that are currently in the cache.
     * 
     * @return
     */
    public Set<String> getAllHosts() {
        return this.cache.keySet();
    }

}
