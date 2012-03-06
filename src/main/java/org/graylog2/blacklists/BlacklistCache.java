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

import org.graylog2.GraylogServer;
import org.graylog2.SimpleObjectCache;

import java.util.List;

/**
 * StreamCache.java: Mar 31, 2011 6:11:14 PM
 *
 * Singleton caching the already fetched blacklist.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BlacklistCache extends SimpleObjectCache<List<Blacklist>> {
    
    private static BlacklistCache instance;
    private GraylogServer server;

    private BlacklistCache() { }

    public static synchronized BlacklistCache initialize(GraylogServer server) {
        BlacklistCache blacklistCache = getInstance();
        blacklistCache.setGraylogServer(server);
        return blacklistCache;
        
    }

    public static synchronized BlacklistCache getInstance() {
        if (instance == null) {
            instance = new BlacklistCache();
        }
        return instance;
    }

    private void setGraylogServer(GraylogServer server) {
        this.server = server;
    }

    public GraylogServer getGraylogServer() {
        return server;
    }
}