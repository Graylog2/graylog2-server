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

import java.util.Set;
import org.graylog2.Core;

import org.graylog2.SimpleObjectCache;
import org.graylog2.plugin.streams.Stream;

/**
 * Singleton caching the already fetched streams.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamCache extends SimpleObjectCache<Set<Stream>> {

    private static StreamCache instance;

    private StreamCache() { }

    public static synchronized StreamCache initialize(Core server) {
        StreamCache streamCache = getInstance();
        return streamCache;
    }

    public static synchronized StreamCache getInstance() {
        if (instance == null) {
            instance = new StreamCache();
        }

        return instance;
    }
}