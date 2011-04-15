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

package org.graylog2.hostgroups;

import java.util.ArrayList;
import org.graylog2.SimpleObjectCache;

/**
 * HostgroupCache.java: Apr 15, 2011 12:13:35 PM
 *
 * Singleton caching the already fetched hostgroups.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
class HostgroupCache extends SimpleObjectCache {

    private static HostgroupCache instance;

    private HostgroupCache() { }

    public synchronized static HostgroupCache getInstance() {
        if (instance == null) {
            instance = new HostgroupCache();
        }

        return instance;
    }

    @Override
    public ArrayList<Hostgroup> get() {
        return (ArrayList<Hostgroup>) super.get();
    }

    public void set(ArrayList<Hostgroup> groups) {
        super.set(groups);
    }

}
