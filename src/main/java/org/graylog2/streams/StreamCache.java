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

package org.graylog2.streams;

import java.util.List;

import org.graylog2.GraylogServer;
import org.graylog2.SimpleObjectCache;

/**
 * StreamCache.java: Mar 26, 2011 11:25:41 PM
 *
 * Singleton caching the already fetched streams.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamCache extends SimpleObjectCache<List<Stream>> {

    private static StreamCache instance;
    private GraylogServer graylogServer;

    private StreamCache() { }

    public static synchronized StreamCache initialize(GraylogServer server) {
        StreamCache streamCache = getInstance();
        streamCache.setGraylogServer(server);
        return streamCache;
    }

    private void setGraylogServer(GraylogServer server) {
        this.graylogServer = server;
    }

    public GraylogServer getGraylogServer() {
        return graylogServer;
    }

    public static synchronized StreamCache getInstance() {
        if (instance == null) {
            instance = new StreamCache();
        }

        return instance;
    }
}