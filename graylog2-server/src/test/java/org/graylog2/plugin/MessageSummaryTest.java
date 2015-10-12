/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.graylog2.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MessageSummaryTest {
    public static final List<String> STREAM_IDS = ImmutableList.of("stream1", "stream2");
    public static final String INDEX_NAME = "graylog2_3";

    private Message message;
    private MessageSummary messageSummary;

    @Before
    public void setUp() throws Exception {
        message = new Message("message", "source", DateTime.now(DateTimeZone.UTC));
        message.addField("streams", STREAM_IDS);
        messageSummary = new MessageSummary(INDEX_NAME, message);
    }

    @Test
    public void testGetIndex() throws Exception {
        assertEquals(messageSummary.getIndex(), INDEX_NAME);
    }

    @Test
    public void testGetId() throws Exception {
        assertEquals(messageSummary.getId(), message.getId());
    }

    @Test
    public void testGetSource() throws Exception {
        assertEquals(messageSummary.getSource(), message.getSource());
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals(messageSummary.getMessage(), message.getMessage());
    }

    @Test
    public void testGetTimestamp() throws Exception {
        assertEquals(messageSummary.getTimestamp(), message.getTimestamp());
    }

    @Test
    public void testGetStreamIds() throws Exception {
        assertEquals(messageSummary.getStreamIds(), STREAM_IDS);
    }

    @Test
    public void testGetFields() throws Exception {
        assertEquals(messageSummary.getFields(), new HashMap<String, Object>());

        message.addField("foo", "bar");

        assertEquals(messageSummary.getFields(), ImmutableMap.of("foo", "bar"));
    }

    @Test
    public void testJSONSerialization() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final MapType valueType = mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);

        final Map<String, Object> map = mapper.readValue(mapper.writeValueAsBytes(messageSummary), valueType);

        assertEquals(map.keySet(), Sets.newHashSet("id", "timestamp", "message", "index", "source", "streamIds", "fields"));
    }

    @Test
    public void testHasField() throws Exception {
        assertFalse(messageSummary.hasField("foo"));

        message.addField("foo", "bar");

        assertTrue(messageSummary.hasField("foo"));
    }

    @Test
    public void testGetField() throws Exception {
        assertNull(messageSummary.getField("foo"));

        message.addField("foo", "bar");

        assertEquals(messageSummary.getField("foo"), "bar");
    }
}
