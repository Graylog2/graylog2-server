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
import jakarta.annotation.Nonnull;
import org.graylog.events.conditions.Expr;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventFactory;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.event.TestEvent;
import org.graylog.events.fields.FieldValue;
import org.graylog.events.fields.FieldValueType;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventStreamService;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamMock;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AggregationSearchUtilsTest {
    public static final int SEARCH_WINDOW_MS = 30000;
    private static final String QUERY_STRING = "aQueryString";

    @Mock
    private AggregationSearch.Factory searchFactory;
    @Mock
    private StreamService streamService;
    @Mock
    private EventFactory eventFactory;

    private PermittedStreams permittedStreams = new PermittedStreams(streamService);
    private EventStreamService eventStreamService = new EventStreamService(streamService);
    private final MessageFactory messageFactory = new TestMessageFactory();

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

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of("stream-2"), ImmutableList.of(), null, emptyList());
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationSearchUtils searchUtils = new AggregationSearchUtils(
                eventDefinitionDto,
                (AggregationEventProcessorConfig) eventDefinitionDto.config(),
                Set.of(),
                searchFactory,
                eventStreamService,
                messageFactory,
                permittedStreams
        );

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
                                                .series(Count.builder()
                                                        .id("abc123")
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(23.0d)
                                                .series(Count.builder()
                                                        .id("abc123-no-field")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(Cardinality.builder()
                                                        .id("xyz789")
                                                        .field("source")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final ImmutableList<EventWithContext> eventsWithContext = searchUtils.eventsFromAggregationResult(eventFactory, parameters, result, (event) -> {});

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
    public void testEventsFromAggregationResultWithEventModifierState() throws EventProcessorException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.minusHours(1).plusMillis(SEARCH_WINDOW_MS));

        // We expect to get the end of the aggregation timerange as event time
        final TestEvent event1 = new TestEvent(timerange.to());
        final TestEvent event2 = new TestEvent(timerange.to());
        when(eventFactory.createEvent(any(EventDefinition.class), any(DateTime.class), anyString()))
                .thenReturn(event1)  // first invocation return value
                .thenReturn(event2); // second invocation return value

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of("stream-2"), ImmutableList.of(), null, emptyList());
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final EventQuerySearchTypeSupplier queryModifier = new EventQuerySearchTypeSupplier() {
            @Nonnull
            @Override
            public Set<SearchType> additionalSearchTypes(EventDefinition eventDefinition) {
                fail("Should not be called in this test, we only look at the result in isolation");
                return Set.of();
            }

            @Override
            public @Nonnull Map<String, Object> eventModifierData(Map<String, SearchType.Result> results) {
                assertThat(results).hasSize(1);
                assertThat(results.containsKey("query-modifier")).isTrue();
                assertThat(results.get("query-modifier").id()).isEqualTo("test");
                return Map.of("query-modifier", results.get("query-modifier").id());
            }
        };
        final AggregationSearchUtils searchUtils = new AggregationSearchUtils(
                eventDefinitionDto,
                (AggregationEventProcessorConfig) eventDefinitionDto.config(),
                Set.of(queryModifier),
                searchFactory,
                eventStreamService,
                messageFactory,
                permittedStreams
        );

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
                                                .series(Count.builder()
                                                        .id("abc123")
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(23.0d)
                                                .series(Count.builder()
                                                        .id("abc123-no-field")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(Cardinality.builder()
                                                        .id("xyz789")
                                                        .field("source")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .additionalResults(ImmutableMap.of(
                        "query-modifier", PivotResult.builder()
                                .id("test")
                                .effectiveTimerange(timerange)
                                .total(1)
                                .build()
                ))
                .build();

        final ImmutableList<EventWithContext> eventsWithContext = searchUtils.eventsFromAggregationResult(eventFactory, parameters, result, (event) -> {});

        assertThat(eventsWithContext).hasSize(1);
        assertThat(eventsWithContext.get(0).eventModifierState()).hasSize(1);
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

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), conditions, emptyList());
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationSearchUtils searchUtils = new AggregationSearchUtils(
                eventDefinitionDto,
                (AggregationEventProcessorConfig) eventDefinitionDto.config(),
                Set.of(),
                searchFactory,
                eventStreamService,
                messageFactory,
                permittedStreams
        );

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
                                                .series(Count.builder()
                                                        .id("abc123")
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(Cardinality.builder()
                                                        .id("xyz789")
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
                                                .series(Count.builder()
                                                        .id("abc123")
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(Cardinality.builder()
                                                        .id("xyz789")
                                                        .field("source")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final ImmutableList<EventWithContext> eventsWithContext = searchUtils.eventsFromAggregationResult(eventFactory, parameters, result, (event) -> {});

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
    public void testEventsFromAggregationResultWithEventDecorator() throws EventProcessorException {
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

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), conditions, emptyList());
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationSearchUtils searchUtils = new AggregationSearchUtils(
                eventDefinitionDto,
                (AggregationEventProcessorConfig) eventDefinitionDto.config(),
                Set.of(),
                searchFactory,
                eventStreamService,
                messageFactory,
                permittedStreams
        );

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
                                                .series(Count.builder()
                                                        .id("abc123")
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(Cardinality.builder()
                                                        .id("xyz789")
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
                                                .series(Count.builder()
                                                        .id("abc123")
                                                        .field("source")
                                                        .build())
                                                .build(),
                                        AggregationSeriesValue.builder()
                                                .key(ImmutableList.of("a"))
                                                .value(1.0d)
                                                .series(Cardinality.builder()
                                                        .id("xyz789")
                                                        .field("source")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        final Consumer<Event> eventDecorator = (event) -> {
            event.setField("decorated_field", FieldValue.builder().dataType(FieldValueType.STRING).value("decorated value").build());
        };
        final ImmutableList<EventWithContext> eventsWithContext = searchUtils.eventsFromAggregationResult(eventFactory, parameters, result, eventDecorator);

        assertThat(eventsWithContext).hasSize(1);

        assertThat(eventsWithContext.get(0)).satisfies(eventWithContext -> {
            final Event event = eventWithContext.event();

            assertThat(event.getField("decorated_field").value()).isEqualTo("decorated value");
        });
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

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of("stream-2"), ImmutableList.of(), null, emptyList());
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationSearchUtils searchUtils = new AggregationSearchUtils(
                eventDefinitionDto,
                (AggregationEventProcessorConfig) eventDefinitionDto.config(),
                Set.of(),
                searchFactory,
                eventStreamService,
                messageFactory,
                permittedStreams
        );
        final AggregationResult result = buildAggregationResult(timerange, timerange.to(), ImmutableList.of("one", "two"));
        final ImmutableList<EventWithContext> eventsWithContext = searchUtils.eventsFromAggregationResult(eventFactory, parameters, result, (event) -> {});

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
        when(streamService.loadAll()).thenReturn(ImmutableList.of(
                new StreamMock(Collections.singletonMap("_id", "stream-1"), emptyList()),
                new StreamMock(Collections.singletonMap("_id", "stream-2"), emptyList()),
                new StreamMock(Collections.singletonMap("_id", "stream-3"), emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_STREAM_ID), emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_EVENTS_STREAM_ID), emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.DEFAULT_SYSTEM_EVENTS_STREAM_ID), emptyList()),
                new StreamMock(Collections.singletonMap("_id", StreamImpl.FAILURES_STREAM_ID), emptyList())
        ));
        permittedStreams = new PermittedStreams(streamService);
        eventStreamService = new EventStreamService(streamService);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange timerange = AbsoluteRange.create(now.minusHours(1), now.minusHours(1).plusMillis(SEARCH_WINDOW_MS));

        // We expect to get the end of the aggregation timerange as event time
        final TestEvent event1 = new TestEvent(timerange.to());
        final TestEvent event2 = new TestEvent(timerange.to());
        when(eventFactory.createEvent(any(EventDefinition.class), any(DateTime.class), anyString()))
                .thenReturn(event1)  // first invocation return value
                .thenReturn(event2); // second invocation return value

        final EventDefinitionDto eventDefinitionDto = buildEventDefinitionDto(ImmutableSet.of(), ImmutableList.of(), null, emptyList());
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .timerange(timerange)
                .build();

        final AggregationSearchUtils searchUtils = new AggregationSearchUtils(
                eventDefinitionDto,
                (AggregationEventProcessorConfig) eventDefinitionDto.config(),
                Set.of(),
                searchFactory,
                eventStreamService,
                messageFactory,
                permittedStreams
        );
        final AggregationResult result = buildAggregationResult(timerange, timerange.to(), ImmutableList.of("one", "two"));
        final ImmutableList<EventWithContext> eventsWithContext = searchUtils.eventsFromAggregationResult(eventFactory, parameters, result, (event) -> {});

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
                                                .series(Count.builder()
                                                        .id("abc123")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();
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
