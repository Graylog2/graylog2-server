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

import java.util.ArrayList;
import org.graylog2.SimpleObjectCache;

/**
 * StreamCache.java: Mar 26, 2011 11:25:41 PM
 *
 * Singleton caching the already fetched streams.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamCache extends SimpleObjectCache {

    private static StreamCache instance;

    private StreamCache() { }

    public synchronized static StreamCache getInstance() {
        if (instance == null) {
            instance = new StreamCache();
        }
        
        return instance;
    }

    @Override
    public ArrayList<Stream> get() {
        return (ArrayList<Stream>) super.get();
    }

    public void set(ArrayList<Stream> streams) {
        super.set(streams);
    }

}