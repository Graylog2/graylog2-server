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
package org.graylog2.alerts.types;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.graylog2.Configuration;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.AlertConditionTest;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.MongoIndexRange;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class FieldContentValueAlertConditionTest extends AlertConditionTest {

    @Test
    public void testConstructor() throws Exception {
        final Map<String, Object> parameters = getParametersMap(0, "field", "value");

        final FieldContentValueAlertCondition condition = getCondition(parameters, alertConditionTitle);

        assertNotNull(condition);
        assertNotNull(condition.getDescription());
    }

    @Test
    public void testRunMatchingMessagesInStream() throws Exception {
        final ResultMessage searchHit = ResultMessage.parseFromSource("some_id", "graylog_test",
                Collections.singletonMap("message", "something is in here"));

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRange = MongoIndexRange.create("graylog_test", now.minusDays(1), now, now, 0);
        final Set<IndexRange> indexRanges = Sets.newHashSet(indexRange);
        final SearchResult searchResult = spy(new SearchResult(Collections.singletonList(searchHit),
            1L,
            indexRanges,
            "message:something",
            null,
            100L));
        when(searchResult.getTotalResults()).thenReturn(1L);
        when(searches.search(
            anyString(),
            anyString(),
            any(RelativeRange.class),
            anyInt(),
            anyInt(),
            any(Sorting.class)))
            .thenReturn(searchResult);
        final FieldContentValueAlertCondition condition = getCondition(getParametersMap(0, "message", "something"), "Alert Condition for testing");

        final AlertCondition.CheckResult result = condition.runCheck();

        assertTriggered(condition, result);
    }


    @Test
    public void testRunNoMatchingMessages() throws Exception {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRange = MongoIndexRange.create("graylog_test", now.minusDays(1), now, now, 0);
        final Set<IndexRange> indexRanges = Sets.newHashSet(indexRange);
        final SearchResult searchResult = spy(new SearchResult(Collections.emptyList(),
            0L,
            indexRanges,
            "message:something",
            null,
            100L));
        when(searches.search(
            anyString(),
            anyString(),
            any(RelativeRange.class),
            anyInt(),
            anyInt(),
            any(Sorting.class)))
            .thenReturn(searchResult);
        final FieldContentValueAlertCondition condition = getCondition(getParametersMap(0, "message", "something"), alertConditionTitle);

        final AlertCondition.CheckResult result = condition.runCheck();

        assertNotTriggered(result);
    }

    @Test
    public void testCorrectUsageOfRelativeRange() throws Exception {
        final Stream stream = mock(Stream.class);
        final Searches searches = mock(Searches.class);
        final Configuration configuration = mock(Configuration.class);
        final SearchResult searchResult = mock(SearchResult.class);
        final int alertCheckInterval = 42;
        final RelativeRange relativeRange = RelativeRange.create(alertCheckInterval);

        when(stream.getId()).thenReturn("stream-id");
        when(configuration.getAlertCheckInterval()).thenReturn(alertCheckInterval);

        when(searches.search(anyString(),
            anyString(),
            eq(relativeRange),
            anyInt(),
            anyInt(),
            any(Sorting.class))).thenReturn(searchResult);

        final FieldContentValueAlertCondition alertCondition = new FieldContentValueAlertCondition(searches, configuration, stream,
            null, DateTime.now(DateTimeZone.UTC), "mockuser", ImmutableMap.<String,Object>of("field", "test", "value", "test"), "Field Content Value Test COndition");

        final AbstractAlertCondition.CheckResult result = alertCondition.runCheck();
    }

    private FieldContentValueAlertCondition getCondition(Map<String, Object> parameters, String title) {
        return new FieldContentValueAlertCondition(
            searches,
            mock(Configuration.class),
            stream,
            CONDITION_ID,
            Tools.nowUTC(),
            STREAM_CREATOR,
            parameters,
            title);
    }

    private Map<String, Object> getParametersMap(Integer grace, String field, String value) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("grace", grace);
        parameters.put("field", field);
        parameters.put("value", value);

        return parameters;
    }

}
