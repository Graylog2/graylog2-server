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
package org.graylog.events.processor.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventFactory;
import org.graylog.events.event.EventReplayInfo;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventConsumer;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventProcessorParameters;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoDBEventProcessorTest {

    private static final String COLLECTION_NAME = "test_collection";
    private static final String VALID_PIPELINE = """
            [{"$group": {"_id": null, "count": {"$sum": 1}}}]""";

    @Mock private MongoCollections mongoCollections;
    @Mock private DBEventProcessorStateService stateService;
    @Mock private MessageFactory messageFactory;
    @Mock private EventDefinition eventDefinition;
    @Mock private EventFactory eventFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Minimal concrete subclass of MongoDBEventProcessor for testing the base class behavior.
     * Does not override createEvents — all events pass through unfiltered.
     */
    static class TestableMongoDBEventProcessor extends MongoDBEventProcessor {
        TestableMongoDBEventProcessor(EventDefinition eventDefinition,
                                      MongoDBEventProcessorConfig config,
                                      String collectionName,
                                      MongoCollections mongoCollections,
                                      DBEventProcessorStateService stateService,
                                      MessageFactory messageFactory,
                                      ObjectMapper objectMapper) {
            super(eventDefinition, config, collectionName, mongoCollections, stateService, messageFactory, objectMapper);
        }
    }

    @BeforeEach
    void setUp() {
        lenient().when(eventDefinition.id()).thenReturn("test-event-def-id");
        lenient().when(eventDefinition.title()).thenReturn("Test Event");
    }

    private TestableMongoDBEventProcessor createProcessor(MongoDBEventProcessorConfig config) {
        return new TestableMongoDBEventProcessor(eventDefinition, config, COLLECTION_NAME,
                mongoCollections, stateService, messageFactory, objectMapper);
    }

    private MongoDBEventProcessorConfig createConfig() {
        return createConfig(VALID_PIPELINE);
    }

    private MongoDBEventProcessorConfig createConfig(String pipeline) {
        return MongoDBEventProcessorConfig.builder()
                .aggregationPipeline(pipeline)
                .timestampField("bucket")
                .searchWithinSeconds(60)
                .executeEverySeconds(60)
                .build();
    }

    private MongoDBEventProcessorParameters createParams(DateTime from, DateTime to) {
        return MongoDBEventProcessorParameters.builder()
                .timerange(AbsoluteRange.create(from, to))
                .build();
    }

    /**
     * Creates a mock Event that tracks setField/getField calls via a backing HashMap,
     * so that field values set by the processor can be inspected in assertions.
     */
    private Event createFieldTrackingEvent() {
        final Map<String, FieldValue> fields = new HashMap<>();
        final Event event = mock(Event.class);
        doAnswer(inv -> {
            fields.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(event).setField(anyString(), any(FieldValue.class));
        lenient().when(event.getField(anyString())).thenAnswer(inv -> fields.get(inv.getArgument(0)));
        lenient().when(event.getMessage()).thenReturn("Test Event");
        return event;
    }

    @SuppressWarnings("unchecked")
    private void setupAggregation(Document result) {
        final com.mongodb.client.MongoCollection<Document> rawCollection = mock(com.mongodb.client.MongoCollection.class);
        final AggregateIterable<Document> aggregateIterable = mock(AggregateIterable.class);

        when(mongoCollections.nonEntityCollection(eq(COLLECTION_NAME), eq(Document.class))).thenReturn(rawCollection);
        when(rawCollection.aggregate(any(List.class), eq(Document.class))).thenReturn(aggregateIterable);
        when(aggregateIterable.batchSize(anyInt())).thenReturn(aggregateIterable);
        when(aggregateIterable.first()).thenReturn(result);
    }

    @SuppressWarnings("unchecked")
    private void setupAggregationThrows(RuntimeException exception) {
        final com.mongodb.client.MongoCollection<Document> rawCollection = mock(com.mongodb.client.MongoCollection.class);
        final AggregateIterable<Document> aggregateIterable = mock(AggregateIterable.class);

        when(mongoCollections.nonEntityCollection(eq(COLLECTION_NAME), eq(Document.class))).thenReturn(rawCollection);
        when(rawCollection.aggregate(any(List.class), eq(Document.class))).thenReturn(aggregateIterable);
        when(aggregateIterable.batchSize(anyInt())).thenReturn(aggregateIterable);
        when(aggregateIterable.first()).thenThrow(exception);
    }

    // ==================== Invalid Parameters ====================

    @Test
    void createEvents_invalidParameterType_throwsException() {
        final var processor = createProcessor(createConfig());
        final EventProcessorParameters invalidParams = mock(EventProcessorParameters.class);

        @SuppressWarnings("unchecked")
        final EventConsumer<List<EventWithContext>> consumer = mock(EventConsumer.class);

        assertThatThrownBy(() -> processor.createEvents(eventFactory, invalidParams, consumer))
                .isInstanceOf(EventProcessorException.class)
                .hasMessageContaining("Invalid parameters type")
                .hasMessageContaining("MongoDBEventProcessorParameters");
    }

    // ==================== Event Field Population ====================

    @SuppressWarnings("unchecked")
    @Test
    void createEvents_setsEventFieldsFromAggregation() throws Exception {
        final Document result = new Document()
                .append("_id", null)
                .append("count", 42)
                .append("total_bytes", 1024L);

        setupAggregation(result);

        final Event event = createFieldTrackingEvent();
        when(eventFactory.createEvent(any(), any(), any())).thenReturn(event);
        when(messageFactory.createMessage(anyString(), anyString(), any(DateTime.class))).thenReturn(mock(Message.class));

        final var processor = createProcessor(createConfig());
        final EventConsumer<List<EventWithContext>> consumer = mock(EventConsumer.class);
        final DateTime from = DateTime.now(DateTimeZone.UTC).minusHours(1);
        final DateTime to = DateTime.now(DateTimeZone.UTC);

        processor.createEvents(eventFactory, createParams(from, to), consumer);

        // Aggregation fields should be set on the event (as string values)
        assertThat(event.getField("count")).isNotNull();
        assertThat(event.getField("count").value()).isEqualTo("42");
        assertThat(event.getField("total_bytes")).isNotNull();
        assertThat(event.getField("total_bytes").value()).isEqualTo("1024");

        // _id should be skipped
        assertThat(event.getField("_id")).isNull();
    }

    // ==================== Event Metadata ====================

    @SuppressWarnings("unchecked")
    @Test
    void createEvents_setsEventMetadata() throws Exception {
        final Document result = new Document("count", 1);
        setupAggregation(result);

        final Event event = createFieldTrackingEvent();
        when(eventFactory.createEvent(any(), any(), any())).thenReturn(event);
        when(messageFactory.createMessage(anyString(), anyString(), any(DateTime.class))).thenReturn(mock(Message.class));

        final var processor = createProcessor(createConfig());
        final EventConsumer<List<EventWithContext>> consumer = mock(EventConsumer.class);
        final DateTime from = new DateTime(2026, 3, 5, 0, 0, DateTimeZone.UTC);
        final DateTime to = new DateTime(2026, 3, 5, 12, 0, DateTimeZone.UTC);

        processor.createEvents(eventFactory, createParams(from, to), consumer);

        // Event should be created with the end-of-timerange timestamp and event definition title
        verify(eventFactory).createEvent(eq(eventDefinition), eq(to), eq("Test Event"));

        // Timerange should be set
        verify(event).setTimerangeStart(from);
        verify(event).setTimerangeEnd(to);

        // Origin context should reference the collection and time range
        final ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
        verify(event).setOriginContext(originCaptor.capture());
        assertThat(originCaptor.getValue())
                .contains(COLLECTION_NAME)
                .contains(String.valueOf(from.getMillis()))
                .contains(String.valueOf(to.getMillis()));

        // Replay info should be set with timerange and pipeline query
        final ArgumentCaptor<EventReplayInfo> replayCaptor = ArgumentCaptor.forClass(EventReplayInfo.class);
        verify(event).setReplayInfo(replayCaptor.capture());
        final EventReplayInfo replayInfo = replayCaptor.getValue();
        assertThat(replayInfo.timerangeStart()).isEqualTo(from);
        assertThat(replayInfo.timerangeEnd()).isEqualTo(to);
        assertThat(replayInfo.query()).isEqualTo(VALID_PIPELINE);
    }

    // ==================== Message Creation ====================

    @SuppressWarnings("unchecked")
    @Test
    void createEvents_createsMessageWithAggregationResult() throws Exception {
        final Document result = new Document()
                .append("_id", null)
                .append("count", 42);
        setupAggregation(result);

        final Event event = createFieldTrackingEvent();
        when(eventFactory.createEvent(any(), any(), any())).thenReturn(event);

        final Message message = mock(Message.class);
        when(messageFactory.createMessage(anyString(), anyString(), any(DateTime.class))).thenReturn(message);

        final var processor = createProcessor(createConfig());
        final EventConsumer<List<EventWithContext>> consumer = mock(EventConsumer.class);
        final DateTime from = new DateTime(2026, 3, 5, 0, 0, DateTimeZone.UTC);
        final DateTime to = new DateTime(2026, 3, 5, 12, 0, DateTimeZone.UTC);

        processor.createEvents(eventFactory, createParams(from, to), consumer);

        // Message should be created with collection name in text, at the end-of-timerange timestamp
        final ArgumentCaptor<String> msgTextCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageFactory).createMessage(msgTextCaptor.capture(), eq("mongodb-event-processor"), eq(to));
        assertThat(msgTextCaptor.getValue()).contains(COLLECTION_NAME);

        // Message should have aggregation fields added
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, Object>> fieldsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(message).addFields(fieldsCaptor.capture());
        final Map<String, Object> messageFields = fieldsCaptor.getValue();
        assertThat(messageFields).containsKey("aggregation_result");
        assertThat(messageFields).containsKey("timerange_start");
        assertThat(messageFields).containsKey("timerange_end");
        assertThat(messageFields).containsEntry("count", 42);
        // _id should be skipped in message fields too
        assertThat(messageFields).doesNotContainKey("_id");
    }

    // ==================== State Service ====================

    @SuppressWarnings("unchecked")
    @Test
    void createEvents_updatesStateOnResult() throws Exception {
        final Document result = new Document("count", 1);
        setupAggregation(result);

        final Event event = createFieldTrackingEvent();
        when(eventFactory.createEvent(any(), any(), any())).thenReturn(event);
        when(messageFactory.createMessage(anyString(), anyString(), any(DateTime.class))).thenReturn(mock(Message.class));

        final var processor = createProcessor(createConfig());
        final EventConsumer<List<EventWithContext>> consumer = mock(EventConsumer.class);
        final DateTime from = new DateTime(2026, 3, 5, 0, 0, DateTimeZone.UTC);
        final DateTime to = new DateTime(2026, 3, 5, 12, 0, DateTimeZone.UTC);

        processor.createEvents(eventFactory, createParams(from, to), consumer);

        verify(stateService).setState("test-event-def-id", from, to);
    }

    @SuppressWarnings("unchecked")
    @Test
    void createEvents_updatesStateOnNoResult() throws Exception {
        setupAggregation(null);

        final var processor = createProcessor(createConfig());
        final EventConsumer<List<EventWithContext>> consumer = mock(EventConsumer.class);
        final DateTime from = new DateTime(2026, 3, 5, 0, 0, DateTimeZone.UTC);
        final DateTime to = new DateTime(2026, 3, 5, 12, 0, DateTimeZone.UTC);

        processor.createEvents(eventFactory, createParams(from, to), consumer);

        // State should still be updated even with no results
        verify(stateService).setState("test-event-def-id", from, to);
        // Consumer should NOT be called
        verify(consumer, never()).accept(any());
    }

    // ==================== Exception Handling ====================

    @SuppressWarnings("unchecked")
    @Test
    void createEvents_wrapsAggregationException() {
        setupAggregationThrows(new MongoException("Connection lost"));

        final var processor = createProcessor(createConfig());
        final EventConsumer<List<EventWithContext>> consumer = mock(EventConsumer.class);
        final DateTime from = DateTime.now(DateTimeZone.UTC).minusHours(1);
        final DateTime to = DateTime.now(DateTimeZone.UTC);

        assertThatThrownBy(() -> processor.createEvents(eventFactory, createParams(from, to), consumer))
                .isInstanceOf(EventProcessorException.class)
                .hasMessageContaining("Failed to execute MongoDB aggregation")
                .hasMessageContaining("Test Event")
                .hasCauseInstanceOf(MongoException.class);
    }

    // ==================== Value Conversion ====================

    @SuppressWarnings("unchecked")
    @Test
    void createEvents_convertsMongoValueTypes() throws Exception {
        final Date javaDate = new Date(1709596800000L); // 2024-03-05T00:00:00Z
        final ObjectId objectId = new ObjectId("507f1f77bcf86cd799439011");
        final DateTime jodaDateTime = new DateTime(2026, 6, 15, 10, 30, DateTimeZone.UTC);

        final Document result = new Document()
                .append("date_field", javaDate)
                .append("objectid_field", objectId)
                .append("number_field", 12345L)
                .append("string_field", "hello")
                .append("null_field", null);
        setupAggregation(result);

        final Event event = createFieldTrackingEvent();
        when(eventFactory.createEvent(any(), any(), any())).thenReturn(event);
        when(messageFactory.createMessage(anyString(), anyString(), any(DateTime.class))).thenReturn(mock(Message.class));

        final var processor = createProcessor(createConfig());
        final EventConsumer<List<EventWithContext>> consumer = mock(EventConsumer.class);
        final DateTime from = DateTime.now(DateTimeZone.UTC).minusHours(1);
        final DateTime to = DateTime.now(DateTimeZone.UTC);

        processor.createEvents(eventFactory, createParams(from, to), consumer);

        // Date should be converted to a DateTime string representation
        assertThat(event.getField("date_field")).isNotNull();
        assertThat(event.getField("date_field").value()).contains("2024-03-05");

        // ObjectId should be converted to its string representation
        assertThat(event.getField("objectid_field")).isNotNull();
        assertThat(event.getField("objectid_field").value()).isEqualTo("507f1f77bcf86cd799439011");

        // Numbers should be preserved (as string via FieldValue.string())
        assertThat(event.getField("number_field")).isNotNull();
        assertThat(event.getField("number_field").value()).isEqualTo("12345");

        // Strings should be preserved
        assertThat(event.getField("string_field")).isNotNull();
        assertThat(event.getField("string_field").value()).isEqualTo("hello");

        // Null values should result in "null" string (String.valueOf(null))
        assertThat(event.getField("null_field")).isNotNull();
        assertThat(event.getField("null_field").value()).isEqualTo("null");
    }
}
