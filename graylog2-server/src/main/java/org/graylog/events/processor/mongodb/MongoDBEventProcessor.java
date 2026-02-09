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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.inject.assistedinject.Assisted;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventFactory;
import org.graylog.events.event.EventOriginContext;
import org.graylog.events.event.EventReplayInfo;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventConsumer;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessor;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventProcessorParameters;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.MessageSummary;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;

public class MongoDBEventProcessor implements EventProcessor {
    public interface Factory extends EventProcessor.Factory<MongoDBEventProcessor> {
        @Override
        MongoDBEventProcessor create(EventDefinition eventDefinition);
    }

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBEventProcessor.class);

    private static final int MONGO_BATCHSIZE = 200;

    private final EventDefinition eventDefinition;
    private final MongoDBEventProcessorConfig config;
    private final MongoCollection<Document> collection;
    private final DBEventProcessorStateService stateService;
    private final MessageFactory messageFactory;

    @Inject
    public MongoDBEventProcessor(@Assisted EventDefinition eventDefinition,
                                 MongoCollections mongoCollections,
                                 DBEventProcessorStateService stateService,
                                 MessageFactory messageFactory) {
        this(eventDefinition, (MongoDBEventProcessorConfig) eventDefinition.config(),
                mongoCollections, stateService, messageFactory);
    }

    // Constructor that allows config override - used by specialized processors
    protected MongoDBEventProcessor(EventDefinition eventDefinition,
                                    MongoDBEventProcessorConfig config,
                                    MongoCollections mongoCollections,
                                    DBEventProcessorStateService stateService,
                                    MessageFactory messageFactory) {
        this.eventDefinition = eventDefinition;
        this.config = config;
        this.collection = mongoCollections.nonEntityCollection(config.collectionName(), Document.class);
        this.stateService = stateService;
        this.messageFactory = messageFactory;
    }

    @Override
    public void createEvents(EventFactory eventFactory,
                            EventProcessorParameters processorParameters,
                            EventConsumer<List<EventWithContext>> eventsConsumer)
            throws EventProcessorException {
        final MongoDBEventProcessorParameters parameters = (MongoDBEventProcessorParameters) processorParameters;

        LOG.debug("Creating events from MongoDB aggregation for config={} parameters={}", config, parameters);

        try {
            // Build aggregation pipeline
            List<Bson> pipeline = buildAggregationPipeline(parameters);

            // Execute aggregation
            AggregateIterable<Document> aggregateIterable = collection
                    .aggregate(pipeline, Document.class)
                    .batchSize(MONGO_BATCHSIZE);

            // Get the single result from aggregation
            Document result = aggregateIterable.first();

            if (result == null) {
                LOG.debug("Aggregation returned no results for timerange {} to {}",
                        parameters.timerange().getFrom(), parameters.timerange().getTo());
                // Update state even if no results
                stateService.setState(eventDefinition.id(),
                        parameters.timerange().getFrom(),
                        parameters.timerange().getTo());
                return;
            }

            // Create single event from aggregation result
            Event event = createEventFromAggregation(eventFactory, result, parameters);
            Message message = convertAggregationToMessage(result, parameters);

            // Send single event
            List<EventWithContext> events = Collections.singletonList(
                    EventWithContext.create(event, message)
            );
            eventsConsumer.accept(events);

            LOG.debug("Created 1 event from MongoDB aggregation result: {} {}",
                    events.stream().findFirst().map(e -> e.event().getEventDefinitionType()).orElse("N/A"),
                    result.toJson());

            // Update processor state
            stateService.setState(eventDefinition.id(),
                    parameters.timerange().getFrom(),
                    parameters.timerange().getTo());

        } catch (Exception e) {
            final String errorMsg = String.format(Locale.ROOT,
                    "Failed to execute MongoDB aggregation for event definition <%s/%s>: %s",
                    eventDefinition.title(), eventDefinition.id(), e.getMessage());
            LOG.error(errorMsg, e);
            throw new EventProcessorException(errorMsg, false, eventDefinition, e);
        }
    }

    private List<Bson> buildAggregationPipeline(MongoDBEventProcessorParameters parameters) throws EventProcessorException {
        List<Bson> pipeline = new ArrayList<>();

        // Add time range filter as first stage
        Bson timeMatchStage = match(and(
                gte(config.timestampField(), parameters.timerange().getFrom().toDate()),
                lt(config.timestampField(), parameters.timerange().getTo().toDate())
        ));
        pipeline.add(timeMatchStage);

        // Parse and add user's pipeline stages
        if (config.aggregationPipeline() != null && !config.aggregationPipeline().trim().isEmpty()) {
            try {
                JsonArray userPipeline = JsonParser.parseString(config.aggregationPipeline()).getAsJsonArray();
                for (JsonElement stage : userPipeline) {
                    Document stageDoc = Document.parse(stage.toString());
                    pipeline.add(stageDoc);
                }
            } catch (Exception e) {
                LOG.error("Failed to parse aggregation pipeline: {}", config.aggregationPipeline(), e);
                throw new EventProcessorException("Invalid aggregation pipeline: " + e.getMessage(), true, eventDefinition, e);
            }
        }

        // Limit to 1 result (pipeline should naturally produce one result)
        pipeline.add(limit(1));

        return pipeline;
    }

    private Event createEventFromAggregation(EventFactory eventFactory,
                                            Document aggregationResult,
                                            MongoDBEventProcessorParameters parameters) {
        // Use end of time range as event timestamp
        DateTime timestamp = parameters.timerange().getTo();
        Event event = eventFactory.createEvent(eventDefinition, timestamp, eventDefinition.title());

        // Set timerange for the event
        event.setTimerangeStart(parameters.timerange().getFrom());
        event.setTimerangeEnd(parameters.timerange().getTo());

        // Set origin context - encodes the time range instead of document ID
        event.setOriginContext(EventOriginContext.mongodbAggregation(
                config.collectionName(),
                parameters.timerange().getFrom().toString(),
                parameters.timerange().getTo().toString()
        ));

        // Add aggregation result fields to event
        for (Map.Entry<String, Object> entry : aggregationResult.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // Skip _id from $group, but include all other fields
            if (!"_id".equals(key)) {
                event.setField(key, FieldValue.string(String.valueOf(convertMongoValue(value))));
            }
        }

        // Set replay info
        event.setReplayInfo(EventReplayInfo.builder()
                .timerangeStart(parameters.timerange().getFrom())
                .timerangeEnd(parameters.timerange().getTo())
                .query(config.aggregationPipeline())
                .build());

        return event;
    }

    private Message convertAggregationToMessage(Document aggregationResult,
                                               MongoDBEventProcessorParameters parameters) {
        // Use end of time range as message timestamp
        DateTime timestamp = parameters.timerange().getTo();

        Message message = messageFactory.createMessage(
                "MongoDB aggregation from " + config.collectionName() + ": " + aggregationResult.toJson(),
                "mongodb-event-processor",
                timestamp
        );

        // Add aggregation result fields
        Map<String, Object> fields = new HashMap<>();
        fields.put("aggregation_result", aggregationResult.toJson());
        fields.put("timerange_start", parameters.timerange().getFrom().toString());
        fields.put("timerange_end", parameters.timerange().getTo().toString());

        // Add individual aggregation fields
        for (Map.Entry<String, Object> entry : aggregationResult.entrySet()) {
            if (!"_id".equals(entry.getKey())) {
                fields.put(entry.getKey(), convertMongoValue(entry.getValue()));
            }
        }

        message.addFields(fields);
        return message;
    }

    private Object convertMongoValue(Object value) {
        // Handle DateTime (Joda) from TrafficDto
        if (value instanceof DateTime dateTime) {
            return dateTime.toString();
        }
        // Handle Date from MongoDB
        else if (value instanceof Date date) {
            return new DateTime(date.getTime(), org.joda.time.DateTimeZone.UTC).toString();
        }
        // Handle ObjectId
        else if (value instanceof org.bson.types.ObjectId) {
            return value.toString();
        }
        // Handle numeric types - ensure they're compatible with event fields
        else if (value instanceof Number) {
            return value;
        }
        // Handle other types
        else if (value != null) {
            return value.toString();
        }
        return null;
    }

    @Override
    public void sourceMessagesForEvent(Event event,
                                      Consumer<List<MessageSummary>> messageConsumer,
                                      long limit)
            throws EventProcessorException {
        LOG.debug("sourceMessagesForEvent not applicable for DB aggregation event processor");
    }
}
