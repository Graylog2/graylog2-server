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
package org.graylog2.streams.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.mongojack.Id;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedId;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;
import static org.graylog2.streams.filters.StreamDestinationFilterRuleDTO.FIELD_DESCRIPTION;
import static org.graylog2.streams.filters.StreamDestinationFilterRuleDTO.FIELD_DESTINATION_TYPE;
import static org.graylog2.streams.filters.StreamDestinationFilterRuleDTO.FIELD_STATUS;
import static org.graylog2.streams.filters.StreamDestinationFilterRuleDTO.FIELD_STREAM_ID;
import static org.graylog2.streams.filters.StreamDestinationFilterRuleDTO.FIELD_TITLE;

public class StreamDestinationFilterService {
    public static final String COLLECTION = "stream_destination_filters";

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(FIELD_TITLE, SearchQueryField.create(FIELD_TITLE))
            .put(FIELD_DESCRIPTION, SearchQueryField.create(FIELD_DESCRIPTION))
            .put(FIELD_DESTINATION_TYPE, SearchQueryField.create(FIELD_DESTINATION_TYPE))
            .put(FIELD_STATUS, SearchQueryField.create(FIELD_STATUS))
            .build();

    private final MongoCollection<StreamDestinationFilterRuleDTO> collection;
    private final MongoPaginationHelper<StreamDestinationFilterRuleDTO> paginationHelper;
    private final MongoUtils<StreamDestinationFilterRuleDTO> utils;
    private final ClusterEventBus clusterEventBus;
    private final Optional<DestinationFilterCreationValidator> optionalDestinationFilterCreationValidator;

    @Inject
    public StreamDestinationFilterService(MongoCollections mongoCollections,
                                          ClusterEventBus clusterEventBus,
                                          EventBus eventBus,
                                          Optional<DestinationFilterCreationValidator> optionalDestinationFilterCreationValidator) {
        this.collection = mongoCollections.collection(COLLECTION, StreamDestinationFilterRuleDTO.class);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.utils = mongoCollections.utils(collection);
        this.clusterEventBus = clusterEventBus;
        this.optionalDestinationFilterCreationValidator = optionalDestinationFilterCreationValidator;

        collection.createIndex(Indexes.ascending(FIELD_STREAM_ID));
        collection.createIndex(Indexes.ascending(FIELD_DESTINATION_TYPE));
        collection.createIndex(Indexes.ascending(FIELD_STATUS));

        eventBus.register(this);
    }

    private Bson parseQuery(String queryString) {
        final var queryParser = new SearchQueryParser(FIELD_TITLE, SEARCH_FIELD_MAPPING);
        return queryParser.parse(queryString).toBson();
    }

    public PaginatedList<StreamDestinationFilterRuleDTO> findPaginatedForStream(
            String streamId,
            String queryString,
            Bson sort,
            int perPage,
            int page,
            Predicate<String> permissionSelector
    ) {
        final var query = parseQuery(queryString);

        return paginationHelper.filter(and(eq(FIELD_STREAM_ID, streamId), query))
                .sort(sort)
                .perPage(perPage)
                .page(page, dto -> permissionSelector.test(dto.id()));
    }

    public PaginatedList<StreamDestinationFilterRuleDTO> findPaginatedForStreamAndTarget(
            String streamId,
            String targetId,
            String queryString,
            Bson sort,
            int perPage,
            int page,
            Predicate<String> permissionSelector
    ) {
        final var query = parseQuery(queryString);

        return paginationHelper.filter(and(eq(FIELD_STREAM_ID, streamId), eq(FIELD_DESTINATION_TYPE, targetId), query))
                .sort(sort)
                .perPage(perPage)
                .page(page, dto -> permissionSelector.test(dto.id()));
    }

    public Optional<StreamDestinationFilterRuleDTO> findByIdForStream(String streamId, String id) {
        collection.find(and(eq(FIELD_STREAM_ID, streamId), idEq(id)));
        return utils.getById(id);
    }

    public StreamDestinationFilterRuleDTO createForStream(String streamId, StreamDestinationFilterRuleDTO dto) {
        if (!isBlank(dto.id())) {
            throw new IllegalArgumentException("id must be blank");
        }

        optionalDestinationFilterCreationValidator.ifPresent(validator -> validator.validate(dto));

        // We don't want to allow the creation of a filter rule for a different stream, so we enforce the stream ID.
        final var dtoId = insertedId(collection.insertOne(dto.withStream(streamId)));
        clusterEventBus.post(StreamDestinationFilterUpdatedEvent.of(dtoId.toHexString()));
        return utils.getById(dtoId)
                .orElseThrow(() -> new IllegalArgumentException(f("Couldn't insert document: %s", dto)));
    }

    public StreamDestinationFilterRuleDTO updateForStream(String streamId, StreamDestinationFilterRuleDTO dto) {
        // We don't want to allow the creation of a filter rule for a different stream, so we enforce the stream ID.
        collection.replaceOne(
                and(eq(FIELD_STREAM_ID, streamId), idEq(requireNonBlank(dto.id(), "id can't be blank"))),
                dto.withStream(streamId)
        );
        clusterEventBus.post(StreamDestinationFilterUpdatedEvent.of(requireNonBlank(dto.id())));

        return utils.getById(dto.id())
                .orElseThrow(() -> new IllegalArgumentException(f("Couldn't find updated document: %s", dto)));
    }

    public StreamDestinationFilterRuleDTO deleteFromStream(String streamId, String id) {
        final var dto = utils.getById(id)
                .orElseThrow(() -> new IllegalArgumentException(f("Couldn't find document with ID <%s> for deletion", id)));

        if (collection.deleteOne(and(eq(FIELD_STREAM_ID, streamId), idEq(id))).getDeletedCount() > 0) {
            clusterEventBus.post(StreamDestinationFilterDeletedEvent.of(id));
        }

        return dto;
    }

    public record GroupByStreamResult(@JsonProperty("id") @Id String streamId,
                                      @JsonProperty("filters") Set<StreamDestinationFilterRuleDTO> filters) {}

    public void forEachEnabledFilterGroupedByStream(Consumer<GroupByStreamResult> consumer) {
        // Group all enabled filters by stream ID
        collection.aggregate(List.of(
                Aggregates.match(eq(FIELD_STATUS, StreamDestinationFilterRuleDTO.Status.ENABLED)),
                Aggregates.group("$" + FIELD_STREAM_ID, List.of(
                        Accumulators.push("filters", "$$ROOT")
                ))
        ), GroupByStreamResult.class).forEach(consumer);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleStreamDeleted(StreamDeletedEvent streamDeletedEvent) {
        collection.deleteMany(eq(FIELD_STREAM_ID, streamDeletedEvent.streamId()));
    }

}
