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

package org.graylog2.gelf;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFChunkManager extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(GELFChunkManager.class);

    private ConcurrentMap<String, Map<Integer, GELFMessageChunk>> chunks = new ConcurrentHashMap<String, Map<Integer,GELFMessageChunk>>(256,0.75f,Runtime.getRuntime().availableProcessors()*4);
    private GELFProcessor processor;

    // The number of seconds a chunk is valid. Every message with chunks older than this will be dropped.
    public static final int SECONDS_VALID = 5;
    private final Meter outdatedMessagesDropped = Metrics.newMeter(GELFChunkManager.class, "OutdatedMessagesDropped", "messages", TimeUnit.SECONDS);

    public GELFChunkManager(Core server) {
        this.processor = new GELFProcessor(server);
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!chunks.isEmpty()) {
                    LOG.debug("Dumping GELF chunk map [{}]:\n{}", chunks.size(), humanReadableChunkMap());
                }
                
                // Check for complete or outdated messages.
                for (Map.Entry<String, Map<Integer, GELFMessageChunk>> message : chunks.entrySet()) {
                    String messageId = message.getKey();

                    // Outdated?
                    if (isOutdated(messageId)) {
                        outdatedMessagesDropped.mark();
                        
                        LOG.debug("Not all chunks of <{}> arrived in time. Dropping. [{}s]", messageId, SECONDS_VALID);
                        dropMessage(messageId);
                        continue;
                    }

                    // Not oudated. Maybe complete?
                    if (isComplete(messageId)) {
                        // We got a complete message! Re-assemble and insert to GELFProcessor.
                        LOG.debug("Message <{}> seems to be complete. Handling now.", messageId);
                        processor.messageReceived(new GELFMessage(chunksToByteArray(messageId)));

                        // Message has been handled. Drop it.
                        LOG.debug("Message <{}> is now being processed. Dropping from chunk map.", messageId);
                        dropMessage(messageId);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error in GELFChunkManager", e);
            }

            try { Thread.sleep(1000); } catch (InterruptedException ex) { /* trololol */}
        }
    }

    public boolean isComplete(String messageId) {
        if (!chunks.containsKey(messageId)) {
            LOG.debug("Message <{}> not in chunk map. Not checking if complete.", messageId);
            return false;
        }

        if (!chunks.get(messageId).containsKey(0)) {
            LOG.debug("Message <{}> does not even contain first chunk. Not complete!", messageId);
            return false;
        }

        int claimedSequenceCount = chunks.get(messageId).get(0).getSequenceCount();
        if (claimedSequenceCount == chunks.get(messageId).size()) {
            // Message seems to be complete.
            return true;
        }

        return false;
    }

    public boolean isOutdated(String messageId) {
        if (!chunks.containsKey(messageId)) {
            LOG.debug("Message <{}> not in chunk map. Not checking if outdated.", messageId);
            return false;
        }

        int limit = (int) (System.currentTimeMillis() / 1000) - SECONDS_VALID;
        
        // Checks for oldest chunk arrival date.
        for (Map.Entry<Integer, GELFMessageChunk> chunk : chunks.get(messageId).entrySet()) {
            if (chunk.getValue().getArrival() < limit) {
                return true;
            }
        }

        return false;
    }

    public void dropMessage(String messageId) {
        if (chunks.containsKey(messageId)) {
            chunks.remove(messageId);
        } else {
            LOG.debug("Message <{}> not in chunk map. Not dropping.", messageId);
        }
    }

    public byte[] chunksToByteArray(String messageId) throws Exception {
        if (!chunks.containsKey(messageId)) {
            throw new Exception("Message <" + messageId + "> not in chunk map. Cannot re-assemble.");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map.Entry<Integer, GELFMessageChunk> chunk : chunks.get(messageId).entrySet()) {
            out.write(chunk.getValue().getData(), 0, chunk.getValue().getData().length);
        }

        return out.toByteArray();
    }
    
    public boolean hasMessage(String messageId) {
        return chunks.containsKey(messageId);
    }

    public void insert(GELFMessage msg) {
        insert(new GELFMessageChunk(msg));
    }

    public void insert(GELFMessageChunk chunk) {
        LOG.debug("Handling GELF chunk: {}", chunk);
        
        Map<Integer, GELFMessageChunk> messageChunks = chunks.get(chunk.getId());
        if (messageChunks!=null) {
            // Add chunk to partial message.
            messageChunks.put(chunk.getSequenceNumber(), chunk);
        } else {
            // First chunk of message.
            // There is a little concurrency expected on individual message sequence maps, still it can happen
            // because individual chunks can be processed in parallel by udp processing input threads
            // so sync map is ok here.
            Map<Integer, GELFMessageChunk> c =Collections.synchronizedMap( new HashMap<Integer, GELFMessageChunk>() );
            c.put(chunk.getSequenceNumber(), chunk);
            c=chunks.putIfAbsent(chunk.getId(), c);
            if (c!=null) {
                // unexpected concurrency happened on map init. need to reinsert chunk top just added map 
                c.put(chunk.getSequenceNumber(), chunk);
            }
        }

    }

    public String humanReadableChunkMap() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Map<Integer, GELFMessageChunk>> entry : chunks.entrySet()) {
            sb.append("Message <").append(entry.getKey()).append("> ");
            sb.append("\tChunks:\n");
            for(Map.Entry<Integer, GELFMessageChunk> chunk : entry.getValue().entrySet()) {
                sb.append("\t\t").append(chunk.getValue()).append(("\n"));
            }
        }

        return sb.toString();
    }

}
