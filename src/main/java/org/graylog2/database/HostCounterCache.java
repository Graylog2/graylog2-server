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

/**
 * Acts as cache for count updates in the hosts collection. Written to MongoDB
 * by a periodically running thread.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class HostCounterCache {

    private ConcurrentMap<String, Integer> cache = new ConcurrentHashMap<String, Integer>();

    /**
     * Increment counter cache by 1 for a host.
     *
     * @param hostname The host of which the counter to increment.
     */
    public void increment(String hostname) {
        int old = 0;

        if (this.cache.containsKey(hostname)) {
            old = this.cache.get(hostname);
        }

        this.cache.put(hostname, old+1);
    }

    /**
     * Remove host from counter.
     *
     * @param hostname The host of which the counter to reset.
     */
    public void reset(String hostname) {
        if (this.cache.containsKey(hostname)) {
            this.cache.remove(hostname);
        }
    }

    /**
     * Get the current count of host.
     *
     * @param hostname The host of which the count to get.
     */
    public int getCount(String hostname) {
        return this.cache.get(hostname) == null ? 0 : this.cache.get(hostname);
    }

    /**
     * Get all hostnames that are currently in the cache.
     */
    public Set<String> getAllHosts() {
        return this.cache.keySet();
    }

}
