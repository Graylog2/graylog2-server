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
package org.graylog2.inputs.extractors;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.graylog2.ConfigurationException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternRegistry;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GrokExtractorTest {

    private List<GrokPattern> patternSet;

    @Before
    public void setUp() throws Exception {
        patternSet = new ArrayList<>();

        final GrokPattern baseNum = GrokPattern.create("BASE10NUM", "(?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))");
        final GrokPattern number = GrokPattern.create("NUMBER", "(?:%{BASE10NUM:UNWANTED})");
        final GrokPattern data = GrokPattern.create("GREEDY", ".*");
        final GrokPattern twoBaseNums = GrokPattern.create("TWOBASENUMS", "%{BASE10NUM} %{BASE10NUM}");
        final GrokPattern test1 = GrokPattern.create("TEST1", "test1");
        final GrokPattern test2 = GrokPattern.create("TEST2", "test2");
        final GrokPattern orPattern = GrokPattern.create("ORTEST", "(%{TEST1:test}|%{TEST2:test})");

        patternSet.add(baseNum);
        patternSet.add(number);
        patternSet.add(data);
        patternSet.add(twoBaseNums);
        patternSet.add(test1);
        patternSet.add(test2);
        patternSet.add(orPattern);

    }

    @Test
    public void testDatatypeExtraction() {
        final GrokExtractor extractor = makeExtractor("%{NUMBER:number;int}");

        final Extractor.Result[] results = extractor.run("199999");
        assertEquals("NUMBER is marked as UNWANTED and does not generate a field", 1, results.length);
        assertEquals(Integer.class, results[0].getValue().getClass());
        assertEquals(199999, results[0].getValue());
    }

    @Test
    public void testDateExtraction() {
        final GrokExtractor extractor = makeExtractor("%{GREEDY:timestamp;date;yyyy-MM-dd'T'HH:mm:ss.SSSX}");
        final Extractor.Result[] results = extractor.run("2015-07-31T10:05:36.773Z");
        assertEquals("ISO date is parsed", 1, results.length);
        Object value = results[0].getValue();
        assertTrue(value instanceof Instant);
        DateTime date = new DateTime(((Instant) value).toEpochMilli(), DateTimeZone.UTC);

        assertEquals(2015, date.getYear());
        assertEquals(7, date.getMonthOfYear());
        assertEquals(31, date.getDayOfMonth());
        assertEquals(10, date.getHourOfDay());
        assertEquals(5, date.getMinuteOfHour());
        assertEquals(36, date.getSecondOfMinute());
        assertEquals(773, date.getMillisOfSecond());
    }

    @Test
    public void testDateWithComma() {
        final GrokExtractor extractor = makeExtractor("%{GREEDY:timestamp;date;yyyy-MM-dd'T'HH:mm:ss,SSSX}");
        final Extractor.Result[] results = extractor.run("2015-07-31T10:05:36,773Z");
        assertEquals("ISO date is parsed", 1, results.length);
        Object value = results[0].getValue();
        assertTrue(value instanceof Instant);
        DateTime date = new DateTime(((Instant) value).toEpochMilli(), DateTimeZone.UTC);

        assertEquals(2015, date.getYear());
        assertEquals(7, date.getMonthOfYear());
        assertEquals(31, date.getDayOfMonth());
        assertEquals(10, date.getHourOfDay());
        assertEquals(5, date.getMinuteOfHour());
        assertEquals(36, date.getSecondOfMinute());
        assertEquals(773, date.getMillisOfSecond());
    }

    @Test
    public void testFlattenValue() {
        final Map<String, Object> config = new HashMap<>();

        final GrokExtractor extractor1 = makeExtractor("%{TWOBASENUMS}", config);

        /* Test flatten with a multiple non unique result [ 22, 23 ] */
        Extractor.Result[] result1 = extractor1.run("22 23");
        assertThat(result1)
                .hasSize(2)
                .contains(new Extractor.Result("22 23", "TWOBASENUMS", -1, -1),
                        new Extractor.Result(Arrays.asList("22", "23"), "BASE10NUM", -1, -1));

        /* Test flatten with a multiple but unique result [ 22, 22 ] */
        Extractor.Result[] result2 = extractor1.run("22 22");
        assertThat(result2)
                .hasSize(2)
                .contains(new Extractor.Result("22 22", "TWOBASENUMS", -1, -1),
                        new Extractor.Result("22", "BASE10NUM", -1, -1));
    }

    @Test
    public void testNamedCapturesOnly() {
        final Map<String, Object> config = new HashMap<>();

        final GrokPattern mynumber = GrokPattern.create("MYNUMBER", "(?:%{BASE10NUM})");

        patternSet.add(mynumber);

        config.put("named_captures_only", true);
        final GrokExtractor extractor1 = makeExtractor("%{MYNUMBER:num}", config);

        config.put("named_captures_only", true);
        final GrokExtractor extractor2 = makeExtractor("%{MYNUMBER:num;int}", config);

        config.put("named_captures_only", false);
        final GrokExtractor extractor3 = makeExtractor("%{MYNUMBER:num}", config);

        final GrokExtractor extractor4 = makeExtractor("%{MYNUMBER:num}");

        assertThat(extractor1.run("2015"))
                .hasSize(1)
                .containsOnly(new Extractor.Result("2015", "num", -1, -1));
        assertThat(extractor2.run("2015"))
                .hasSize(1)
                .containsOnly(new Extractor.Result(2015, "num", -1, -1));
        assertThat(extractor3.run("2015"))
                .hasSize(2)
                .containsOnly(
                        new Extractor.Result("2015", "num", -1, -1),
                        new Extractor.Result("2015", "BASE10NUM", -1, -1)
                );
        assertThat(extractor4.run("2015"))
                .hasSize(2)
                .containsOnly(
                        new Extractor.Result("2015", "num", -1, -1),
                        new Extractor.Result("2015", "BASE10NUM", -1, -1)
                );
    }

    @Test
    public void testIssue4773() {
        // See: https://github.com/Graylog2/graylog2-server/issues/4773
        final Map<String, Object> config = new HashMap<>();

        config.put("named_captures_only", true);

        // Using an OR with the same named capture should only return one value "2015" instead of "[2015, null]"
        final GrokExtractor extractor = makeExtractor("(%{BASE10NUM:num}|%{BASE10NUM:num})", config);

        assertThat(extractor.run("2015"))
                .hasSize(1)
                .containsOnly(
                        new Extractor.Result("2015", "num", -1, -1)
                );
    }

    @Test
    public void testIssue5563() {
        // See: https://github.com/Graylog2/graylog2-server/issues/5563
        //      https://github.com/Graylog2/graylog2-server/issues/5704
        final Map<String, Object> config = new HashMap<>();

        config.put("named_captures_only", true);

        patternSet.add(GrokPattern.create("YOLO", "(?<test_field>test)"));
        // Make sure that the user can use a capture name with an "_".
        final GrokExtractor extractor = makeExtractor("%{YOLO}", config);

        assertThat(extractor.run("test"))
                .hasSize(1)
                .containsOnly(
                        new Extractor.Result("test", "test_field", -1, -1)
                );
    }

    private GrokExtractor makeExtractor(String pattern) {
        return makeExtractor(pattern, new HashMap<>());
    }

    @SuppressForbidden("Allow using default thread factory")
    private GrokExtractor makeExtractor(String pattern, Map<String, Object> config) {
        config.put("grok_pattern", pattern);
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final EventBus clusterBus = new EventBus();
        final GrokPatternService grokPatternService = new InMemoryGrokPatternService(clusterEventBus);
        try {
            grokPatternService.saveAll(patternSet, true);
        } catch (Exception e) {
            fail("Could not save grok patter: " + e.getMessage());
        }
        final GrokPatternRegistry grokPatternRegistry = new GrokPatternRegistry(clusterBus, grokPatternService, Executors.newScheduledThreadPool(1));

        try {
            return new GrokExtractor(new LocalMetricRegistry(),
                                     grokPatternRegistry,
                                     "id",
                                     "title",
                                     0,
                                     Extractor.CursorStrategy.COPY,
                                     "message",
                                     "message",
                                     config,
                                     "admin",
                                     Lists.newArrayList(),
                                     Extractor.ConditionType.NONE,
                                     null);
        } catch (Extractor.ReservedFieldException | ConfigurationException e) {
            fail("Test setup is wrong: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}