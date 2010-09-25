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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;

/**
 * ChunkedGELFClient.java: Sep 14, 2010 6:38:38 PM
 *
 * Handling a GELF client message consisting on more than one UDP message.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ChunkedGELFClientHandler extends GELFClientHandlerBase implements GELFClientHandlerIF {

    /**
     * Representing a GELF client based on more than one UDP message.
     *
     * @param clientMessage The raw data the GELF client sent. (JSON string)
     * @param threadName The name of the GELFClientHandlerThread that called this.
     */
    public ChunkedGELFClientHandler(DatagramPacket clientMessage) throws UnsupportedEncodingException, InvalidGELFHeaderException, IOException {
        GELFHeader header = GELF.extractGELFHeader(clientMessage);

        GELFClientChunk chunk = new GELFClientChunk();
        chunk.setHash(header.getHash());
        chunk.setSequenceCount(header.getSequenceCount());
        chunk.setSequenceNumber(header.getSequenceNumber());
        chunk.setData(GELF.extractData(clientMessage));


        // Insert the chunk.
        int messageState = 0;
        try {
            messageState = ChunkedGELFClientManager.getInstance().insertChunk(chunk);
        } catch (InvalidGELFChunkException e) {
            throw new InvalidGELFHeaderException(e.toString());
        } catch (ForeignGELFChunkException e) {
            throw new InvalidGELFHeaderException(e.toString());
        }

        //// DEBUG
        /*try {
            DatagramPacket bla = new DatagramPacket(chunk.getData(), chunk.getData().length);
            System.out.println(chunk.getSequenceNumber() + ":" + GELF.getGELFType(bla));
        } catch (InvalidGELFCompressionMethodException ex) {
            System.out.println(chunk.getSequenceNumber() + ":" + ex.toString());
        }*/
        ///////

        // Fully handle message if complete.
        if (messageState == ChunkedGELFClientManager.MESSAGE_IS_COMPLETE) {
            // ALSO TEST IF MESSAGE HAS ALL CHUNKS NOT ONLY IF ENOUGH CHUNKS!
            System.out.println("MESSAGE IS COMPLETE! HANDLING NOW!");

        }
    }

    /**
     * Handles the client: Decodes JSON, Stores in MongoDB, ReceiveHooks
     * 
     * @return
     */
    public boolean handle() {
        return true;
    }

}
