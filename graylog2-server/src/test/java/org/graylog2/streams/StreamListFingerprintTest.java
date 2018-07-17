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

package org.graylog2.streams;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StreamListFingerprintTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    Stream stream1;
    @Mock
    Stream stream2;
    @Mock
    StreamRule streamRule1;
    @Mock
    StreamRule streamRule2;
    @Mock
    StreamRule streamRule3;
    @Mock
    Output output1;
    @Mock
    Output output2;

    private final String expectedEmptyFingerprint = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    @Before
    public void setUp() throws Exception {
        output1 = makeOutput(1, "output1");
        output2 = makeOutput(2, "output2");

        streamRule1 = makeStreamRule(1, "field1");
        streamRule2 = makeStreamRule(2, "field2");
        streamRule3 = makeStreamRule(3, "field3");

        stream1 = makeStream(1, "title1", new StreamRule[]{streamRule1, streamRule2}, new Output[]{output1, output2});
        stream2 = makeStream(2, "title2", new StreamRule[]{streamRule3}, new Output[]{output2, output1});
    }

    private static Stream makeStream(int id, String title, StreamRule[] rules, Output[] outputs) {
        final HashMap<String, Object> fields = Maps.newHashMap();
        fields.put(StreamImpl.FIELD_TITLE, title);
        return new StreamImpl(new ObjectId(String.format(Locale.ENGLISH, "%024d", id)), fields, Lists.newArrayList(rules), Sets.newHashSet(
                outputs), null);
    }

    private static StreamRule makeStreamRule(int id, String field) {
        final HashMap<String, Object> fields = Maps.newHashMap();
        fields.put(StreamRuleImpl.FIELD_FIELD, field);
        return new StreamRuleImpl(new ObjectId(String.format(Locale.ENGLISH, "%024d", id)), fields);
    }

    private static Output makeOutput(int id, String title) {
        return OutputImpl.create(
                String.format(Locale.ENGLISH, "%024d", id),
                title,
                "foo",
                "user1",
                Collections.<String, Object>emptyMap(),
                DateTime.parse("2015-01-01T00:00:00Z").toDate(),
                null);
    }

    @Test
    public void testGetFingerprint() throws Exception {
        final StreamListFingerprint fingerprint = new StreamListFingerprint(Lists.newArrayList(stream1, stream2));

        // The fingerprint depends on the hashCode of each stream and stream rule and might change if the underlying
        // implementation changed.
        assertEquals("d669c1037a49c956ef8f25033abc065c2fb259d4", fingerprint.getFingerprint());
    }

    @Test
    public void testIdenticalStreams() throws Exception {
        final StreamListFingerprint fingerprint1 = new StreamListFingerprint(Lists.newArrayList(stream1));
        final StreamListFingerprint fingerprint2 = new StreamListFingerprint(Lists.newArrayList(stream1));
        final StreamListFingerprint fingerprint3 = new StreamListFingerprint(Lists.newArrayList(stream2));

        assertEquals(fingerprint1.getFingerprint(), fingerprint2.getFingerprint());
        assertNotEquals(fingerprint1.getFingerprint(), fingerprint3.getFingerprint());
    }

    @Test
    public void testWithEmptyStreamList() throws Exception {
        final StreamListFingerprint fingerprint = new StreamListFingerprint(Lists.<Stream>newArrayList());

        assertEquals(expectedEmptyFingerprint, fingerprint.getFingerprint());
    }
}
