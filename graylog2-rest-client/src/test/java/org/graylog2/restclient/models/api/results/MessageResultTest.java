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