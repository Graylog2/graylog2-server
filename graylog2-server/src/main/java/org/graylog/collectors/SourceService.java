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

import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

@Singleton
public class SourceService {

    private static final Map<String, SearchQueryField> SEARCH_FIELD_MAPPING = Map.of(
            "name", SearchQueryField.create(SourceDTO.FIELD_NAME),
            "description", SearchQueryField.create(SourceDTO.FIELD_DESCRIPTION),
            "type", SearchQueryField.create("config.type")
    );

    private final MongoCollection<SourceDTO> collection;
    private final MongoPaginationHelper<SourceDTO> paginationHelper;
    private final SearchQueryParser searchQueryParser;
    private final FleetTransactionLogService txnLogService;

    @Inject
    public SourceService(MongoCollections mongoCollections, FleetTransactionLogService txnLogService) {
        this.collection = mongoCollections.collection("fleet_sources", SourceDTO.class);
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
                                                 String sortField, SortOrder order,
                                                 Predicate<SourceDTO> permissionFilter) {
        return paginationHelper
                .filter(Filters.and(searchQuery.toBson(), Filters.eq(SourceDTO.FIELD_FLEET_ID, fleetId)))
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page, permissionFilter);
    }

    public List<SourceDTO> listAllByFleet(String fleetId) {
        return collection.find(Filters.eq(SourceDTO.FIELD_FLEET_ID, fleetId)).into(new ArrayList<>());
    }

    public Optional<SourceDTO> get(String fleetId, String sourceId) {
        return Optional.ofNullable(collection.find(idEq(sourceId)).first())
                .filter(source -> source.fleetId().equals(fleetId));
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
        return collection.countDocuments(Filters.eq(SourceDTO.FIELD_FLEET_ID, fleetId));
    }

    public long deleteAllByFleet(String fleetId, boolean appendMarker) {
        long deletedCount = collection.deleteMany(Filters.eq(SourceDTO.FIELD_FLEET_ID, fleetId)).getDeletedCount();
        if (deletedCount > 0 && appendMarker) {
            txnLogService.appendFleetMarker(fleetId, MarkerType.CONFIG_CHANGED);
        }
        return deletedCount;
    }
}
