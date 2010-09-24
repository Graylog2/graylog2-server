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

import java.util.HashMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * ChunkedGELFClientManager.java: Sep 20, 2010 6:52:36 PM
 *
 * Singleton. Managing chunks of GELF messages. Ordering them, reporting when complete and
 * discards incomplete after a given time.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public final class ChunkedGELFClientManager {

    private static HashMap<String, ChunkedGELFMessage> messageMap = new HashMap<String, ChunkedGELFMessage>();

    private static ChunkedGELFClientManager INSTANCE;

    public static final int MESSAGE_IS_COMPLETE = 1;
    public static final int MESSAGE_IS_INCOMPLETE = 2;

    private ChunkedGELFClientManager() { }

    public synchronized static ChunkedGELFClientManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChunkedGELFClientManager();
        }
        return INSTANCE;
    }

    public int insertChunk(GELFClientChunk chunk) throws ForeignGELFChunkException, InvalidGELFChunkException {
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
            /////
            try {
                byte[] result = new byte[8192*3];
                Inflater decompresser = new Inflater();
                decompresser.setInput(fullMessage.getData(), 0, fullMessage.getData().length);
                int finalLength = decompresser.inflate(result);
                System.out.println("FULL MESSAGE:" + new String(result, 0, finalLength, "UTF-8"));
            } catch(Exception e) {
                System.out.println("Damn: " + e.getMessage());
            }
            /////
            return MESSAGE_IS_COMPLETE;
        }

        return MESSAGE_IS_INCOMPLETE;
    }

    public HashMap<String, ChunkedGELFMessage> getMessageMap() {
        return messageMap;
    }

}
