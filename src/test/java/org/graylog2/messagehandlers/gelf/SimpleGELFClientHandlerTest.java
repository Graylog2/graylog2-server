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

import java.net.DatagramPacket;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * SimpleGELFClientHandlerTest.java: Sep 17, 2010 6:50:42 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SimpleGELFClientHandlerTest {

    private String originalMessage = "{\"short_message\":\"something.\",\"full_message\":\"lol!\",\"host\":\"somehost\",\"level\":2,\"file\":\"example.php\",\"line\":1337}";
    
    /**
     * Test if ZLIB compressed non-chunked GELF messages are correctly decompressed.
     */
    @Test
    public void testDecompressionWithZLIB() throws Exception {
        // Build a datagram packet.
        DatagramPacket gelfMessage = GELFTestHelper.buildZLIBCompressedDatagramPacket(this.originalMessage);

        // Let the decompression take place.
        SimpleGELFClientHandler handler = new SimpleGELFClientHandler(gelfMessage);

        assertEquals(handler.getClientMessage(), this.originalMessage);
    }

    /**
     * Test if GZIP compressed non-chunked GELF messages are correctly decompressed.
     */
    @Test
    public void testDecompressionWithGZIP() throws Exception {
        // Build a datagram packet.
        DatagramPacket gelfMessage = GELFTestHelper.buildGZIPCompressedDatagramPacket(this.originalMessage);

        // Let the decompression take place.
        SimpleGELFClientHandler handler = new SimpleGELFClientHandler(gelfMessage);

        assertEquals(handler.getClientMessage(), this.originalMessage);
    }

}