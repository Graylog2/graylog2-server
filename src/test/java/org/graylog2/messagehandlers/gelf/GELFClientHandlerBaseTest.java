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

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * GELFClientHandlerBaseTest.java: Oct 18, 2010 7:07:26 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GELFClientHandlerBaseTest {

    private String originalMessage = "{\"short_message\":\"something.\",\"full_message\":\"lol!\",\"host\":\"somehost\",\"level\":2,\"file\":\"example.php\",\"line\":1337}";
    private String originalMessageWithAdditionalData = "{\"short_message\":\"something.\",\"full_message\":\"lol!\",\"host\":\"somehost\",\"level\":2,\"file\":\"example.php\",\"line\":1337,\"_a_s1\":\"yes\",\"_a_s2\":\"yes, really\"}";
    private String originalMessageWithIDField = "{\"short_message\":\"something.\",\"full_message\":\"lol!\",\"host\":\"somehost\",\"level\":2,\"file\":\"example.php\",\"line\":1337,\"_id\":\"foo\"}";
    private String originalMessageWithAdditionalDataThatHasInts = "{\"short_message\":\"something.\",\"full_message\":\"lol!\",\"host\":\"somehost\",\"level\":2,\"file\":\"example.php\",\"line\":1337,\"_a_s1\":\"yes\",\"_a_s2\":\"yes, really\",\"_a_i\":9001}";


    public GELFClientHandlerBaseTest() {
    }

    /**
     * Test parsing of standard GELF message with no additional fields.
     */
    @Test
    public void testParseWithoutAdditionalData() throws Exception {
        GELFClientHandlerBase instance = new GELFClientHandlerBase();
        instance.clientMessage = this.originalMessage;
        instance.parse();

        GELFMessage message = instance.message;

        // There should be not additional data.
        assertEquals(message.getAdditionalData().size(), 0);

        // Test standard fields.
        assertEquals("something.", message.getShortMessage());
        assertEquals("lol!", message.getFullMessage());
        assertEquals("somehost", message.getHost());
        assertEquals(2, message.getLevel());
        assertEquals("example.php", message.getFile());
        assertEquals(1337, message.getLine());
    }

    /**
     * Test parsing of standard GELF message with additional fields.
     */
    @Test
    public void testParseWithAdditionalData() throws Exception {
        GELFClientHandlerBase instance = new GELFClientHandlerBase();
        instance.clientMessage = this.originalMessageWithAdditionalData;
        instance.parse();

        GELFMessage message = instance.message;

        // There should be two additional data fields.
        assertEquals(2, message.getAdditionalData().size());

        // Test standard fields.
        assertEquals("something.", message.getShortMessage());
        assertEquals("lol!", message.getFullMessage());
        assertEquals("somehost", message.getHost());
        assertEquals(2, message.getLevel());
        assertEquals("example.php", message.getFile());
        assertEquals(1337, message.getLine());

        // Test additional fields.
        Map<String, Object> additionalData = message.getAdditionalData();
        assertEquals("yes", additionalData.get("_a_s1"));
        assertEquals("yes, really", additionalData.get("_a_s2"));
    }

    @Test
    public void testParseWithAdditionalDataAndDifferentDataTypes() throws Exception {
        GELFClientHandlerBase instance = new GELFClientHandlerBase();
        instance.clientMessage = this.originalMessageWithAdditionalDataThatHasInts;
        instance.parse();

        GELFMessage message = instance.message;

        // There should be two additional data fields.
        assertEquals(3, message.getAdditionalData().size());

        // Test standard fields.
        assertEquals("something.", message.getShortMessage());
        assertEquals("lol!", message.getFullMessage());
        assertEquals("somehost", message.getHost());
        assertEquals(2, message.getLevel());
        assertEquals("example.php", message.getFile());
        assertEquals(1337, message.getLine());

        // Test additional fields.
        long thatLong = 9001;
        Map<String, Object> additionalData = message.getAdditionalData();
        assertEquals("yes", additionalData.get("_a_s1"));
        assertEquals("yes, really", additionalData.get("_a_s2"));
        assertEquals(thatLong, additionalData.get("_a_i"));
    }

    @Test
    public void testIdFieldIsSkipped() throws Exception {
        GELFClientHandlerBase instance = new GELFClientHandlerBase();
        instance.clientMessage = this.originalMessageWithIDField;
        instance.parse();

        GELFMessage message = instance.message;

        assertFalse(message.getAdditionalData().containsKey("_id"));
        assertFalse(message.getAdditionalData().containsKey("id"));
    }

    /**
     * Test of getClientMessage method, of class GELFClientHandlerBase.
     */
    @Test
    public void testGetClientMessage() {
        GELFClientHandlerBase instance = new GELFClientHandlerBase();
        instance.clientMessage = this.originalMessage;
        assertEquals(this.originalMessage, instance.clientMessage);
    }

}