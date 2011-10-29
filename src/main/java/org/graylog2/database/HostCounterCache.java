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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    private Map<String, Integer> cache = new HashMap<String, Integer>();

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
        int old = 0;

        if (this.cache.containsKey(hostname)) {
            old = this.cache.get(hostname);
        }

        this.cache.put(hostname, old+1);
    }

    /**
     * Set counter cache to 0 for a host.
     *
     * @param hostname The host of which the counter to reset.
     */
    public void reset(String hostname) {
        if (this.cache.containsKey(hostname)) {
            this.cache.put(hostname, 0);
        }
    }

    /**
     * Get the current count of host.
     *
     * @param hostname The host of which the count to get.
     * @return
     */
    public int getCount(String hostname) {
        return this.cache.get(hostname) == null ? 0 : this.cache.get(hostname);
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
