/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(messageSummary.getStreamIds()).containsAll(STREAM_IDS);
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
