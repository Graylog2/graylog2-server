/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

public class MessageTest {
    private Message message;

    @BeforeMethod
    public void setUp() {
        message = new Message("foo", "bar", Tools.iso8601());
    }

    @Test
    public void testAddFieldDoesOnlyAcceptAlphanumericKeys() throws Exception {
        Message m = new Message("foo", "bar", Tools.iso8601());
        m.addField("some_thing", "bar");
        assertEquals("bar", m.getField("some_thing"));

        m = new Message("foo", "bar", Tools.iso8601());
        m.addField("some-thing", "bar");
        assertEquals("bar", m.getField("some-thing"));

        m = new Message("foo", "bar", Tools.iso8601());
        m.addField("somethin$g", "bar");
        assertNull(m.getField("somethin$g"));

        m = new Message("foo", "bar", Tools.iso8601());
        m.addField("someäthing", "bar");
        assertNull(m.getField("someäthing"));
    }

    @Test
    public void testAddFieldTrimsValue() throws Exception {
        Message m = new Message("foo", "bar", Tools.iso8601());
        m.addField("something", " bar ");
        assertEquals("bar", m.getField("something"));

        m.addField("something2", " bar");
        assertEquals("bar", m.getField("something2"));

        m.addField("something3", "bar ");
        assertEquals("bar", m.getField("something3"));
    }

    @Test
    public void testAddFieldWorksWithIntegers() throws Exception {
        Message m = new Message("foo", "bar", Tools.iso8601());
        m.addField("something", 3);
        assertEquals(3, m.getField("something"));
    }

    @Test
    public void testAddFields() throws Exception {
        final Map<String, Object> map = Maps.newHashMap();

        map.put("field1", "Foo");
        map.put("field2", 1);

        message.addFields(map);

        assertEquals("Foo", message.getField("field1"));
        assertEquals(1, message.getField("field2"));
    }

    @Test
    public void testAddStringFields() throws Exception {
        final Map<String, String> map = Maps.newHashMap();

        map.put("field1", "Foo");
        map.put("field2", "Bar");

        message.addStringFields(map);

        assertEquals("Foo", message.getField("field1"));
        assertEquals("Bar", message.getField("field2"));
    }

    @Test
    public void testAddLongFields() throws Exception {
        final Map<String, Long> map = Maps.newHashMap();

        map.put("field1", 10L);
        map.put("field2", 230L);

        message.addLongFields(map);

        assertEquals(10L, message.getField("field1"));
        assertEquals(230L, message.getField("field2"));
    }

    @Test
    public void testAddDoubleFields() throws Exception {
        final Map<String, Double> map = Maps.newHashMap();

        map.put("field1", 10.0d);
        map.put("field2", 230.2d);

        message.addDoubleFields(map);

        assertEquals(10.0d, message.getField("field1"));
        assertEquals(230.2d, message.getField("field2"));
    }

    @Test
    public void testRemoveField() throws Exception {
        message.addField("foo", "bar");

        message.removeField("foo");
        assertNull(message.getField("foo"));
    }

    @Test
    public void testRemoveFieldNotDeletingReservedFields() throws Exception {
        message.removeField("message");
        message.removeField("source");
        message.removeField("timestamp");

        assertNotNull(message.getField("message"));
        assertNotNull(message.getField("source"));
        assertNotNull(message.getField("timestamp"));
    }

    @Test
    public void testGetFieldAs() throws Exception {
        message.addField("fields", Lists.newArrayList("hello"));

        assertEquals(Lists.newArrayList("hello"), message.getFieldAs(List.class, "fields"));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testGetFieldAsWithIncompatibleCast() throws Exception {
        message.addField("fields", Lists.newArrayList("hello"));
        message.getFieldAs(Map.class, "fields");
    }

    @Test
    public void testSetAndGetStreams() throws Exception {
        final Stream stream1 = mock(Stream.class);
        final Stream stream2 = mock(Stream.class);

        message.setStreams(Lists.newArrayList(stream1, stream2));

        assertEquals(Lists.newArrayList(stream1, stream2), message.getStreams());
    }

    @Test
    public void testGetStreamIds() throws Exception {
        message.addField("streams", Lists.newArrayList("stream-id"));

        assertEquals(Lists.newArrayList("stream-id"), message.getStreamIds());
    }

    @Test
    public void testGetAndSetFilterOut() throws Exception {
        assertFalse(message.getFilterOut());

        message.setFilterOut(true);

        assertTrue(message.getFilterOut());

        message.setFilterOut(false);

        assertFalse(message.getFilterOut());
    }

    @Test
    public void testGetAndSetSourceInput() throws Exception {
        assertNull(message.getSourceInput());

        final MessageInput input = mock(MessageInput.class);

        message.setSourceInput(input);

        assertEquals(input, message.getSourceInput());
    }

    @Test
    public void testGetId() throws Exception {
        final Pattern pattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        assertTrue(pattern.matcher(message.getId()).matches());
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("foo", message.getMessage());
    }

    @Test
    public void testGetSource() throws Exception {
        assertEquals("bar", message.getSource());
    }

    @Test
    public void testValidKeys() throws Exception {
        assertTrue(Message.validKey("foo123"));
        assertTrue(Message.validKey("foo-bar123"));
        assertTrue(Message.validKey("foo_bar123"));
        assertTrue(Message.validKey("foo.bar123"));
        assertTrue(Message.validKey("123"));
        assertTrue(Message.validKey(""));

        assertFalse(Message.validKey("foo bar"));
        assertFalse(Message.validKey("foo+bar"));
        assertFalse(Message.validKey("foo$bar"));
        assertFalse(Message.validKey(" "));
    }

    @Test
    public void testToElasticSearchObject() throws Exception {
        message.addField("field1", "wat");
        message.addField("field2", "that");

        final Map<String, Object> object = message.toElasticSearchObject();

        assertEquals("foo", object.get("message"));
        assertEquals("bar", object.get("source"));
        assertEquals("wat", object.get("field1"));
        assertEquals("that", object.get("field2"));
        assertEquals(Tools.buildElasticSearchTimeFormat((DateTime) message.getField("timestamp")), object.get("timestamp"));
        assertEquals(Collections.EMPTY_LIST, object.get("streams"));
    }

    @Test
    public void testToElasticSearchObjectWithoutDateTimeTimestamp() throws Exception {
        message.addField("timestamp", "time!");

        final Map<String, Object> object = message.toElasticSearchObject();

        assertEquals("time!", object.get("timestamp"));
    }

    @Test
    public void testToElasticSearchObjectWithStreams() throws Exception {
        final Stream stream = mock(Stream.class);

        when(stream.getId()).thenReturn("stream-id");

        message.setStreams(Lists.newArrayList(stream));

        final Map<String, Object> object = message.toElasticSearchObject();

        assertEquals(Lists.newArrayList("stream-id"), object.get("streams"));
    }

    @Test
    public void testIsComplete() throws Exception {
        Message message = new Message("message", "source", Tools.iso8601());
        assertTrue(message.isComplete());

        message = new Message("message", "", Tools.iso8601());
        assertFalse(message.isComplete());

        message = new Message("message", null, Tools.iso8601());
        assertFalse(message.isComplete());

        message = new Message("", "source", Tools.iso8601());
        assertFalse(message.isComplete());

        message = new Message(null, "source", Tools.iso8601());
        assertFalse(message.isComplete());
    }

    @Test
    public void testGetValidationErrorsWithEmptyMessage() throws Exception {
        final Message message = new Message("", "source", Tools.iso8601());

        assertEquals("message is empty, ", message.getValidationErrors());
    }

    @Test
    public void testGetValidationErrorsWithNullMessage() throws Exception {
        final Message message = new Message(null, "source", Tools.iso8601());

        assertEquals("message is missing, ", message.getValidationErrors());
    }

    @Test
    public void testGetFields() throws Exception {
        final Map<String, Object> fields = message.getFields();

        assertEquals(message.getId(), fields.get("_id"));
        assertEquals(message.getMessage(), fields.get("message"));
        assertEquals(message.getSource(), fields.get("source"));
        assertEquals(message.getField("timestamp"), fields.get("timestamp"));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testGetFieldsReturnsImmutableMap() throws Exception {
        final Map<String, Object> fields = message.getFields();

        fields.put("foo", "bar");
    }
}
