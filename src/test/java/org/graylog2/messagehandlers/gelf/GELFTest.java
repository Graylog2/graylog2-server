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
 * GELFTest.java: Sep 17, 2010 8:19:07 PM
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFTest {

    private String originalMessage = "{\"short_message\":\"something.\",\"full_message\":\"lol!\",\"host\":\"somehost\",\"level\":2,\"file\":\"example.php\",\"line\":1337}";

    /**
     * Test isChunkedMessage with a ZLIB encrypted message
     */
    @Test
    public void testIsChunkedMessageWithZLIBEncryption() throws Exception {
        // Build a datagram packet.
        DatagramPacket gelfMessage = GELFTestHelper.buildZLIBCompressedDatagramPacket(this.originalMessage);

        assertFalse(GELF.isChunkedMessage(gelfMessage));
    }

    /**
     * Test isChunkedMessage with a GZIP encrypted message
     */
    @Test
    public void testIsChunkedMessageWithGZIPEncryption() throws Exception {
        // Build a datagram packet.
        DatagramPacket gelfMessage = GELFTestHelper.buildGZIPCompressedDatagramPacket(this.originalMessage);

        assertFalse(GELF.isChunkedMessage(gelfMessage));
    }

    /**
     * Test getGELFType method with a ZLIB encrypted message
     */
    @Test
    public void testGetGELFTypeWithZLIBEncryption() throws Exception {
        // Build a datagram packet.
        DatagramPacket gelfMessage = GELFTestHelper.buildZLIBCompressedDatagramPacket(this.originalMessage);

        assertEquals(GELF.TYPE_ZLIB, GELF.getGELFType(gelfMessage));
    }

    /**
     * Test getGELFType method with a GZIP encrypted message
     */
    @Test
    public void testGetGELFTypeWithGZIPEncryption() throws Exception {
        // Build a datagram packet.
        DatagramPacket gelfMessage = GELFTestHelper.buildGZIPCompressedDatagramPacket(this.originalMessage);

        assertEquals(GELF.TYPE_GZIP, GELF.getGELFType(gelfMessage));
    }

    /**
     * Test getGELFType method with an invalid encryption
     */
    @Test(expected=InvalidGELFCompressionMethodException.class)
    public void testGetGELFTypeWithInvalidEncryption() throws Exception {
        // Build a datagram packet.
        String invalidEncryptedString = "mamamamamamamamamamamama";
        DatagramPacket invalidGELFMessage = new DatagramPacket(invalidEncryptedString.getBytes(), invalidEncryptedString.getBytes().length);

        // Cause the exception we expect.
        GELF.getGELFType(invalidGELFMessage);
    }

}