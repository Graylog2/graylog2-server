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

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import org.graylog2.Core;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 * @author Oleg Anastasyev<oa@odnoklassniki.ru> redesigned for performance 
 */
public class GELFChunkManager extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(GELFChunkManager.class);

    private ConcurrentSkipListMap<Long, GELFChunks> chunks = new ConcurrentSkipListMap<Long, GELFChunks>();
    
    private GELFProcessor processor;

    // The number of seconds a chunk is valid. Every message with chunks older than this will be dropped.
    public static final int SECONDS_VALID = 5;
    private final Meter outdatedMessagesDropped = Metrics.newMeter(GELFChunkManager.class, "OutdatedMessagesDropped", "messages", TimeUnit.SECONDS);

    public GELFChunkManager(Core server) {
        this.processor = new GELFProcessor(server);
    }

    @Override
    public void run() {
        /**
         * On the main loop we only expire messages hoping, that worker cores will not generate
         * too much chunks to expire (otherwise we just run out of memory)
         */
        while (true) {
            try {
                if (LOG.isDebugEnabled() && !chunks.isEmpty()) {
                    LOG.debug("Dumping GELF chunk map [{}]:\n{}", chunks.size(), humanReadableChunkMap());
                }
                
                // Check for complete or outdated messages.
                for (Entry<Long, GELFChunks> message : chunks.entrySet()) {
                    long messageId = message.getKey();

                    // Outdated?
                    if (isOutdated(message.getValue())) {
                        outdatedMessagesDropped.mark();
                        
                        LOG.debug("Not all chunks of <{}> arrived in time. Dropping. [{}s]", messageId, SECONDS_VALID);
                        dropMessage(messageId);
                        continue;
                    }

                }
            } catch (Exception e) {
                LOG.error("Error in GELFChunkManager", e);
            }

            try { Thread.sleep(1000); } catch (InterruptedException ex) { /* trololol */}
        }
    }

    public boolean isOutdated(GELFChunks gelfChunks) {

        int limit = (int) (System.currentTimeMillis() / 1000) - SECONDS_VALID;
        
        return gelfChunks.getArrival() < limit;
    }

    public void dropMessage(long messageId) {
        if (chunks.remove(messageId)==null) {
            LOG.debug("Message <{}> not in chunk map. Not dropping.", messageId);
        }
    }

    public boolean hasMessage(long messageId) {
        return chunks.containsKey(messageId);
    }

    public void insert(GELFMessage msg) throws MessageParseException, BufferOutOfCapacityException {
        insert(new GELFMessageChunk(msg));
    }

    public void insert(GELFMessageChunk chunk) throws MessageParseException, BufferOutOfCapacityException {
        LOG.debug("Handling GELF chunk: {}", chunk);
        
        GELFChunks messageChunks = chunks.get(chunk.getId());
        if (messageChunks!=null) {
            // Add chunk to partial message.
            if ( messageChunks.add(chunk) ) {

                receiveAssembledMessage(chunk, messageChunks);
                
            }
        } else {
            // First chunk of message.
            messageChunks = new GELFChunks(chunk);
            
            messageChunks=chunks.putIfAbsent(chunk.getId(), messageChunks);
            if (messageChunks!=null) {
                // unexpected concurrency happened on map init. need to reinsert chunk top just added map 
                if ( messageChunks.add(chunk) )  {
                    receiveAssembledMessage(chunk, messageChunks);
                }
            }
        }

    }

    protected void receiveAssembledMessage(GELFMessageChunk chunk, GELFChunks messageChunks)
            throws BufferOutOfCapacityException, MessageParseException
    {
        // We got a complete message! Re-assemble and insert to GELFProcessor.
        LOG.debug("Message <{}> seems to be complete. Handling now.", chunk.getId());
        processor.messageReceived(messageChunks.assembleGELFMessage());

        // Message has been handled. Drop it.
        dropMessage(chunk.getId());
    }

    public String humanReadableChunkMap() {
        StringBuilder sb = new StringBuilder();

        for (Entry<Long, GELFChunks> entry : chunks.entrySet()) {
            sb.append("Message <").append(entry.getKey()).append("> ");
            sb.append("\tChunks:\n");
            sb.append(entry.getValue());
        }

        return sb.toString();
    }

    /*
     * tests use
     */
    public boolean isComplete(long msgId)
    {
        return hasMessage(msgId) && chunks.get(msgId).isComplete();
    }

    /*
     * tests use
     */
    public boolean isOutdated(long msgId)
    {
        return hasMessage(msgId) && isOutdated( chunks.get(msgId) );
    }

    /**
     * @return
     */
    public int size()
    {
        return chunks.size();
    }


}
