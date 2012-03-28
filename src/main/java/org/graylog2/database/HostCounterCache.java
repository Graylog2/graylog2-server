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

import java.lang.Integer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    private ConcurrentMap<String, Integer> cache = new ConcurrentHashMap<String, Integer>();

    private HostCounterCache() {}

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
        Integer old;

        old = this.cache.get(hostname);
        this.cache.put(hostname, (old) ? (old + 1) : 1);
    }

    /**
     * Remove host from counter.
     *
     * @param hostname The host of which the counter to reset.
     */
    public void reset(String hostname) {
        this.cache.remove(hostname);
    }

    /**
     * Get the current count of host.
     *
     * @param hostname The host of which the count to get.
     * @return
     */
    public int getCount(String hostname) {
        Integer result;

        result = this.cache.get(hostname);

        if (result == null) {
            return 0;
        }

        return result;
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
