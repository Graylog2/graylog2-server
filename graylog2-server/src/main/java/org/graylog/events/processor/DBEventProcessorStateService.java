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
package org.graylog.events.processor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog.events.processor.EventProcessorStateDto.FIELD_EVENT_DEFINITION_ID;
import static org.graylog.events.processor.EventProcessorStateDto.FIELD_MAX_PROCESSED_TIMESTAMP;
import static org.graylog.events.processor.EventProcessorStateDto.FIELD_MIN_PROCESSED_TIMESTAMP;

/**
 * Manages database state for {@link EventProcessor}s.
 */
public class DBEventProcessorStateService {
    private static final Logger LOG = LoggerFactory.getLogger(DBEventProcessorStateService.class);
    private static final String COLLECTION_NAME = "event_processor_state";

    private final MongoCollection<EventProcessorStateDto> collection;

    @Inject
    public DBEventProcessorStateService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection(COLLECTION_NAME, EventProcessorStateDto.class);

        // There should only be one state document for each event processor.
        collection.createIndex(Indexes.ascending(FIELD_EVENT_DEFINITION_ID), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending(FIELD_MIN_PROCESSED_TIMESTAMP));
        collection.createIndex(Indexes.ascending(FIELD_MAX_PROCESSED_TIMESTAMP));
    }

    /**
     * Loads the event processor state for the given event definition ID.
     *
     * @param eventDefinitionId the event definition ID to load the state for
     * @return filled optional with the state object if one exists, empty optional otherwise
     */
    @VisibleForTesting
    Optional<EventProcessorStateDto> findByEventDefinitionId(String eventDefinitionId) {
        checkArgument(!isNullOrEmpty(eventDefinitionId), "eventDefinitionId cannot be null or empty");

        return Optional.ofNullable(collection.find(Filters.eq(FIELD_EVENT_DEFINITION_ID, eventDefinitionId)).first());
    }

    /**
     * Returns a set of event processor state objects where the event definition ID matches one of the ones in the given
     * set of IDs and the maximum processed timestamp is greater or equals the given timestamp.
     *
     * @param eventDefinitionIds set of event definition IDs
     * @param maxTimestamp       the threshold for the maximum processed timestamp
     * @return set of state objects or empty set when no object matches
     */
    public ImmutableSet<EventProcessorStateDto> findByEventDefinitionsAndMaxTimestamp(Set<String> eventDefinitionIds, DateTime maxTimestamp) {
        checkArgument(eventDefinitionIds != null && !eventDefinitionIds.isEmpty(), "eventDefinitionIds cannot be null or empty");
        checkArgument(maxTimestamp != null, "maxTimestamp cannot be null");

        final Bson query = Filters.and(
                Filters.in(FIELD_EVENT_DEFINITION_ID, eventDefinitionIds),
                Filters.gte(FIELD_MAX_PROCESSED_TIMESTAMP, maxTimestamp)
        );

        return ImmutableSet.copyOf(collection.find(query));
    }

    /**
     * Creates a new or updates an existing event processor state record for the values in the given DTO.
     *
     * @param dto state DTO
     * @return the created or updated record
     */
    public Optional<EventProcessorStateDto> setState(EventProcessorStateDto dto) {
        return setState(dto.eventDefinitionId(), dto.minProcessedTimestamp(), dto.maxProcessedTimestamp());
    }

    /**
     * Creates a new or updates an existing event processor state record for the given values.
     *
     * @param eventDefinitionId     the related event definition ID
     * @param maxProcessedTimestamp the maximum processed timestamp
     * @return the created or updated record
     */
    public Optional<EventProcessorStateDto> setState(String eventDefinitionId,
                                                     DateTime minProcessedTimestamp,
                                                     DateTime maxProcessedTimestamp) {
        checkArgument(!isNullOrEmpty(eventDefinitionId), "eventDefinitionId cannot be null or empty");
        checkArgument(minProcessedTimestamp != null, "minProcessedTimestamp cannot be null");
        checkArgument(maxProcessedTimestamp != null, "maxProcessedTimestamp cannot be null");
        checkArgument(maxProcessedTimestamp.isAfter(minProcessedTimestamp), "minProcessedTimestamp must be older than maxProcessedTimestamp");

        LOG.debug("Update event processor state for <{}> with min processed timestamp of <{}> max processed timestamp of <{}>",
                eventDefinitionId, minProcessedTimestamp, maxProcessedTimestamp);

        // The state record must always keep the oldest minProcessedTimestamp and the newest maxProcessedTimestamp
        // regardless of the value min/max timestamp arguments.
        // Example: If the minProcessedTimestamp argument is newer than the value in the existing record, we don't
        // want to change it. The other way around for the maxProcessedTimestamp.
        // That's why we are using the $min and $max operations for the update query.
        final Bson update = Updates.combine(
                Updates.set(FIELD_EVENT_DEFINITION_ID, eventDefinitionId),
                Updates.min(FIELD_MIN_PROCESSED_TIMESTAMP, minProcessedTimestamp),
                Updates.max(FIELD_MAX_PROCESSED_TIMESTAMP, maxProcessedTimestamp));

        return Optional.ofNullable(collection.findOneAndUpdate(Filters.eq(FIELD_EVENT_DEFINITION_ID, eventDefinitionId),
                update, new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)));
    }

    /**
     * Delete state objects for the given event definition ID.
     *
     * @param id the object ID to delete
     * @return the number of objects that have been deleted
     */
    public int deleteByEventDefinitionId(String id) {
        return (int) collection.deleteOne(Filters.eq(FIELD_EVENT_DEFINITION_ID, id)).getDeletedCount();
    }
}
