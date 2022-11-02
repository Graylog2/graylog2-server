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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import one.util.streamex.EntryStream;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class V20220930095323_MigratePivotLimitsInSearches extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20220930095323_MigratePivotLimitsInSearches.class);

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> searches;

    @Inject
    public V20220930095323_MigratePivotLimitsInSearches(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.searches = mongoConnection.getMongoDatabase().getCollection("searches");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-09-30T09:53:23Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }

        final List<SearchPivotLimitMigration> pivotLimitMigrations = new ArrayList<>();
        for (Document document : this.searches.find()) {
            final String searchId = document.get("_id", ObjectId.class).toHexString();
            final List<Document> queries = document.get("queries", Collections.emptyList());
            EntryStream.of(queries)
                    .forEach(entry -> {
                        final Integer queryIndex = entry.getKey();
                        final List<Document> searchTypes = entry.getValue().get("search_types", Collections.emptyList());
                        EntryStream.of(searchTypes)
                                .filter(searchType -> "pivot".equals(searchType.getValue().getString("type")))
                                .forEach(searchTypeEntry -> {
                                    final Document searchType = searchTypeEntry.getValue();
                                    final Integer searchTypeIndex = searchTypeEntry.getKey();
                                    final List<Document> rowPivots = searchType.get("row_groups", Collections.emptyList());
                                    final Optional<Integer> maxRowPivotLimit = extractMaxLimit(rowPivots);
                                    final List<Document> columnPivots = searchType.get("column_groups", Collections.emptyList());
                                    final Optional<Integer> maxColumnPivotLimit = extractMaxLimit(columnPivots);

                                    if (searchTypeIndex != null && (maxRowPivotLimit.isPresent() || maxColumnPivotLimit.isPresent())) {
                                        pivotLimitMigrations.add(new SearchPivotLimitMigration(searchId, queryIndex, searchTypeIndex, maxRowPivotLimit, maxColumnPivotLimit));
                                    }
                                });
                    });
        }

        final List<WriteModel<Document>> operations = pivotLimitMigrations.stream()
                .flatMap(pivotMigration -> {
                    final ImmutableList.Builder<WriteModel<Document>> builder = ImmutableList.builder();
                    pivotMigration.rowLimit().ifPresent(rowLimit -> {
                        builder.add(updateSearch(pivotMigration.searchId(),
                                new Document("$set", new Document(pivotPath(pivotMigration) + ".row_limit", rowLimit))
                        ));
                        builder.add(updateSearch(pivotMigration.searchId(),
                                new Document("$unset", new Document(pivotPath(pivotMigration) + ".row_groups.$[].limit", 1))
                        ));
                    });
                    pivotMigration.columnLimit().ifPresent(columnLimit -> {
                        builder.add(updateSearch(pivotMigration.searchId(),
                                new Document("$set", new Document(pivotPath(pivotMigration) + ".column_limit", columnLimit))
                        ));
                        builder.add(updateSearch(pivotMigration.searchId(),
                                new Document("$unset", new Document(pivotPath(pivotMigration) + ".column_groups.$[].limit", 1))
                        ));
                    });
                    return builder.build().stream();
                })
                .collect(Collectors.toList());

        if (!operations.isEmpty()) {
            searches.bulkWrite(operations);
        }

        clusterConfigService.write(new MigrationCompleted(pivotLimitMigrations.size()));
    }
    private String pivotPath(SearchPivotLimitMigration pivotMigration) {
        return "queries." + pivotMigration.queryIndex() + ".search_types." + pivotMigration.searchTypeIndex();
    }

    private WriteModel<Document> updateSearch(String searchId, Document update) {
        return new UpdateOneModel<>(
                new Document("_id", new ObjectId(searchId)),
                update,
                new UpdateOptions().upsert(false)
        );
    }

    private Optional<Integer> extractMaxLimit(List<Document> groups) {
        return groups.stream()
                .filter(group -> "values".equals(group.get("type")))
                .map(group -> group.getInteger("limit"))
                .filter(Objects::nonNull)
                .max(Integer::compare);
    }

    public record SearchPivotLimitMigration(String searchId, Integer queryIndex, Integer searchTypeIndex, Optional<Integer> rowLimit, Optional<Integer> columnLimit) {}

    public record MigrationCompleted(@JsonProperty("migrated_search_types") Integer migratedSearchTypes) {}
}
