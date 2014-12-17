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

package org.graylog2.streams;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.bson.types.ObjectId;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.matchers.StreamRuleMock;
import org.joda.time.DateTime;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class StreamRouterEngineTest {
    private MetricRegistry metricRegistry = new MetricRegistry();

    @Mock StreamFaultManager streamFaultManager;
    StreamMetrics streamMetrics = new StreamMetrics(new MetricRegistry());

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(streamFaultManager.getStreamProcessingTimeout()).thenReturn(50L);
    }

    private StreamRouterEngine newEngine(List<Stream> streams) {
        return new StreamRouterEngine(streams, Executors.newSingleThreadExecutor(), streamFaultManager, streamMetrics);
    }

    @Test
    public void testGetStreams() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));

        assertEquals(engine.getStreams(), Lists.newArrayList(stream));
    }

    @Test
    public void testPresenceMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.<StreamRule>newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));
        final Message message = getMessage();

        // Without testfield in the message.
        assertTrue(engine.match(message).isEmpty());

        // With field in the message.
        message.addField("testfield", "testvalue");

        assertEquals(engine.match(message), Lists.newArrayList(stream));
    }

    @Test
    public void testExactMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "testvalue",
                "type", StreamRuleType.EXACT.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.<StreamRule>newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));
        final Message message = getMessage();

        // With wrong value for field.
        message.addField("testfield", "no-testvalue");

        assertTrue(engine.match(message).isEmpty());

        // With matching value for field.
        message.addField("testfield", "testvalue");

        assertEquals(engine.match(message), Lists.newArrayList(stream));
    }

    @Test
    public void testGreaterMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "1",
                "type", StreamRuleType.GREATER.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.<StreamRule>newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));
        final Message message = getMessage();

        // With smaller value.
        message.addField("testfield", "1");

        assertTrue(engine.match(message).isEmpty());

        // With greater value.
        message.addField("testfield", "2");

        assertEquals(engine.match(message), Lists.newArrayList(stream));
    }

    @Test
    public void testSmallerMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "5",
                "type", StreamRuleType.SMALLER.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.<StreamRule>newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));
        final Message message = getMessage();

        // With bigger value.
        message.addField("testfield", "5");

        assertTrue(engine.match(message).isEmpty());

        // With smaller value.
        message.addField("testfield", "2");

        assertEquals(engine.match(message), Lists.newArrayList(stream));
    }

    @Test
    public void testRegexMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.<StreamRule>newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));
        final Message message = getMessage();

        // With non-matching value.
        message.addField("testfield", "notestvalue");

        assertTrue(engine.match(message).isEmpty());

        // With matching value.
        message.addField("testfield", "testvalue");

        assertEquals(engine.match(message), Lists.newArrayList(stream));
    }

    @Test
    public void testMultipleRulesMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.<StreamRule>newArrayList(rule1, rule2));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));

        // Without testfield1 and testfield2 in the message.
        final Message message1 = getMessage();

        assertTrue(engine.match(message1).isEmpty());

        // With testfield1 but no-matching testfield2 in the message.
        final Message message2 = getMessage();
        message2.addField("testfield1", "testvalue");
        message2.addField("testfield2", "no-testvalue");

        assertTrue(engine.match(message2).isEmpty());

        // With testfield1 and matching testfield2 in the message.
        final Message message3 = getMessage();
        message3.addField("testfield1", "testvalue");
        message3.addField("testfield2", "testvalue2");

        assertEquals(engine.match(message3), Lists.newArrayList(stream));
    }

    @Test
    public void testMultipleStreamsMatch() throws Exception {
        final StreamMock stream1 = getStreamMock("test1");
        final StreamMock stream2 = getStreamMock("test2");

        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream1.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream1.getId()
        ));
        final StreamRuleMock rule3 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield3",
                "value", "testvalue3",
                "type", StreamRuleType.EXACT.toInteger(),
                "stream_id", stream2.getId()
        ));

        stream1.setStreamRules(Lists.<StreamRule>newArrayList(rule1, rule2));
        stream2.setStreamRules(Lists.<StreamRule>newArrayList(rule3));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream1, stream2));

        // Without testfield1 and testfield2 in the message.
        final Message message1 = getMessage();

        assertTrue(engine.match(message1).isEmpty());

        // With testfield1 and matching testfield2 in the message.
        final Message message2 = getMessage();
        message2.addField("testfield1", "testvalue");
        message2.addField("testfield2", "testvalue2");

        assertEquals(engine.match(message2), Lists.newArrayList(stream1));

        // With testfield1, matching testfield2 and matching testfield3 in the message.
        final Message message3 = getMessage();
        message3.addField("testfield1", "testvalue");
        message3.addField("testfield2", "testvalue2");
        message3.addField("testfield3", "testvalue3");

        final List<Stream> match = engine.match(message3);

        assertTrue(match.contains(stream1));
        assertTrue(match.contains(stream2));
        assertEquals(match.size(), 2);

        // With matching testfield3 in the message.
        final Message message4 = getMessage();
        message4.addField("testfield3", "testvalue3");

        assertEquals(engine.match(message4), Lists.newArrayList(stream2));
    }

    @Test
    public void testInvertedRulesMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "value", "1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "inverted", true,
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.<StreamRule>newArrayList(rule1, rule2));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));

        // Without testfield1 and testfield2 in the message.
        final Message message1 = getMessage();

        assertTrue(engine.match(message1).isEmpty());

        // With testfield1 and testfield2 in the message.
        final Message message2 = getMessage();
        message2.addField("testfield1", "testvalue");
        message2.addField("testfield2", "testvalue");

        assertTrue(engine.match(message2).isEmpty());

        // With testfield1 and not testfield2 in the message.
        final Message message3 = getMessage();
        message3.addField("testfield1", "testvalue");

        assertEquals(engine.match(message3), Lists.newArrayList(stream));

        // With testfield2 in the message.
        final Message message4 = getMessage();
        message4.addField("testfield2", "testvalue");

        assertTrue(engine.match(message4).isEmpty());
    }

    @Test
    public void testTestMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.<StreamRule>newArrayList(rule1, rule2));

        final StreamRouterEngine engine = newEngine(Lists.<Stream>newArrayList(stream));


        // Without testfield1 and testfield2 in the message.
        final Message message1 = getMessage();

        final StreamRouterEngine.StreamTestMatch testMatch1 = engine.testMatch(message1).get(0);
        final Map<StreamRule, Boolean> matches1 = testMatch1.getMatches();

        assertFalse(testMatch1.isMatched());
        assertFalse(matches1.get(rule1));
        assertFalse(matches1.get(rule2));

        // With testfield1 but no-matching testfield2 in the message.
        final Message message2 = getMessage();
        message2.addField("testfield1", "testvalue");
        message2.addField("testfield2", "no-testvalue");

        final StreamRouterEngine.StreamTestMatch testMatch2 = engine.testMatch(message2).get(0);
        final Map<StreamRule, Boolean> matches2 = testMatch2.getMatches();

        assertFalse(testMatch2.isMatched());
        assertTrue(matches2.get(rule1));
        assertFalse(matches2.get(rule2));

        // With testfield1 and matching testfield2 in the message.
        final Message message3 = getMessage();
        message3.addField("testfield1", "testvalue");
        message3.addField("testfield2", "testvalue2");

        final StreamRouterEngine.StreamTestMatch testMatch3 = engine.testMatch(message3).get(0);
        final Map<StreamRule, Boolean> matches3 = testMatch3.getMatches();

        assertTrue(testMatch3.isMatched());
        assertTrue(matches3.get(rule1));
        assertTrue(matches3.get(rule2));
    }

    @Test
    public void testGetFingerprint() {
        final StreamMock stream1 = getStreamMock("test");
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream1.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream1.getId()
        ));

        stream1.setStreamRules(Lists.<StreamRule>newArrayList(rule1, rule2));

        final StreamMock stream2 = getStreamMock("test");
        final StreamRuleMock rule3 = new StreamRuleMock(ImmutableMap.<String, Object>of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream2.getId()
        ));

        stream2.setStreamRules(Lists.<StreamRule>newArrayList(rule3));

        final StreamRouterEngine engine1 = newEngine(Lists.<Stream>newArrayList(stream1));
        final StreamRouterEngine engine2 = newEngine(Lists.<Stream>newArrayList(stream1));
        final StreamRouterEngine engine3 = newEngine(Lists.<Stream>newArrayList(stream2));

        assertEquals(engine1.getFingerprint(), engine2.getFingerprint());
        assertNotEquals(engine1.getFingerprint(), engine3.getFingerprint());
    }

    private StreamMock getStreamMock(String title) {
        return new StreamMock(ImmutableMap.<String, Object>of("_id", new ObjectId(), "title", title));
    }

    private Message getMessage() {
        return new Message("test message", "localhost", new DateTime());
    }
}