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
package org.graylog.collectors;

import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.MongoException;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.SourceConfig;
import org.graylog.collectors.db.SourceDTO;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.database.utils.MongoUtils.stream;

@Singleton
public class SourceService {

    private static final Map<String, SearchQueryField> SEARCH_FIELD_MAPPING = Map.of(
            "name", SearchQueryField.create(SourceDTO.FIELD_NAME),
            "description", SearchQueryField.create(SourceDTO.FIELD_DESCRIPTION),
            "type", SearchQueryField.create("config.type")
    );
    private static final String COLLECTION_NAME = "collector_fleet_sources";

    private final MongoCollection<SourceDTO> collection;
    private final MongoPaginationHelper<SourceDTO> paginationHelper;
    private final SearchQueryParser searchQueryParser;
    private final FleetTransactionLogService txnLogService;

    @Inject
    public SourceService(MongoCollections mongoCollections, FleetTransactionLogService txnLogService) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, SourceDTO.class);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.searchQueryParser = new SearchQueryParser(SourceDTO.FIELD_NAME, SEARCH_FIELD_MAPPING);
        this.txnLogService = txnLogService;

        collection.createIndexes(List.of(
                new IndexModel(Indexes.ascending(SourceDTO.FIELD_FLEET_ID)),
                new IndexModel(
                        Indexes.compoundIndex(
                                Indexes.ascending(SourceDTO.FIELD_FLEET_ID),
                                Indexes.ascending(SourceDTO.FIELD_NAME)
                        ),
                        new IndexOptions().unique(true)
                )
        ));
    }

    public SearchQuery parseSearchQuery(String query) {
        return searchQueryParser.parse(query);
    }

    public PaginatedList<SourceDTO> findByFleet(String fleetId, SearchQuery searchQuery, int page, int perPage,
                                                String sortField, SortOrder order) {
        return paginationHelper
                .filter(Filters.and(searchQuery.toBson(), eq(SourceDTO.FIELD_FLEET_ID, fleetId)))
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }

    @MustBeClosed
    public Stream<SourceDTO> streamAllByFleet(String fleetId) {
        return stream(collection.find(eq(SourceDTO.FIELD_FLEET_ID, fleetId)));
    }

    public Optional<SourceDTO> get(String fleetId, String sourceId) {
        return Optional.ofNullable(collection.find(Filters.and(idEq(sourceId), eq(SourceDTO.FIELD_FLEET_ID, fleetId))).first());
    }

    public SourceDTO create(String fleetId, String name, @Nullable String description, boolean enabled, SourceConfig config) {
        config.validate();

        final SourceDTO source = SourceDTO.builder()
                .fleetId(fleetId)
                .name(name)
                .description(description)
                .enabled(enabled)
                .config(config)
                .build();

        try {
            final var result = collection.insertOne(source);
            final SourceDTO saved = source.toBuilder()
                    .id(insertedIdAsString(result))
                    .build();
            txnLogService.appendFleetMarker(fleetId, MarkerType.CONFIG_CHANGED);
            return saved;
        } catch (MongoException e) {
            if (MongoUtils.isDuplicateKeyError(e)) {
                throw new IllegalArgumentException(
                        "A source with name '" + name + "' already exists in fleet '" + fleetId + "'", e);
            }
            throw e;
        }
    }

    public Optional<SourceDTO> update(String fleetId, String sourceId, String name, @Nullable String description,
                                      boolean enabled, SourceConfig config) {
        return get(fleetId, sourceId).map(existing -> {
            config.validate();
            final SourceDTO updated = existing.toBuilder()
                    .name(name)
                    .description(description)
                    .enabled(enabled)
                    .config(config)
                    .build();
            try {
                collection.replaceOne(idEq(sourceId), updated);
            } catch (MongoException e) {
                if (MongoUtils.isDuplicateKeyError(e)) {
                    throw new IllegalArgumentException(
                            "A source with name '" + name + "' already exists in fleet '" + fleetId + "'", e);
                }
                throw e;
            }
            txnLogService.appendFleetMarker(fleetId, MarkerType.CONFIG_CHANGED);
            return updated;
        });
    }

    public boolean delete(String fleetId, String sourceId) {
        return get(fleetId, sourceId).map(source -> {
            boolean deleted = collection.deleteOne(idEq(sourceId)).getDeletedCount() > 0;
            if (deleted) {
                txnLogService.appendFleetMarker(fleetId, MarkerType.CONFIG_CHANGED);
            }
            return deleted;
        }).orElse(false);
    }

    public long deleteAllByFleet(String fleetId) {
        return deleteAllByFleet(fleetId, true);
    }

    public long count() {
        return collection.countDocuments();
    }

    public long countByFleet(String fleetId) {
        return collection.countDocuments(eq(SourceDTO.FIELD_FLEET_ID, fleetId));
    }

    public Map<String, Long> countByFleetGrouped() {
        final var pipeline = List.of(
                Aggregates.group("$" + SourceDTO.FIELD_FLEET_ID, Accumulators.sum("count", 1L))
        );
        final Map<String, Long> result = new HashMap<>();
        collection.aggregate(pipeline, Document.class).forEach(doc -> {
            final String fleetId = doc.getString("_id");
            if (fleetId != null) {
                result.put(fleetId, ((Number) doc.get("count")).longValue());
            }
        });
        return result;
    }

    public long deleteAllByFleet(String fleetId, boolean appendMarker) {
        long deletedCount = collection.deleteMany(eq(SourceDTO.FIELD_FLEET_ID, fleetId)).getDeletedCount();
        if (deletedCount > 0 && appendMarker) {
            txnLogService.appendFleetMarker(fleetId, MarkerType.CONFIG_CHANGED);
        }
        return deletedCount;
    }
}
