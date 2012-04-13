/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.inputs.gelf;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

/**
 * GELFChunkManager.java: 13.04.2012 22:38:40
 *
 * Describe me.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFChunkManager extends Thread {

    private static final Logger LOG = Logger.getLogger(GELFChunkManager.class);

    private Map<String, Map<Integer, GELFMessageChunk>> chunks = new ConcurrentHashMap<String, Map<Integer, GELFMessageChunk>>();

    @Override
    public void run() {
        // Check for chunks to discard.
    }

    public void insert(GELFMessageChunk chunk) {
        LOG.debug("Handling GELF chunk: " + chunk);
        
        if (chunks.containsKey(chunk.getId())) {
            // Add chunk to partial message.
            chunks.get(chunk.getId()).put(chunk.getSequenceNumber(), chunk);
        } else {
            // First chunk of message.
            Map<Integer, GELFMessageChunk> c = new HashMap<Integer, GELFMessageChunk>();
            c.put(chunk.getSequenceNumber(), chunk);
            chunks.put(chunk.getId(), c);
        }

    }

}
