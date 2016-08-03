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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.eaio.uuid.UUID;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.collect.Sets.symmetricDifference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageTest {
    private Message message;
    private DateTime originalTimestamp;
    private MetricRegistry metricRegistry;
    private Meter invalidTimestampMeter;

    @Before
    public void setUp() {
        metricRegistry = new MetricRegistry();
        originalTimestamp = Tools.nowUTC();
        message = new Message("foo", "bar", originalTimestamp);
        invalidTimestampMeter = metricRegistry.meter("test");
    }

    @Test
    public void testAddFieldDoesOnlyAcceptAlphanumericKeys() throws Exception {
        Message m = new Message("foo", "bar", Tools.nowUTC());
        m.addField("some_thing", "bar");
        assertEquals("bar", m.getField("some_thing"));

        m = new Message("foo", "bar", Tools.nowUTC());
        m.addField("some-thing", "bar");
        assertEquals("bar", m.getField("some-thing"));

        m = new Message("foo", "bar", Tools.nowUTC());
        m.addField("somethin$g", "bar");
        assertNull(m.getField("somethin$g"));

        m = new Message("foo", "bar", Tools.nowUTC());
        m.addField("someäthing", "bar");
        assertNull(m.getField("someäthing"));
    }

    @Test
    public void testAddFieldTrimsValue() throws Exception {
        Message m = new Message("foo", "bar", Tools.nowUTC());
        m.addField("something", " bar ");
        assertEquals("bar", m.getField("something"));

        m.addField("something2", " bar");
        assertEquals("bar", m.getField("something2"));

        m.addField("something3", "bar ");
        assertEquals("bar", m.getField("something3"));
    }

    @Test
    public void testConstructorsTrimValues() throws Exception {
        final Map<String, Object> messageFields = ImmutableMap.of(
                Message.FIELD_ID, new UUID().toString(),
                Message.FIELD_MESSAGE, " foo ",
                Message.FIELD_SOURCE, " bar ",
                "something", " awesome ",
                "something_else", " "
        );

        Message m = new Message((String) messageFields.get(Message.FIELD_MESSAGE), (String) messageFields.get(Message.FIELD_SOURCE), Tools.nowUTC());
        assertEquals("foo", m.getMessage());
        assertEquals("bar", m.getSource());

        Message m2 = new Message(messageFields);
        assertEquals("foo", m2.getMessage());
        assertEquals("bar", m2.getSource());
        assertEquals("awesome", m2.getField("something"));
        assertNull(m2.getField("something_else"));
    }

    @Test
    public void testAddFieldWorksWithIntegers() throws Exception {
        Message m = new Message("foo", "bar", Tools.nowUTC());
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

    @Test(expected = ClassCastException.class)
    public void testGetFieldAsWithIncompatibleCast() throws Exception {
        message.addField("fields", Lists.newArrayList("hello"));
        message.getFieldAs(Map.class, "fields");
    }

    @Test
    public void testSetAndGetStreams() throws Exception {
        final Stream stream1 = mock(Stream.class);
        final Stream stream2 = mock(Stream.class);

        message.addStreams(Lists.newArrayList(stream2, stream1));

        // make sure all streams we've added are being returned. Internally it's a set, so don't check the order, it doesn't matter anyway.
        assertThat(message.getStreams()).containsOnly(stream1, stream2);
    }

    @Test
    public void testStreamMutators() {
        final Stream stream1 = mock(Stream.class);
        final Stream stream2 = mock(Stream.class);
        final Stream stream3 = mock(Stream.class);

        assertThat(message.getStreams()).isNotNull();
        assertThat(message.getStreams()).isEmpty();

        message.addStream(stream1);

        final Set<Stream> onlyWithStream1 = message.getStreams();
        assertThat(onlyWithStream1).containsOnly(stream1);

        message.addStreams(Sets.newHashSet(stream3, stream2));
        assertThat(message.getStreams()).containsOnly(stream1, stream2, stream3);

        // getStreams is a copy and doesn't change after mutations
        assertThat(onlyWithStream1).containsOnly(stream1);

        // stream2 was assigned
        assertThat(message.removeStream(stream2)).isTrue();
        // streams2 is no longer assigned
        assertThat(message.removeStream(stream2)).isFalse();
        assertThat(message.getStreams()).containsOnly(stream1, stream3);
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
    public void testGetId() throws Exception {
        final Pattern pattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        assertTrue(pattern.matcher(message.getId()).matches());
    }

    @Test
    public void testGetTimestamp() {
        try {
            final DateTime timestamp = message.getTimestamp();
            assertNotNull(timestamp);
            assertEquals(originalTimestamp.getZone(), timestamp.getZone());
        } catch (ClassCastException e) {
            fail("timestamp wasn't a DateTime " + e.getMessage());
        }
    }

    @Test
    public void testTimestampAsDate() {
        final DateTime dateTime = new DateTime(2015, 9, 8, 0, 0, DateTimeZone.UTC);

        message.addField(Message.FIELD_TIMESTAMP,
                         dateTime.toDate());

        final Map<String, Object> elasticSearchObject = message.toElasticSearchObject(invalidTimestampMeter);
        final Object esTimestampFormatted = elasticSearchObject.get(Message.FIELD_TIMESTAMP);

        assertEquals("Setting message timestamp as java.util.Date results in correct format for elasticsearch",
                     Tools.buildElasticSearchTimeFormat(dateTime), esTimestampFormatted);
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
        assertTrue(Message.validKey("foo@bar"));
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

        final Map<String, Object> object = message.toElasticSearchObject(invalidTimestampMeter);

        assertEquals("foo", object.get("message"));
        assertEquals("bar", object.get("source"));
        assertEquals("wat", object.get("field1"));
        assertEquals("that", object.get("field2"));
        assertEquals(Tools.buildElasticSearchTimeFormat((DateTime) message.getField("timestamp")), object.get("timestamp"));
        assertEquals(Collections.emptyList(), object.get("streams"));
    }

    @Test
    public void testToElasticSearchObjectWithInvalidKey() throws Exception {
        message.addField("field.3", "dot");

        final Map<String, Object> object = message.toElasticSearchObject(invalidTimestampMeter);

        // Elasticsearch >=2.0 does not allow "." in keys. Make sure we replace them before writing the message.
        assertEquals("#toElasticsearchObject() should replace \".\" in keys with a \"_\"",
                "dot", object.get("field_3"));

        assertEquals("foo", object.get("message"));
        assertEquals("bar", object.get("source"));
        assertEquals(Tools.buildElasticSearchTimeFormat((DateTime) message.getField("timestamp")), object.get("timestamp"));
        assertEquals(Collections.emptyList(), object.get("streams"));
    }

    @Test
    public void testToElasticSearchObjectWithoutDateTimeTimestamp() throws Exception {
        message.addField("timestamp", "time!");

        final Meter errorMeter = metricRegistry.meter("test-meter");
        final Map<String, Object> object = message.toElasticSearchObject(errorMeter);

        assertNotEquals("time!", object.get("timestamp"));
        assertEquals(1, errorMeter.getCount());
    }

    @Test
    public void testToElasticSearchObjectWithStreams() throws Exception {
        final Stream stream = mock(Stream.class);

        when(stream.getId()).thenReturn("stream-id");

        message.setStreams(Lists.newArrayList(stream));

        final Map<String, Object> object = message.toElasticSearchObject(invalidTimestampMeter);

        assertEquals(Lists.newArrayList("stream-id"), object.get("streams"));
    }

    @Test
    public void testIsComplete() throws Exception {
        Message message = new Message("message", "source", Tools.nowUTC());
        assertTrue(message.isComplete());

        message = new Message("message", "", Tools.nowUTC());
        assertTrue(message.isComplete());

        message = new Message("message", null, Tools.nowUTC());
        assertTrue(message.isComplete());

        message = new Message("", "source", Tools.nowUTC());
        assertFalse(message.isComplete());

        message = new Message(null, "source", Tools.nowUTC());
        assertFalse(message.isComplete());
    }

    @Test
    public void testGetValidationErrorsWithEmptyMessage() throws Exception {
        final Message message = new Message("", "source", Tools.nowUTC());

        assertEquals("message is empty, ", message.getValidationErrors());
    }

    @Test
    public void testGetValidationErrorsWithNullMessage() throws Exception {
        final Message message = new Message(null, "source", Tools.nowUTC());

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

    @Test(expected = UnsupportedOperationException.class)
    public void testGetFieldsReturnsImmutableMap() throws Exception {
        final Map<String, Object> fields = message.getFields();

        fields.put("foo", "bar");
    }

    @Test
    public void testGetFieldNames() throws Exception {
        assertTrue("Missing fields in set!", symmetricDifference(message.getFieldNames(), Sets.newHashSet("_id", "timestamp", "source", "message")).isEmpty());

        message.addField("testfield", "testvalue");

        assertTrue("Missing fields in set!", symmetricDifference(message.getFieldNames(), Sets.newHashSet("_id", "timestamp", "source", "message", "testfield")).isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetFieldNamesReturnsUnmodifiableSet() throws Exception {
        final Set<String> fieldNames = message.getFieldNames();

        fieldNames.remove("_id");
    }

    @Test
    public void testHasField() throws Exception {
        assertFalse(message.hasField("__foo__"));

        message.addField("__foo__", "bar");

        assertTrue(message.hasField("__foo__"));
    }

    @Test
    public void testDateConvertedToDateTime() {
        final Message message = new Message("", "source", Tools.nowUTC());

        final Date dateObject = DateTime.parse("2010-07-30T16:03:25Z").toDate();
        message.addField(Message.FIELD_TIMESTAMP, dateObject);

        assertEquals(message.getTimestamp().toDate(), dateObject);
        assertEquals(message.getField(Message.FIELD_TIMESTAMP).getClass(), DateTime.class);
    }

    @Test
    public void getStreamIdsReturnsStreamsIdsIfFieldDoesNotExist() {
        final Message message = new Message("", "source", Tools.nowUTC());
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("test");
        message.addStream(stream);

        assertThat(message.getStreamIds()).containsOnly("test");

    }

    @Test
    public void getStreamIdsReturnsStreamsFieldContentsIfFieldDoesExist() {
        final Message message = new Message("", "source", Tools.nowUTC());
        final Stream stream = mock(Stream.class);
        message.addField("streams", Collections.singletonList("test2"));
        message.addStream(stream);

        assertThat(message.getStreamIds()).containsOnly("test2");

    }
}
