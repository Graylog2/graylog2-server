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

package org.graylog2.inputs.gelf.gelf;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.codahale.metrics.Meter;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFChunkManager extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(GELFChunkManager.class);

    private Map<String, Map<Integer, GELFMessageChunk>> chunks = Maps.newConcurrentMap();
    private GELFProcessor processor;
    
	private InputHost server;

    // The number of seconds a chunk is valid. Every message with chunks older than this will be dropped.
    public static final int SECONDS_VALID = 5;
    private final Meter outdatedMessagesDropped;

    public GELFChunkManager(InputHost server) {

        this.processor = new GELFProcessor(server);
        this.server = server;

        this.outdatedMessagesDropped = server.metrics().meter(name(GELFChunkManager.class, "outdatedMessagesDropped"));
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
                        processor.messageReceived(new GELFMessage(chunksToByteArray(messageId)), getSourceInput(messageId));

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

    private MessageInput getSourceInput(String messageId) {
        try {
            return chunks.get(messageId).get(0).getSourceInput();
        } catch(Exception e) {
            LOG.error("Could not get source input ID from chunked GELF message.", e);
            return null;
        }
    }
    
    public boolean hasMessage(String messageId) {
        return chunks.containsKey(messageId);
    }

    public void insert(GELFMessage msg, MessageInput sourceInput) {
        insert(new GELFMessageChunk(msg, sourceInput));
    }

    public void insert(GELFMessageChunk chunk) {
        LOG.debug("Handling GELF chunk: {}", chunk);
        
        if (chunks.containsKey(chunk.getId())) {
            // Add chunk to partial message.
            chunks.get(chunk.getId()).put(chunk.getSequenceNumber(), chunk);
        } else {
            // First chunk of message.
            Map<Integer, GELFMessageChunk> c = Maps.newHashMap();
            c.put(chunk.getSequenceNumber(), chunk);
            chunks.put(chunk.getId(), c);
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
