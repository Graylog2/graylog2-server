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
import org.graylog.events.conditions.Expr;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventFactory;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.event.TestEvent;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventProcessorDependencyCheck;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventProcessorPreconditionException;
import org.graylog.events.processor.EventStreamService;
import org.graylog.events.search.MoreSearch;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AggregationEventProcessorTest {
    public static final int SEARCH_WINDOW_MS = 30000;
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
    private EventStreamService eventStreamService;

    @Before
    public void setUp() throws Exception {
        final StreamService streamService = mock(StreamService.class);
        eventStreamService = new EventStreamService(streamService);
        when(streamService.loadAll()).thenReturn(ImmutableList.of(
                new StreamMock(Collections.singletonMap("_id", "stream-1"), Collections.emptyList()),
                new StreamMock(Collections.singletonMap("_id", "stream-2"), Collections.emptyList()),
                new StreamMock(Collections.singletonMap("_id", "stream-3"), Collections.emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_STREAM_ID), Collections.emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_EVENTS_STREAM_ID), Collections.emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_SYSTEM_EVENTS_STREAM_ID), Collections.emptyList())
        ));
    }

    @Test
    public void testEventsFromAggregationResult() throws EventProcessorException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.minusHours(1).plusMillis(SEARCH_WINDOW_MS));

        // We expect to get the end of the aggregation timerange as event time
        final TestEvent event1 = new TestEvent(timerange.to());
        final TestEvent event2 = new TestEvent(timerange.to());
        when(eventFactory.createEvent(any(EventDefinition.class), any(DateTime.class), anyString()))
                .thenReturn(event1)  // first invocation return value
                .thenReturn(event2); // second invocation return value

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of("stream-2"), ImmutableList.of(), null);
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(
                eventDefinitionDto, searchFactory, eventProcessorDependencyCheck, stateService, moreSearch, eventStreamService, messages);

        final AggregationResult result = AggregationResult.builder()
                .effectiveTimerange(timerange)
                .totalAggregatedMessages(1)
                .sourceStreams(ImmutableSet.of("stream-1", "stream-2"))
                .keyResults(ImmutableList.of(
                        AggregationKeyResult.builder()
                                .key(ImmutableList.of("one", "two"))
                                .timestamp(timerange.to())
                                .seriesValues(ImmutableList.of(
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(42.0d)
                                                .series(AggregationSeries.builder()
                                                        .id("abc123")
                                                        .function(AggregationFunction.COUNT)
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(23.0d)
                                                .series(AggregationSeries.builder()
                                                        .id("abc123-no-field")
                                                        .function(AggregationFunction.COUNT)
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(AggregationSeries.builder()
                                                        .id("xyz789")
                                                        .function(AggregationFunction.CARD)
                                                        .field("source")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final ImmutableList<EventWithContext> eventsWithContext = eventProcessor.eventsFromAggregationResult(eventFactory, parameters, result);

        assertThat(eventsWithContext).hasSize(1);

        assertThat(eventsWithContext.get(0)).satisfies(eventWithContext -> {
            final Event event = eventWithContext.event();

            assertThat(event.getId()).isEqualTo(event1.getId());
            assertThat(event.getMessage()).isEqualTo(event1.getMessage());
            assertThat(event.getEventTimestamp()).isEqualTo(timerange.to());
            assertThat(event.getTimerangeStart()).isEqualTo(timerange.from());
            assertThat(event.getTimerangeEnd()).isEqualTo(timerange.to());
            // Should only contain the streams that have been configured in event definition
            assertThat(event.getSourceStreams()).containsOnly("stream-2");

            final Message message = eventWithContext.messageContext().orElse(null);

            assertThat(message).isNotNull();
            assertThat(message.getField("group_field_one")).isEqualTo("one");
            assertThat(message.getField("group_field_two")).isEqualTo("two");
            assertThat(message.getField("aggregation_key")).isEqualTo("one|two");
            assertThat(message.getField("aggregation_value_count_source")).isEqualTo(42.0d);
            // Make sure that the count with a "null" field doesn't include the field in the name
            assertThat(message.getField("aggregation_value_count")).isEqualTo(23.0d);
            assertThat(message.getField("aggregation_value_card_source")).isEqualTo(1.0d);

            assertThat(event.getGroupByFields().get("group_field_one")).isEqualTo("one");
            assertThat(event.getGroupByFields().get("group_field_two")).isEqualTo("two");
        });
    }

    @Test
    public void testEventsFromAggregationResultWithConditions() throws EventProcessorException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.minusHours(1).plusMillis(SEARCH_WINDOW_MS));

        // We expect to get the end of the aggregation timerange as event time
        final TestEvent event1 = new TestEvent(timerange.to());
        final TestEvent event2 = new TestEvent(timerange.to());
        when(eventFactory.createEvent(any(EventDefinition.class), any(DateTime.class), anyString()))
                .thenReturn(event1)  // first invocation return value
                .thenReturn(event2); // second invocation return value

        // There should only be one result because the second result's "abc123" value is less than 40. (it is 23)
        // See result builder below
        final AggregationConditions conditions = AggregationConditions.builder()
                .expression(Expr.And.create(
                        Expr.Greater.create(Expr.NumberReference.create("abc123"), Expr.NumberValue.create(40.0d)),
                        Expr.Lesser.create(Expr.NumberReference.create("xyz789"), Expr.NumberValue.create(2.0d))
                ))
                .build();

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), conditions);
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(eventDefinitionDto, searchFactory, eventProcessorDependencyCheck, stateService, moreSearch, eventStreamService, messages);

        final AggregationResult result = AggregationResult.builder()
                .effectiveTimerange(timerange)
                .totalAggregatedMessages(1)
                .sourceStreams(ImmutableSet.of("stream-1", "stream-2", "stream-3"))
                .keyResults(ImmutableList.of(
                        AggregationKeyResult.builder()
                                .key(ImmutableList.of("one", "two"))
                                .timestamp(timerange.to())
                                .seriesValues(ImmutableList.of(
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(42.0d)
                                                .series(AggregationSeries.builder()
                                                        .id("abc123")
                                                        .function(AggregationFunction.COUNT)
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(AggregationSeries.builder()
                                                        .id("xyz789")
                                                        .function(AggregationFunction.CARD)
                                                        .field("source")
                                                        .build())
                                                .build()
                                ))
                                .build(),
                        AggregationKeyResult.builder()
                                .key(ImmutableList.of(now.toString(), "one", "two"))
                                .seriesValues(ImmutableList.of(
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(23.0d) // Doesn't match condition
                                                .series(AggregationSeries.builder()
                                                        .id("abc123")
                                                        .function(AggregationFunction.COUNT)
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(AggregationSeries.builder()
                                                        .id("xyz789")
                                                        .function(AggregationFunction.CARD)
                                                        .field("source")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final ImmutableList<EventWithContext> eventsWithContext = eventProcessor.eventsFromAggregationResult(eventFactory, parameters, result);

        assertThat(eventsWithContext).hasSize(1);

        assertThat(eventsWithContext.get(0)).satisfies(eventWithContext -> {
            final Event event = eventWithContext.event();

            assertThat(event.getId()).isEqualTo(event1.getId());
            assertThat(event.getMessage()).isEqualTo(event1.getMessage());
            assertThat(event.getEventTimestamp()).isEqualTo(timerange.to());
            assertThat(event.getTimerangeStart()).isEqualTo(timerange.from());
            assertThat(event.getTimerangeEnd()).isEqualTo(timerange.to());
            // Should contain all streams because when config.streams is empty, we search in all streams
            assertThat(event.getSourceStreams()).containsOnly("stream-1", "stream-2", "stream-3");

            final Message message = eventWithContext.messageContext().orElse(null);

            assertThat(message).isNotNull();
            assertThat(message.getField("group_field_one")).isEqualTo("one");
            assertThat(message.getField("group_field_two")).isEqualTo("two");
            assertThat(message.getField("aggregation_key")).isEqualTo("one|two");
            assertThat(message.getField("aggregation_value_count_source")).isEqualTo(42.0d);
            assertThat(message.getField("aggregation_value_card_source")).isEqualTo(1.0d);

            assertThat(event.getGroupByFields().get("group_field_one")).isEqualTo("one");
            assertThat(event.getGroupByFields().get("group_field_two")).isEqualTo("two");
        });
    }

    @Test
    public void createEventsWithFilter() throws Exception {
        when(eventProcessorDependencyCheck.hasMessagesIndexedUpTo(any(TimeRange.class))).thenReturn(true);

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.minusHours(1).plusMillis(SEARCH_WINDOW_MS));

        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("aQueryString")
                .streams(ImmutableSet.of())
                .groupBy(ImmutableList.of())
                .series(ImmutableList.of())
                .conditions(null)
                .searchWithinMs(SEARCH_WINDOW_MS)
                .executeEveryMs(SEARCH_WINDOW_MS)
                .build();
        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), null);
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(eventDefinitionDto, searchFactory, eventProcessorDependencyCheck, stateService, moreSearch, eventStreamService, messages);

        assertThatCode(() -> eventProcessor.createEvents(eventFactory, parameters, (events) -> {})).doesNotThrowAnyException();

        verify(moreSearch, times(1)).scrollQuery(
                eq(config.query()),
                eq(config.streams()),
                eq(config.queryParameters()),
                eq(parameters.timerange()),
                eq(parameters.batchSize()),
                any(MoreSearch.ScrollCallback.class)
        );
        verify(searchFactory, never()).create(eq(config), eq(parameters), any(String.class), eq(eventDefinitionDto));
    }

    @Test
    public void createEventsWithoutRequiredMessagesBeingIndexed() throws Exception {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.plusHours(1));

        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("aQueryString")
                .streams(ImmutableSet.of())
                .groupBy(ImmutableList.of())
                .series(ImmutableList.of())
                .conditions(null)
                .searchWithinMs(30000)
                .executeEveryMs(30000)
                .build();
        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), null);
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(eventDefinitionDto, searchFactory, eventProcessorDependencyCheck, stateService, moreSearch, eventStreamService, messages);

        // If the dependency check returns true, there should be no exception raised and the state service should be called
        when(eventProcessorDependencyCheck.hasMessagesIndexedUpTo(timerange)).thenReturn(true);

        assertThatCode(() -> eventProcessor.createEvents(eventFactory, parameters, (events) -> {})).doesNotThrowAnyException();

        verify(stateService, times(1)).setState("dto-id-1", timerange.from(), timerange.to());
        verify(moreSearch, times(1)).scrollQuery(
                eq(config.query()),
                eq(config.streams()),
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
        verify(searchFactory, never()).create(any(), any(), any(), any());
        verify(moreSearch, never()).scrollQuery(
                eq(config.query()),
                eq(config.streams()),
                eq(config.queryParameters()),
                eq(parameters.timerange()),
                eq(parameters.batchSize()),
                any(MoreSearch.ScrollCallback.class)
        );
    }

    @Test
    public void testEventsFromAggregationResultWithEmptyResultUsesEventDefinitionStreamAsSourceStreams() throws EventProcessorException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.minusHours(1).plusMillis(SEARCH_WINDOW_MS));

        // We expect to get the end of the aggregation timerange as event time
        final TestEvent event1 = new TestEvent(timerange.to());
        final TestEvent event2 = new TestEvent(timerange.to());
        when(eventFactory.createEvent(any(EventDefinition.class), any(DateTime.class), anyString()))
                .thenReturn(event1)  // first invocation return value
                .thenReturn(event2); // second invocation return value

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of("stream-2"), ImmutableList.of(), null);
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(eventDefinitionDto, searchFactory, eventProcessorDependencyCheck, stateService, moreSearch, eventStreamService, messages);
        final AggregationResult result = buildAggregationResult(timerange, timerange.to(), ImmutableList.of("one", "two"));
        final ImmutableList<EventWithContext> eventsWithContext = eventProcessor.eventsFromAggregationResult(eventFactory, parameters, result);

        assertThat(eventsWithContext).hasSize(1);

        assertThat(eventsWithContext.get(0)).satisfies(eventWithContext -> {
            final Event event = eventWithContext.event();

            assertThat(event.getId()).isEqualTo(event1.getId());
            assertThat(event.getMessage()).isEqualTo(event1.getMessage());
            assertThat(event.getEventTimestamp()).isEqualTo(timerange.to());
            assertThat(event.getTimerangeStart()).isEqualTo(timerange.from());
            assertThat(event.getTimerangeEnd()).isEqualTo(timerange.to());
            // Must contain the stream from the event definition because there is none in the result
            assertThat(event.getSourceStreams()).containsOnly("stream-2");

            final Message message = eventWithContext.messageContext().orElse(null);

            assertThat(message).isNotNull();
            assertThat(message.getField("group_field_one")).isEqualTo("one");
            assertThat(message.getField("group_field_two")).isEqualTo("two");
            assertThat(message.getField("aggregation_key")).isEqualTo("one|two");
            assertThat(message.getField("aggregation_value_count")).isEqualTo(0.0d);
        });
    }

    @Test
    public void testEventsFromAggregationResultWithEmptyResultAndNoConfiguredStreamsUsesAllStreamsAsSourceStreams() throws EventProcessorException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.minusHours(1).plusMillis(SEARCH_WINDOW_MS));

        // We expect to get the end of the aggregation timerange as event time
        final TestEvent event1 = new TestEvent(timerange.to());
        final TestEvent event2 = new TestEvent(timerange.to());
        when(eventFactory.createEvent(any(EventDefinition.class), any(DateTime.class), anyString()))
                .thenReturn(event1)  // first invocation return value
                .thenReturn(event2); // second invocation return value

        final StreamService streamService = mock(StreamService.class);

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), null);
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(eventDefinitionDto, searchFactory, eventProcessorDependencyCheck, stateService, moreSearch,
                eventStreamService, messages);
        final AggregationResult result = buildAggregationResult(timerange, timerange.to(), ImmutableList.of("one", "two"));
        final ImmutableList<EventWithContext> eventsWithContext = eventProcessor.eventsFromAggregationResult(eventFactory, parameters, result);

        assertThat(eventsWithContext).hasSize(1);

        assertThat(eventsWithContext.get(0)).satisfies(eventWithContext -> {
            final Event event = eventWithContext.event();

            assertThat(event.getId()).isEqualTo(event1.getId());
            assertThat(event.getMessage()).isEqualTo(event1.getMessage());
            assertThat(event.getEventTimestamp()).isEqualTo(timerange.to());
            assertThat(event.getTimerangeStart()).isEqualTo(timerange.from());
            assertThat(event.getTimerangeEnd()).isEqualTo(timerange.to());
            // Must contain all existing streams but the default event streams!
            assertThat(event.getSourceStreams()).containsOnly(
                    "stream-1",
                    "stream-2",
                    "stream-3",
                    StreamImpl.DEFAULT_STREAM_ID
            );

            final Message message = eventWithContext.messageContext().orElse(null);

            assertThat(message).isNotNull();
            assertThat(message.getField("group_field_one")).isEqualTo("one");
            assertThat(message.getField("group_field_two")).isEqualTo("two");
            assertThat(message.getField("aggregation_key")).isEqualTo("one|two");
            assertThat(message.getField("aggregation_value_count")).isEqualTo(0.0d);
        });
    }

    @Test
    public void testGroupByQueryString() throws EventProcessorException {
        Map<String, String> groupByFields = ImmutableMap.of(
                "group_field_one", "one",
                "group_field_two", "two"
        );
        sourceMessagesWithAggregation(groupByFields, 1);

        String expectedQueryString = "(aQueryString) AND ((group_field_one:\"one\") AND (group_field_two:\"two\"))";
        verify(moreSearch).scrollQuery(eq(expectedQueryString), any(), any(), any(), eq(1), any());
    }

    @Test
    public void testGroupByQueryStringEscapedValues() throws EventProcessorException {
        Map<String, String> groupByFields = ImmutableMap.of(
                "group_field_one", "\" \" * & ? - \\",
                "group_field_two", "/ / ~ | []{}"
        );
        sourceMessagesWithAggregation(groupByFields, 1);

        String expectedQueryString = "(aQueryString) AND ((group_field_one:\"\\\" \\\" \\* \\& \\? \\- \\\\\") AND (group_field_two:\"\\/ \\/ \\~ \\| \\[\\]\\{\\}\"))";
        verify(moreSearch).scrollQuery(eq(expectedQueryString), any(), any(), any(), eq(1), any());
    }

    @Test
    public void testGroupByQuerySingleValue() throws EventProcessorException {
        Map<String, String> groupByFields = ImmutableMap.of(
                "group_field_one", "group_value_one"
        );
        sourceMessagesWithAggregation(groupByFields, 5);

        String expectedQueryString = "(aQueryString) AND (group_field_one:\"group_value_one\")";
        verify(moreSearch).scrollQuery(eq(expectedQueryString), any(), any(), any(), eq(5), any());
    }

    @Test
    public void testGroupByQueryEmpty() throws EventProcessorException {
        Map<String, String> groupByFields = ImmutableMap.of();
        sourceMessagesWithAggregation(groupByFields, 5);

        String expectedQueryString = "aQueryString";
        verify(moreSearch).scrollQuery(eq(expectedQueryString), any(), any(), any(), eq(5), any());
    }

    // Helper to call sourceMessagesForEvent when testing query string values - we don't care about anything else
    private void sourceMessagesWithAggregation(Map<String, String> groupByFields, int batchLimit) throws EventProcessorException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timeRange = AbsoluteRange.create(now.minusHours(1), now.plusHours(1));
        final TestEvent event = new TestEvent(timeRange.to());
        event.setTimerangeStart(timeRange.from());
        event.setTimerangeEnd(timeRange.to());
        event.setGroupByFields(groupByFields);

        final AggregationSeries series = AggregationSeries.builder()
                .id("abc123")
                .function(AggregationFunction.COUNT)
                .field("source")
                .build();
        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(series), null);
        final AggregationEventProcessor eventProcessor = new AggregationEventProcessor(
                eventDefinitionDto, searchFactory, eventProcessorDependencyCheck, stateService, moreSearch, eventStreamService, messages);

        eventProcessor.sourceMessagesForEvent(event, messageConsumer, batchLimit);
    }

    // Helper method to build test AggregationResult, since we only care about a few of the values
    private AggregationResult buildAggregationResult(AbsoluteRange timeRange, DateTime dateTime, List<String> testKey) {
        return AggregationResult.builder()
                .effectiveTimerange(timeRange)
                .totalAggregatedMessages(0)
                .sourceStreams(ImmutableSet.of()) // No streams in result
                .keyResults(ImmutableList.of(
                        AggregationKeyResult.builder()
                                .key(testKey)
                                .timestamp(dateTime)
                                .seriesValues(ImmutableList.of(
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(0.0d)
                                                .series(AggregationSeries.builder()
                                                        .id("abc123")
                                                        .function(AggregationFunction.COUNT)
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    // Helper method to build test EventDefinitionDto, since we only care about a few of the values
    private EventDefinitionDto buildEventDefinitionDto(
            Set<String> testStreams, List<AggregationSeries> testSeries, AggregationConditions testConditions) {
        return EventDefinitionDto.builder()
                .id("dto-id-1")
                .title("Test Aggregation")
                .description("A test aggregation event processors")
                .priority(1)
                .alert(false)
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .config(AggregationEventProcessorConfig.builder()
                        .query("aQueryString")
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
