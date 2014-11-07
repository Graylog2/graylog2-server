/**
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
 */
package org.graylog2.restclient.models.api.results;

import com.google.common.collect.ImmutableMap;
import org.graylog2.restclient.lib.DateTools;
import org.graylog2.restclient.models.FieldMapper;
import org.graylog2.restclient.models.api.responses.HighlightRange;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class MessageResultTest {

    private static final DateTime NOW = DateTools.nowInUTC();

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