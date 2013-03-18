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

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.graylog2.plugin.database.HostCounterCache;

/**
 * Acts as cache for count updates in the hosts collection. Written to MongoDB
 * by a periodically running thread.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class HostCounterCacheImpl implements HostCounterCache {

    private NonBlockingHashMap<String, Counter> hosts = new NonBlockingHashMap<String, Counter>();

    /**
     * Increment counter cache by 1 for a host.
     *
     * @param hostname The host of which the counter to increment.
     */
    @Override
    public void increment(String hostname) {
        Counter c = this.hosts.get( hostname );
        if (c != null) {
            c.increment();
        } else {
            c = new Counter();
            Counter c1 = this.hosts.putIfAbsent(hostname, c);
            if (c1 !=null ) c = c1;
            c.increment();
        }
    }

    /**
     * Remove host from counter.
     *
     * @param hostname The host of which the counter to reset.
     */
    @Override
    public void reset(String hostname) {
        Counter c = this.hosts.get(hostname);
        c.set(0);
    }

    /**
     * Get the current count of host.
     *
     * @param hostname The host of which the count to get.
     */
    @Override
    public int getCount(String hostname) {
        Counter c = this.hosts.get(hostname);
        return c == null ? 0 : (int)c.get();
    }

    /**
     * Get all hostnames that are currently in the cache.
     */
    @Override
    public Set<String> getAllHosts() {
        return this.hosts.keySet();
    }

}
