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

import org.graylog2.streams.*;
import java.util.ArrayList;
import org.graylog2.Tools;

/**
 * StreamCache.java: Mar 31, 2011 6:11:14 PM
 *
 * Singleton caching the already fetched blacklist.
 *
 * THIS IS PRETTY DUPLICATED FROM THE STREAMS CACHE. May be done in a smarter
 * way soon. Not sure yet.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class BlacklistCache {

    public static final int TIMEOUT_SECONDS = 5;

    private static BlacklistCache instance;

    private ArrayList<Blacklist> blacklists = new ArrayList<Blacklist>();
    private int lastSet = 0;

    private BlacklistCache() { }

    public synchronized static BlacklistCache getInstance() {
        if (instance == null) {
            instance = new BlacklistCache();
        }
        return instance;
    }

    public void set(ArrayList<Blacklist> blacklists) {
        this.blacklists = blacklists;
        this.lastSet = Tools.getUTCTimestamp();
    }

    public ArrayList<Blacklist> get() {
        return blacklists;
    }

    public boolean valid() {
        // For the first request.
        if (this.lastSet == 0) {
            return false;
        }

        return this.lastSet >= (Tools.getUTCTimestamp()-TIMEOUT_SECONDS);
    }

}