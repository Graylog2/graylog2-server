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

import jakarta.inject.Inject;
import org.graylog2.database.MongoConnection;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class V20240316141613_ApplyNewFieldTypesToExisitingIndex extends Migration {
    private final MongoConnection mongoConnection;
    private final IndexSetRegistry indexSetRegistry;
    private final ClusterConfigService clusterConfigService;
    private final IndicesAdapter indicesAdapter;
    private final Indices indices;

    @Inject
    public V20240316141613_ApplyNewFieldTypesToExisitingIndex(MongoConnection mongoConnection,
                                                              IndexSetRegistry indexSetRegistry,
                                                              ClusterConfigService clusterConfigService,
                                                              IndicesAdapter indicesAdapter,
                                                              Indices indices) {
        this.mongoConnection = mongoConnection;
        this.indexSetRegistry = indexSetRegistry;
        this.clusterConfigService = clusterConfigService;
        this.indicesAdapter = indicesAdapter;
        this.indices = indices;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-03-16T14:16:13Z");
    }

    @Override
    public void upgrade() {

        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            return;
        }
//        indexSetRegistry.getAll().stream()
//                .filter(is -> is.getConfig().isRegularIndex()).forEach(indexSet ->
//                {
//                    Template template = indices.getIndexTemplate(indexSet);
//                    final String activeWriteIndex = indexSet.getActiveWriteIndex();
//                    indicesAdapter.updateIndexMapping(activeWriteIndex, "ignored", template.mappings() );
//
//                });


        final List<String> activeRegularWriteIndices = indexSetRegistry.getAll().stream()
                .filter(is -> is.getConfig().isRegularIndex())
                .map(IndexSet::getActiveWriteIndex).toList();

        final Map<String, Object> mapppings =
                Map.of("mappings",
                        Map.of("properties",
                                Map.of(
                                        Message.FIELD_GL2_PROCESSING_DURATION_MS,
                                        Map.of("type", "integer")
                                )
                        )
                );
        final Map<String, Object> mapping = Map.of();

        activeRegularWriteIndices.forEach(i -> {
            try {
                indicesAdapter.updateIndexMapping(i, "ignored", mapping);
            } catch (Exception e) {
                ; // ignore
            }
        });
        clusterConfigService.write(new MigrationCompleted(activeRegularWriteIndices));
    }

    public record MigrationCompleted(List<String> updatedIndices) {}
}
