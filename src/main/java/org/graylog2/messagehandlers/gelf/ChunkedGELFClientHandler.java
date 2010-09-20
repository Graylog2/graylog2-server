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
    public ChunkedGELFClientHandler(DatagramPacket clientMessage) throws UnsupportedEncodingException, InvalidGELFHeaderException {

        // 70 byte / 140 chars:
        // 3765 3335383765646265393165323131396537373066383266383363383432373733366661393430366534656436646665643833376366373239636437636661 0000 0002
        // 1: 0-2 byte: MAGIC NUMBER
        // 2: 2-66 byte: MESSAGE ID
        // 3: 66-68 byte: SEQUENCE NUMBER
        // 4: 68-70 byte: SEQUENCE COUNT

        GELFHeader header = GELF.extractGELFHeader(clientMessage);

        System.out.println("GOT MESSAGE: " + header.getSequenceNumber() + "/" + header.getSequenceCount());
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
