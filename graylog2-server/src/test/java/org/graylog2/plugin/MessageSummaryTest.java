/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
    public static final ImmutableList<String> STREAM_IDS = ImmutableList.of("stream1", "stream2");
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
        assertEquals(INDEX_NAME, messageSummary.getIndex());
    }

    @Test
    public void testGetId() throws Exception {
        assertEquals(message.getId(), messageSummary.getId());
    }

    @Test
    public void testGetSource() throws Exception {
        assertEquals(message.getSource(), messageSummary.getSource());
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals(message.getMessage(), messageSummary.getMessage());
    }

    @Test
    public void testGetTimestamp() throws Exception {
        assertEquals(message.getTimestamp(), messageSummary.getTimestamp());
    }

    @Test
    public void testGetStreamIds() throws Exception {
        assertThat(messageSummary.getStreamIds()).containsAll(STREAM_IDS);
    }

    @Test
    public void testGetFields() throws Exception {
        assertEquals(new HashMap<String, Object>(), messageSummary.getFields());

        message.addField("foo", "bar");

        assertEquals(ImmutableMap.of("foo", "bar"), messageSummary.getFields());
    }

    @Test
    public void testJSONSerialization() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final MapType valueType = mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);

        final Map<String, Object> map = mapper.readValue(mapper.writeValueAsBytes(messageSummary), valueType);

        assertEquals(Sets.newHashSet("id", "timestamp", "message", "index", "source", "streamIds", "fields"), map.keySet());
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

        assertEquals("bar", messageSummary.getField("foo"));
    }
}
