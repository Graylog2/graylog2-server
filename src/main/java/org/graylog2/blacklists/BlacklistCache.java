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

package org.graylog2.blacklists;

import java.util.ArrayList;
import org.graylog2.SimpleObjectCache;

/**
 * StreamCache.java: Mar 31, 2011 6:11:14 PM
 *
 * Singleton caching the already fetched blacklist.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class BlacklistCache extends SimpleObjectCache {
    
    private static BlacklistCache instance;

    private BlacklistCache() { }

    public synchronized static BlacklistCache getInstance() {
        if (instance == null) {
            instance = new BlacklistCache();
        }
        return instance;
    }

    @Override
    public ArrayList<Blacklist> get() {
        return (ArrayList<Blacklist>) super.get();
    }

    public void set(ArrayList<Blacklist> lists) {
        super.set(lists);
    }

}