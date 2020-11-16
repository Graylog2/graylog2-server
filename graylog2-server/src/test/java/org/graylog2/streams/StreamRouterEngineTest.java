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
package org.graylog2.streams;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.bson.types.ObjectId;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.streams.matchers.StreamRuleMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StreamRouterEngineTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StreamFaultManager streamFaultManager;
    @Mock
    private Stream defaultStream;
    private Provider<Stream> defaultStreamProvider;

    private StreamMetrics streamMetrics;

    @Before
    public void setUp() throws Exception {
        defaultStreamProvider = () -> defaultStream;
        streamMetrics = new StreamMetrics(new MetricRegistry());
        when(streamFaultManager.getStreamProcessingTimeout()).thenReturn(250L);
    }

    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    private StreamRouterEngine newEngine(List<Stream> streams) {
        return new StreamRouterEngine(streams, Executors.newSingleThreadExecutor(), streamFaultManager, streamMetrics, defaultStreamProvider);
    }

    @Test
    public void testGetStreams() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));

        assertEquals(Lists.newArrayList(stream), engine.getStreams());
    }

    @Test
    public void testPresenceMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));
        final Message message = getMessage();

        // Without testfield in the message.
        assertTrue(engine.match(message).isEmpty());

        // With field in the message.
        message.addField("testfield", "testvalue");

        assertEquals(Lists.newArrayList(stream), engine.match(message));
    }

    @Test
    public void testRemoveFromAllMessages() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));
        stream.setRemoveMatchesFromDefaultStream(true);
        stream.setStreamRules(Collections.singletonList(rule));

        final StreamRouterEngine engine = newEngine(Collections.singletonList(stream));
        final Message message = getMessage();
        message.addStream(defaultStream);

        assertThat(message.getStreams()).containsExactly(defaultStream);

        // Without testfield in the message.
        assertThat(engine.match(message)).isEmpty();

        // With field in the message.
        message.addField("testfield", "testvalue");
        assertThat(engine.match(message)).containsExactly(stream);
        assertThat(message.getStreams()).doesNotContain(defaultStream);
    }

    @Test
    public void testExactMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "testvalue",
                "type", StreamRuleType.EXACT.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));
        final Message message = getMessage();

        // With wrong value for field.
        message.addField("testfield", "no-testvalue");

        assertTrue(engine.match(message).isEmpty());

        // With matching value for field.
        message.addField("testfield", "testvalue");

        assertEquals(Lists.newArrayList(stream), engine.match(message));
    }

    @Test
    public void testContainsMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "testvalue",
                "type", StreamRuleType.CONTAINS.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));
        final Message message = getMessage();

        // Without the field
        assertTrue(engine.match(message).isEmpty());

        // With wrong value for field.
        message.addField("testfield", "no-foobar");

        assertTrue(engine.match(message).isEmpty());

        // With matching value for field.
        message.addField("testfield", "hello testvalue");

        assertEquals(Lists.newArrayList(stream), engine.match(message));
    }

    @Test
    public void testInvertedContainsMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(
            ImmutableMap.<String, Object>builder()
                .put("_id", new ObjectId())
                .put("field", "testfield")
                .put("inverted", true)
                .put("value", "testvalue")
                .put("type", StreamRuleType.CONTAINS.toInteger())
                .put("stream_id", stream.getId())
                .build()
        );

        stream.setStreamRules(Lists.newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));
        final Message message = getMessage();

        // Without the field
        assertEquals(Lists.newArrayList(stream), engine.match(message));

        // Without the matching value in the field
        message.addField("testfield", "no-foobar");

        assertEquals(Lists.newArrayList(stream), engine.match(message));

        // With matching value in the field.
        message.addField("testfield", "hello testvalue");

        assertTrue(engine.match(message).isEmpty());
    }

    @Test
    public void testGreaterMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "1",
                "type", StreamRuleType.GREATER.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));
        final Message message = getMessage();

        // With smaller value.
        message.addField("testfield", "1");

        assertTrue(engine.match(message).isEmpty());

        // With greater value.
        message.addField("testfield", "2");

        assertEquals(Lists.newArrayList(stream), engine.match(message));
    }

    @Test
    public void testSmallerMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "5",
                "type", StreamRuleType.SMALLER.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));
        final Message message = getMessage();

        // With bigger value.
        message.addField("testfield", "5");

        assertTrue(engine.match(message).isEmpty());

        // With smaller value.
        message.addField("testfield", "2");

        assertEquals(Lists.newArrayList(stream), engine.match(message));
    }

    @Test
    public void testRegexMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));
        final Message message = getMessage();

        // With non-matching value.
        message.addField("testfield", "notestvalue");

        assertTrue(engine.match(message).isEmpty());

        // With matching value.
        message.addField("testfield", "testvalue");

        assertEquals(Lists.newArrayList(stream), engine.match(message));
    }

    @Test
    public void testMultipleRulesMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule1, rule2));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));

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

        assertEquals(Lists.newArrayList(stream), engine.match(message3));
    }

    @Test
    public void testMultipleStreamsMatch() throws Exception {
        final StreamMock stream1 = getStreamMock("test1");
        final StreamMock stream2 = getStreamMock("test2");

        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream1.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream1.getId()
        ));
        final StreamRuleMock rule3 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield3",
                "value", "testvalue3",
                "type", StreamRuleType.EXACT.toInteger(),
                "stream_id", stream2.getId()
        ));

        stream1.setStreamRules(Lists.newArrayList(rule1, rule2));
        stream2.setStreamRules(Lists.newArrayList(rule3));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream1, stream2));

        // Without testfield1 and testfield2 in the message.
        final Message message1 = getMessage();

        assertTrue(engine.match(message1).isEmpty());

        // With testfield1 and matching testfield2 in the message.
        final Message message2 = getMessage();
        message2.addField("testfield1", "testvalue");
        message2.addField("testfield2", "testvalue2");

        assertEquals(Lists.newArrayList(stream1), engine.match(message2));

        // With testfield1, matching testfield2 and matching testfield3 in the message.
        final Message message3 = getMessage();
        message3.addField("testfield1", "testvalue");
        message3.addField("testfield2", "testvalue2");
        message3.addField("testfield3", "testvalue3");

        final List<Stream> match = engine.match(message3);

        assertTrue(match.contains(stream1));
        assertTrue(match.contains(stream2));
        assertEquals(2, match.size());

        // With matching testfield3 in the message.
        final Message message4 = getMessage();
        message4.addField("testfield3", "testvalue3");

        assertEquals(Lists.newArrayList(stream2), engine.match(message4));
    }

    @Test
    public void testInvertedRulesMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "value", "1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "inverted", true,
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule1, rule2));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));

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

        assertEquals(Lists.newArrayList(stream), engine.match(message3));

        // With testfield2 in the message.
        final Message message4 = getMessage();
        message4.addField("testfield2", "testvalue");

        assertTrue(engine.match(message4).isEmpty());
    }

    @Test
    public void testTestMatch() throws Exception {
        final StreamMock stream = getStreamMock("test");
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule1, rule2));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));


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
    public void testOrTestMatch() throws Exception {
        final StreamMock stream = getStreamMock("test", Stream.MatchingType.OR);
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream.getId()
        ));

        stream.setStreamRules(Lists.newArrayList(rule1, rule2));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));


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

        assertTrue(testMatch2.isMatched());
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
        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield1",
                "type", StreamRuleType.PRESENCE.toInteger(),
                "stream_id", stream1.getId()
        ));
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield2",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream1.getId()
        ));

        stream1.setStreamRules(Lists.newArrayList(rule1, rule2));

        final StreamMock stream2 = getStreamMock("test");
        final StreamRuleMock rule3 = new StreamRuleMock(ImmutableMap.of(
                "_id", new ObjectId(),
                "field", "testfield",
                "value", "^test",
                "type", StreamRuleType.REGEX.toInteger(),
                "stream_id", stream2.getId()
        ));

        stream2.setStreamRules(Lists.newArrayList(rule3));

        final StreamRouterEngine engine1 = newEngine(Lists.newArrayList(stream1));
        final StreamRouterEngine engine2 = newEngine(Lists.newArrayList(stream1));
        final StreamRouterEngine engine3 = newEngine(Lists.newArrayList(stream2));

        assertEquals(engine1.getFingerprint(), engine2.getFingerprint());
        assertNotEquals(engine1.getFingerprint(), engine3.getFingerprint());
    }

    @Test
    public void testOrMatching() {
        final String dummyField = "dummyField";
        final String dummyValue = "dummyValue";

        final Stream stream = mock(Stream.class);
        when(stream.getMatchingType()).thenReturn(Stream.MatchingType.OR);
        final StreamRule streamRule1 = getStreamRuleMock("StreamRule1Id", StreamRuleType.EXACT, dummyField, dummyValue);
        final StreamRule streamRule2 = getStreamRuleMock("StreamRule2Id", StreamRuleType.EXACT, dummyField, "not" + dummyValue);

        when(stream.getStreamRules()).thenReturn(Lists.newArrayList(streamRule1, streamRule2));

        final Message message = mock(Message.class);
        when(message.getField(eq(dummyField))).thenReturn(dummyValue);

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));

        final List<Stream> result = engine.match(message);

        assertThat(result).hasSize(1);
        assertThat(result).contains(stream);
    }

    @Test
    public void testOrMatchingShouldNotMatch() {
        final String dummyField = "dummyField";
        final String dummyValue = "dummyValue";

        final Stream stream = mock(Stream.class);
        when(stream.getMatchingType()).thenReturn(Stream.MatchingType.OR);
        final StreamRule streamRule1 = getStreamRuleMock("StreamRule1Id", StreamRuleType.EXACT, dummyField, "not" + dummyValue);
        final StreamRule streamRule2 = getStreamRuleMock("StreamRule2Id", StreamRuleType.EXACT, dummyField, "alsoNot" + dummyValue);

        when(stream.getStreamRules()).thenReturn(Lists.newArrayList(streamRule1, streamRule2));

        final Message message = mock(Message.class);
        when(message.getField(eq(dummyField))).thenReturn(dummyValue);

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));

        final List<Stream> result = engine.match(message);

        assertThat(result).isEmpty();
    }

    @Test
    public void testMultipleStreamWithDifferentMatching() {
        final String dummyField = "dummyField";
        final String dummyValue = "dummyValue";

        final StreamRule streamRule1 = getStreamRuleMock("StreamRule1Id", StreamRuleType.EXACT, dummyField, dummyValue);
        final StreamRule streamRule2 = getStreamRuleMock("StreamRule2Id", StreamRuleType.EXACT, dummyField, "not" + dummyValue);

        final Stream stream1 = mock(Stream.class);
        when(stream1.getId()).thenReturn("Stream1Id");
        when(stream1.getMatchingType()).thenReturn(Stream.MatchingType.OR);
        when(stream1.getStreamRules()).thenReturn(Lists.newArrayList(streamRule1, streamRule2));

        final Stream stream2 = mock(Stream.class);
        when(stream2.getId()).thenReturn("Stream2Id");
        when(stream2.getMatchingType()).thenReturn(Stream.MatchingType.AND);
        when(stream2.getStreamRules()).thenReturn(Lists.newArrayList(streamRule1, streamRule2));

        final Message message = mock(Message.class);
        when(message.getField(eq(dummyField))).thenReturn(dummyValue);

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream1, stream2));

        final List<Stream> result = engine.match(message);

        assertThat(result).hasSize(1);
        assertThat(result).contains(stream1);
        assertThat(result).doesNotContain(stream2);
    }

    @Test
    public void testAndStreamWithMultipleRules() {
        final String dummyField = "dummyField";
        final String dummyValue = "dummyValue";

        final StreamRule streamRule1 = getStreamRuleMock("StreamRule1Id", StreamRuleType.EXACT, dummyField, dummyValue);
        final StreamRule streamRule2 = getStreamRuleMock("StreamRule2Id", StreamRuleType.EXACT, dummyField, dummyValue);

        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("Stream1Id");
        when(stream.getMatchingType()).thenReturn(Stream.MatchingType.OR);
        when(stream.getStreamRules()).thenReturn(Lists.newArrayList(streamRule1, streamRule2));

        final Message message = mock(Message.class);
        when(message.getField(eq(dummyField))).thenReturn(dummyValue);

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));

        final List<Stream> result = engine.match(message);

        assertThat(result).hasSize(1);
        assertThat(result).contains(stream);
    }

    @Test
    public void testEmptyStreamRulesNonMatch() {
        final Stream stream = mock(Stream.class);
        when(stream.getStreamRules()).thenReturn(Collections.emptyList());

        final Message message = mock(Message.class);

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));

        final List<Stream> result = engine.match(message);

        assertThat(result).isEmpty();
        assertThat(result).doesNotContain(stream);
    }

    @Test
    public void issue1396() throws Exception {
        final StreamMock stream = getStreamMock("GitHub issue #1396");
        stream.setMatchingType(Stream.MatchingType.AND);

        final StreamRuleMock rule1 = new StreamRuleMock(ImmutableMap.<String, Object>builder()
                .put("_id", new ObjectId())
                .put("field", "custom1")
                .put("value", "value1")
                .put("type", StreamRuleType.EXACT.toInteger())
                .put("inverted", false)
                .put("stream_id", stream.getId())
                .build()
        );
        final StreamRuleMock rule2 = new StreamRuleMock(ImmutableMap.<String, Object>builder()
                .put("_id", new ObjectId())
                .put("field", "custom2")
                .put("value", "value2")
                .put("type", StreamRuleType.EXACT.toInteger())
                .put("inverted", false)
                .put("stream_id", stream.getId())
                .build()
        );

        stream.setStreamRules(Lists.newArrayList(rule1, rule2));

        final StreamRouterEngine engine = newEngine(Lists.newArrayList(stream));

        final Message message1 = getMessage();
        message1.addFields(ImmutableMap.of("custom1", "value1"));

        assertTrue("Message without \"custom2\" should not match conditions", engine.match(message1).isEmpty());

        final Message message2 = getMessage();
        message2.addFields(ImmutableMap.of(
                        "custom1", "value1",
                        "custom2", "value2"
                )
        );

        assertEquals("Message with \"custom1\" and \"custom2\" should match conditions",
                Lists.newArrayList(stream), engine.match(message2));
    }

    private StreamMock getStreamMock(String title) {
        return getStreamMock(title, Stream.MatchingType.AND);
    }

    private StreamMock getStreamMock(String title, Stream.MatchingType matchingType) {
        return new StreamMock(ImmutableMap.of("_id", new ObjectId(), "title", title, "matching_type", matchingType));
    }

    private StreamRule getStreamRuleMock(String id, StreamRuleType type, String field, String value) {
        final StreamRule result = mock(StreamRule.class);
        when(result.getId()).thenReturn(id);
        when(result.getType()).thenReturn(type);
        when(result.getField()).thenReturn(field);
        when(result.getValue()).thenReturn(value);

        return result;
    }

    private Message getMessage() {
        return new Message("test message", "localhost", new DateTime(DateTimeZone.UTC));
    }
}
