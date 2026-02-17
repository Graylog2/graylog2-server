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
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.db.FleetConfig;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.MarkerType;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

@Singleton
public class FleetService {

    private static final Map<String, SearchQueryField> SEARCH_FIELD_MAPPING = Map.of(
            "name", SearchQueryField.create(FleetDTO.FIELD_NAME),
            "description", SearchQueryField.create(FleetDTO.FIELD_DESCRIPTION)
    );

    private final MongoCollection<FleetDTO> collection;
    private final MongoPaginationHelper<FleetDTO> paginationHelper;
    private final SearchQueryParser searchQueryParser;
    private final FleetTransactionLogService txnLogService;
    private final SourceService sourceService;

    @Inject
    public FleetService(MongoCollections mongoCollections, FleetTransactionLogService txnLogService,
                        SourceService sourceService) {
        this.collection = mongoCollections.collection("fleets", FleetDTO.class);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.searchQueryParser = new SearchQueryParser(FleetDTO.FIELD_NAME, SEARCH_FIELD_MAPPING);
        this.txnLogService = txnLogService;
        this.sourceService = sourceService;

        collection.createIndexes(List.of(
                new IndexModel(Indexes.ascending(FleetDTO.FIELD_NAME), new IndexOptions().unique(true))
        ));
    }

    public SearchQuery parseSearchQuery(String query) {
        return searchQueryParser.parse(query);
    }

    public PaginatedList<FleetDTO> findPaginated(SearchQuery searchQuery, int page, int perPage,
                                                  String sortField, SortOrder order,
                                                  Predicate<FleetDTO> permissionFilter) {
        return paginationHelper
                .filter(searchQuery.toBson())
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page, permissionFilter);
    }

    public Optional<FleetDTO> get(String fleetId) {
        return Optional.ofNullable(collection.find(idEq(fleetId)).first());
    }

    public FleetDTO create(String name, String description, @Nullable String targetVersion) {
        final Instant now = Instant.now();
        final FleetDTO fleet = FleetDTO.builder()
                .name(name)
                .description(description)
                .targetVersion(targetVersion)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            final var result = collection.insertOne(fleet);
            final FleetDTO saved = fleet.toBuilder()
                    .id(insertedIdAsString(result))
                    .build();
            txnLogService.appendFleetMarker(saved.id(), MarkerType.CONFIG_CHANGED);
            return saved;
        } catch (MongoException e) {
            if (MongoUtils.isDuplicateKeyError(e)) {
                throw new IllegalArgumentException("A fleet with name '" + name + "' already exists", e);
            }
            throw e;
        }
    }

    public Optional<FleetDTO> update(String fleetId, String name, String description, @Nullable String targetVersion) {
        return get(fleetId).map(existing -> {
            final FleetDTO updated = existing.toBuilder()
                    .name(name)
                    .description(description)
                    .targetVersion(targetVersion)
                    .updatedAt(Instant.now())
                    .build();
            try {
                collection.replaceOne(idEq(fleetId), updated);
            } catch (MongoException e) {
                if (MongoUtils.isDuplicateKeyError(e)) {
                    throw new IllegalArgumentException("A fleet with name '" + name + "' already exists", e);
                }
                throw e;
            }
            txnLogService.appendFleetMarker(fleetId, MarkerType.CONFIG_CHANGED);
            return updated;
        });
    }

    public boolean delete(String fleetId) {
        sourceService.deleteAllByFleet(fleetId, false);
        final boolean deleted = collection.deleteOne(idEq(fleetId)).getDeletedCount() > 0;
        if (deleted) {
            txnLogService.appendFleetMarker(fleetId, MarkerType.CONFIG_CHANGED);
        }
        return deleted;
    }

    public Optional<FleetConfig> assembleConfig(String fleetId) {
        return get(fleetId).map(fleet -> {
            var sources = sourceService.listAllByFleet(fleetId);
            return new FleetConfig(fleet, sources);
        });
    }
}
