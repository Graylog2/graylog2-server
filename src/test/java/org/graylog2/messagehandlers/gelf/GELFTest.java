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

import java.math.BigInteger;
import org.apache.commons.codec.binary.Hex;
import java.io.IOException;
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

        assertEquals(GELF.TYPE_ZLIB, GELF.getGELFType(gelfMessage.getData()));
    }

    /**
     * Test getGELFType method with a GZIP encrypted message
     */
    @Test
    public void testGetGELFTypeWithGZIPEncryption() throws Exception {
        // Build a datagram packet.
        DatagramPacket gelfMessage = GELFTestHelper.buildGZIPCompressedDatagramPacket(this.originalMessage);

        assertEquals(GELF.TYPE_GZIP, GELF.getGELFType(gelfMessage.getData()));
    }

    /**
     * Test getGELFType method with an invalid encryption
     */
    @Test(expected=InvalidGELFCompressionMethodException.class)
    public void testGetGELFTypeWithInvalidEncryption() throws Exception {
        // Build a datagram packet.
        String invalidlyEncryptedString = "mamamamamamamamamamamama";
        DatagramPacket invalidGELFMessage = new DatagramPacket(invalidlyEncryptedString.getBytes(), invalidlyEncryptedString.getBytes().length);

        // Cause the exception we expect.
        GELF.getGELFType(invalidGELFMessage.getData());
    }

    /**
     * Test of extractGELFHeader method, of class GELF.
     */
    @Test
    public void testExtractGELFHeader() throws IOException {

    }

    /**
     * Test of extractData method, of class GELF.
     */
    @Test
    public void testExtractData() throws Exception {
        // A GELF chunk header. Sequence 2 of 7.
        String header = "1e0fdf0fcb728fd5b73b0232ee2db47ca1e1e859725c7f0202631f8fcb6d4297c32a00020007";
        String foo = asHex("foo".getBytes());
        header = header + foo;
        byte[] headerHex = Hex.decodeHex(header.toCharArray());
        DatagramPacket msg = new DatagramPacket(headerHex, headerHex.length);

        assertEquals("foo", new String(GELF.extractData(msg)));
    }

    public static String asHex(byte[] buf) {
        String s = new BigInteger(1, buf).toString(16);
        return (s.length() % 2 == 0) ? s : "0" + s;
    }

}