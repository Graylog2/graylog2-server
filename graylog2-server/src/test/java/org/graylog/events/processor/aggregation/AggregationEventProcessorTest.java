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
package org.graylog.events.processor.aggregation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventFactory;
import org.graylog.events.event.TestEvent;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventProcessorDependencyCheck;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventProcessorPreconditionException;
import org.graylog.events.processor.EventStreamService;
import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamMock;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AggregationEventProcessorTest {
    public static final int SEARCH_WINDOW_MS = 30000;
    private static final String QUERY_STRING = "aQueryString";
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AggregationSearch.Factory searchFactory;
    @Mock
    private MoreSearch moreSearch;
    @Mock
    private EventFactory eventFactory;
    @Mock
    private DBEventProcessorStateService stateService;
    @Mock
    private EventProcessorDependencyCheck eventProcessorDependencyCheck;
    @Mock
    private Messages messages;
    @Mock
    private Consumer<List<MessageSummary>> messageConsumer;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StreamService streamService;

    private PermittedStreams permittedStreams;
    private EventStreamService eventStreamService;
    private final MessageFactory messageFactory = new TestMessageFactory();

    @Before
    public void setUp() throws Exception {
        when(streamService.loadAll()).thenReturn(ImmutableList.of(
                new StreamMock(Collections.singletonMap("_id", "stream-1"), emptyList()),
                new StreamMock(Collections.singletonMap("_id", "stream-2"), emptyList()),
                new StreamMock(Collections.singletonMap("_id", "stream-3"), emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_STREAM_ID), emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_EVENTS_STREAM_ID), emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_SYSTEM_EVENTS_STREAM_ID), emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.FAILURES_STREAM_ID), emptyList())
        ));

        eventStreamService = new EventStreamService(streamService);
        permittedStreams = new PermittedStreams(streamService);
    }
    @Test
    public void createEventsWithFilter() throws Exception {
        when(eventProcessorDependencyCheck.hasMessagesIndexedUpTo(any(TimeRange.class))).thenReturn(true);

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.minusHours(1).plusMillis(SEARCH_WINDOW_MS));

        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query(QUERY_STRING)
                .streams(ImmutableSet.of())
                .groupBy(ImmutableList.of())
                .series(ImmutableList.of())
                .conditions(null)
                .searchWithinMs(SEARCH_WINDOW_MS)
                .executeEveryMs(SEARCH_WINDOW_MS)
                .build();
        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), null, emptyList());
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(eventDefinitionDto, searchFactory,
                eventProcessorDependencyCheck, stateService, moreSearch, eventStreamService, messages, permittedStreams, Set.of(), messageFactory);

        assertThatCode(() -> eventProcessor.createEvents(eventFactory, parameters, (events) -> {})).doesNotThrowAnyException();

        verify(moreSearch, times(1)).scrollQuery(
                eq(config.query()),
                eq(ImmutableSet.of("stream-3", "stream-2", "stream-1", "000000000000000000000001")),
                eq(emptyList()),
                eq(config.queryParameters()),
                eq(parameters.timerange()),
                eq(parameters.batchSize()),
                any(MoreSearch.ScrollCallback.class)
        );
        verify(searchFactory, never()).create(eq(config), eq(parameters), any(AggregationSearch.User.class), eq(eventDefinitionDto), eq(List.of()));
    }

    @Test
    public void createEventsWithoutRequiredMessagesBeingIndexed() throws Exception {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.plusHours(1));

        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query(QUERY_STRING)
                .streams(ImmutableSet.of())
                .groupBy(ImmutableList.of())
                .series(ImmutableList.of())
                .conditions(null)
                .searchWithinMs(30000)
                .executeEveryMs(30000)
                .build();
        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), null, emptyList());
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(eventDefinitionDto, searchFactory,
                eventProcessorDependencyCheck, stateService, moreSearch, eventStreamService, messages, permittedStreams, Set.of(), messageFactory);

        // If the dependency check returns true, there should be no exception raised and the state service should be called
        when(eventProcessorDependencyCheck.hasMessagesIndexedUpTo(timerange)).thenReturn(true);

        assertThatCode(() -> eventProcessor.createEvents(eventFactory, parameters, (events) -> {})).doesNotThrowAnyException();

        verify(stateService, times(1)).setState("dto-id-1", timerange.from(), timerange.to());
        verify(moreSearch, times(1)).scrollQuery(
                eq(config.query()),
                eq(ImmutableSet.of("stream-3", "stream-2", "stream-1", "000000000000000000000001")),
                eq(emptyList()),
                eq(config.queryParameters()),
                eq(parameters.timerange()),
                eq(parameters.batchSize()),
                any(MoreSearch.ScrollCallback.class)
        );

        reset(stateService, moreSearch, searchFactory); // Rest mocks so we can verify it again

        // If the dependency check returns false, a precondition exception should be raised and the state service not be called
        when(eventProcessorDependencyCheck.hasMessagesIndexedUpTo(timerange)).thenReturn(false);

        assertThatCode(() -> eventProcessor.createEvents(eventFactory, parameters, (events) -> {}))
                .hasMessageContaining(eventDefinitionDto.title())
                .hasMessageContaining(eventDefinitionDto.id())
                .hasMessageContaining(timerange.from().toString())
                .hasMessageContaining(timerange.to().toString())
                .isInstanceOf(EventProcessorPreconditionException.class);

        verify(stateService, never()).setState(any(String.class), any(DateTime.class), any(DateTime.class));
        verify(searchFactory, never()).create(any(), any(), any(), any(), any());
        verify(moreSearch, never()).scrollQuery(
                eq(config.query()),
                eq(config.streams()),
                eq(config.filters()), eq(config.queryParameters()),
                eq(parameters.timerange()),
                eq(parameters.batchSize()),
                any(MoreSearch.ScrollCallback.class)
        );
    }

    @Test
    public void testGroupByQueryString() throws EventProcessorException {
        Map<String, String> groupByFields = ImmutableMap.of(
                "group_field_one", "one",
                "group_field_two", "two"
        );
        sourceMessagesWithAggregation(groupByFields, 1, emptyList());

        String expectedQueryString = "(aQueryString) AND ((group_field_one:\"one\") AND (group_field_two:\"two\"))";
        verify(moreSearch).scrollQuery(eq(expectedQueryString), any(), any(), any(), any(), eq(1), any());
    }

    @Test
    public void testGroupByQueryStringEscapedValues() throws EventProcessorException {
        Map<String, String> groupByFields = ImmutableMap.of(
                "group_field_one", "\" \" * & ? - \\",
                "group_field_two", "/ / ~ | []{}"
        );
        sourceMessagesWithAggregation(groupByFields, 1, emptyList());

        String expectedQueryString = "(aQueryString) AND ((group_field_one:\"\\\" \\\" \\* \\& \\? \\- \\\\\") AND (group_field_two:\"\\/ \\/ \\~ \\| \\[\\]\\{\\}\"))";
        verify(moreSearch).scrollQuery(eq(expectedQueryString), any(), any(), any(), any(), eq(1), any());
    }

    @Test
    public void testGroupByQuerySingleValue() throws EventProcessorException {
        Map<String, String> groupByFields = ImmutableMap.of(
                "group_field_one", "group_value_one"
        );
        sourceMessagesWithAggregation(groupByFields, 5, emptyList());

        String expectedQueryString = "(aQueryString) AND (group_field_one:\"group_value_one\")";
        verify(moreSearch).scrollQuery(eq(expectedQueryString), any(), any(), any(), any(), eq(5), any());
    }

    @Test
    public void testGroupByQueryEmpty() throws EventProcessorException {
        Map<String, String> groupByFields = ImmutableMap.of();
        sourceMessagesWithAggregation(groupByFields, 5, emptyList());
        verify(moreSearch).scrollQuery(eq(QUERY_STRING), any(), any(), any(), any(), eq(5), any());
    }

    @Test
    public void testWithSearchFilters() throws EventProcessorException {
        final ArrayList<UsedSearchFilter> filters = new ArrayList<>();
        filters.add(InlineQueryStringSearchFilter.builder()
                .title("filter 1")
                .queryString("searchFilter:value")
                .build());
        filters.add(InlineQueryStringSearchFilter.builder()
                .title("filter 2")
                .queryString("searchFilter2:value2")
                .build());
        sourceMessagesWithAggregation(Collections.emptyMap(), 1, filters);
        verify(moreSearch).scrollQuery(eq(QUERY_STRING), any(), eq(filters), any(), any(), eq(1), any());
    }

    // Helper to call sourceMessagesForEvent when testing query string values - we don't care about anything else
    private void sourceMessagesWithAggregation(Map<String, String> groupByFields, int batchLimit, List<UsedSearchFilter> filters) throws EventProcessorException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timeRange = AbsoluteRange.create(now.minusHours(1), now.plusHours(1));
        final TestEvent event = new TestEvent(timeRange.to());
        event.setTimerangeStart(timeRange.from());
        event.setTimerangeEnd(timeRange.to());
        event.setGroupByFields(groupByFields);

        final SeriesSpec series = Count.builder()
                .id("abc123")
                .field("source")
                .build();
        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(series), null, filters);
        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(
                eventDefinitionDto, searchFactory, eventProcessorDependencyCheck, stateService, moreSearch,
                eventStreamService, messages, permittedStreams, Set.of(), messageFactory);

        eventProcessor.sourceMessagesForEvent(event, messageConsumer, batchLimit);
    }

    // Helper method to build test EventDefinitionDto, since we only care about a few of the values
    private EventDefinitionDto buildEventDefinitionDto(
            Set<String> testStreams, List<SeriesSpec> testSeries, AggregationConditions testConditions, List<UsedSearchFilter> filters) {
        return EventDefinitionDto.builder()
                .id("dto-id-1")
                .title("Test Aggregation")
                .description("A test aggregation event processors")
                .priority(1)
                .alert(false)
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .config(AggregationEventProcessorConfig.builder()
                        .query(QUERY_STRING)
                        .filters(filters)
                        .streams(testStreams)
                        .groupBy(ImmutableList.of("group_field_one", "group_field_two"))
                        .series(testSeries)
                        .conditions(testConditions)
                        .searchWithinMs(SEARCH_WINDOW_MS)
                        .executeEveryMs(SEARCH_WINDOW_MS)
                        .build())
                .keySpec(ImmutableList.of())
                .build();
    }
}
