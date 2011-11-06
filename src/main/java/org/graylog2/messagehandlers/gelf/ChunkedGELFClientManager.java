/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.gelf;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ChunkedGELFClientManager.java: Sep 20, 2010 6:52:36 PM
 *
 * Singleton. Managing chunks of GELF messages. Ordering them, reporting when complete and
 * discards incomplete after a given time.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class ChunkedGELFClientManager {

    private static ConcurrentMap<String, ChunkedGELFMessage> messageMap = new ConcurrentHashMap<String, ChunkedGELFMessage>();

    private static ChunkedGELFClientManager instance;

    private ChunkedGELFClientManager() { }

    /**
     *
     * @return
     */
    public static synchronized ChunkedGELFClientManager getInstance() {
        if (instance == null) {
            instance = new ChunkedGELFClientManager();
        }
        return instance;
    }

    /**
     * Add a chunk to it's message
     *
     * @param chunk
     * @return NULL if message is not yet complete, the complete ChunkedGELFMessage if this was the last missing chunk
     * @throws ForeignGELFChunkException
     * @throws InvalidGELFChunkException
     */
    public ChunkedGELFMessage insertChunk(GELFClientChunk chunk) throws ForeignGELFChunkException, InvalidGELFChunkException {
        ChunkedGELFMessage fullMessage = messageMap.get(chunk.getHash());
        
        if (fullMessage == null) {
            // This is the first chunk of this message. Create a new message.
            fullMessage = new ChunkedGELFMessage();
        }
        
        // Add this chunk to the message.
        fullMessage.insertChunk(chunk);

        // Save the message with the new chunk.
        messageMap.put(chunk.getHash(), fullMessage);

        if (fullMessage.isComplete()) {
            return fullMessage;
        }

        return null;
    }

    /**
     * Remove a message
     *
     * @param hash The has of the message to remove
     */
    public void dropMessage(String hash) {
        messageMap.remove(hash);
    }

    /**
     * Get the message map
     *
     * @return
     */
    public ConcurrentMap<String, ChunkedGELFMessage> getMessageMap() {
        return messageMap;
    }

}
