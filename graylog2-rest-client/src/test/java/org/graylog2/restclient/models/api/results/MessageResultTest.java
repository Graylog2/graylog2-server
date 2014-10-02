package org.graylog2.restclient.models.api.results;

import com.google.common.collect.ImmutableMap;
import org.graylog2.restclient.models.FieldMapper;
import org.graylog2.restclient.models.api.responses.HighlightRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class MessageResultTest {

    private static final DateTime NOW = DateTime.now(DateTimeZone.UTC);

    @Test
    public void testTimestampAsDouble() {
        final ImmutableMap<String, Object> message = ImmutableMap.of(
                "_id", "dead-beef-cafe-babe",
                "timestamp", NOW.getMillis() / 1000.0d,
                "gl2_source_node", "localhost",
                "gl2_source_input", "Test",
                "streams", Collections.<String>emptyList()
        );
        final MessageResult result = new MessageResult(message, "test_index",
                Collections.<String, List<HighlightRange>>emptyMap(), new FieldMapper());

        assertEquals(result.getTimestamp(), NOW);
    }

    @Test
    public void testTimestampAsInteger() {
        final ImmutableMap<String, Object> message = ImmutableMap.of(
                "_id", "dead-beef-cafe-babe",
                "timestamp", NOW.getMillis() / 1000,
                "gl2_source_node", "localhost",
                "gl2_source_input", "Test",
                "streams", Collections.<String>emptyList()
        );
        final MessageResult result = new MessageResult(message, "test_index",
                Collections.<String, List<HighlightRange>>emptyMap(), new FieldMapper());

        assertEquals(result.getTimestamp(), NOW.withMillisOfSecond(0));
    }

    @Test
    public void testTimestampAsLong() {
        final ImmutableMap<String, Object> message = ImmutableMap.of(
                "_id", "dead-beef-cafe-babe",
                "timestamp", NOW.getMillis() / 1000l,
                "gl2_source_node", "localhost",
                "gl2_source_input", "Test",
                "streams", Collections.<String>emptyList()
        );
        final MessageResult result = new MessageResult(message, "test_index",
                Collections.<String, List<HighlightRange>>emptyMap(), new FieldMapper());

        assertEquals(result.getTimestamp(), NOW.withMillisOfSecond(0));
    }
}