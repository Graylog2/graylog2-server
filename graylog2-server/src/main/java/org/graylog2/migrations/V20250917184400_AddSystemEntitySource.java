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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.NonDeletableSystemScope;
import org.graylog2.database.entities.source.DBEntitySourceService;
import org.graylog2.database.entities.source.EntitySource;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.graylog2.contentpacks.model.ModelTypes.DASHBOARD_V1;
import static org.graylog2.contentpacks.model.ModelTypes.DASHBOARD_V2;
import static org.graylog2.contentpacks.model.ModelTypes.SEARCH_V1;
import static org.graylog2.database.utils.MongoUtils.stream;

public class V20250917184400_AddSystemEntitySource extends Migration {


    private final ClusterConfigService configService;
    private final MongoCollection<EventDefinitionDto> eventDefinitionCollection;
    private final MongoCollection<EntitySource> entitySourceCollection;
    private final com.mongodb.client.MongoCollection<ContentPackInstallation> contentPackCollection;

    @Inject
    public V20250917184400_AddSystemEntitySource(ClusterConfigService configService,
                                                 MongoCollections mongoCollections) {
        this.configService = configService;
        this.eventDefinitionCollection = mongoCollections.collection(DBEventDefinitionService.COLLECTION_NAME, EventDefinitionDto.class);
        this.entitySourceCollection = mongoCollections.collection(DBEntitySourceService.COLLECTION_NAME, EntitySource.class);
        this.contentPackCollection = mongoCollections.nonEntityCollection(ContentPackInstallationPersistenceService.COLLECTION_NAME, ContentPackInstallation.class);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-09-17T18:44:00Z");
    }

    @Override
    public void upgrade() {
        if (migrationAlreadyApplied()) {
            return;
        }

        insertSystemEntitySourceDocuments();

        markMigrationApplied();
    }

    private void insertSystemEntitySourceDocuments() {
        final List<InsertOneModel<EntitySource>> entitySourceRecords = new ArrayList<>();

        final V20230601104500_AddSourcesPageV2.MigrationCompleted sourcesMigration = configService.get(V20230601104500_AddSourcesPageV2.MigrationCompleted.class);
        if (sourcesMigration != null) {
            final String contentPackId = sourcesMigration.contentPackId();
            final ContentPackInstallation installation = contentPackCollection.find(Filters.eq(ContentPackInstallation.FIELD_CONTENT_PACK_ID, contentPackId)).first();
            if (installation != null) {
                entitySourceRecords.addAll(installation.entities().stream()
                        // Collect system-provided views from the content pack.
                        .filter(e -> e.type().equals(DASHBOARD_V1) || e.type().equals(DASHBOARD_V2) || e.type().equals(SEARCH_V1))
                        .map(e -> new InsertOneModel<>(EntitySource.builder()
                                .entityId(e.id().toString())
                                .entityType(EntitySource.VIEW_TYPE)
                                .source(EntitySource.SYSTEM)
                                .build()))
                        .collect(Collectors.toSet())
                );
            }
        }

        final Bson systemDefinitionFilter = Filters.eq(EventDefinitionDto.FIELD_SCOPE, NonDeletableSystemScope.NAME);
        try (var stream = stream(eventDefinitionCollection.find(systemDefinitionFilter))) {
            entitySourceRecords.addAll(stream
                    .map(dto -> new InsertOneModel<>(EntitySource.builder()
                            .entityId(dto.id())
                            .entityType(EntitySource.EVENT_DEFINITION_TYPE)
                            .source(EntitySource.SYSTEM)
                            .build()))
                    .toList()
            );
        }

        if (!entitySourceRecords.isEmpty()) {
            entitySourceCollection.bulkWrite(entitySourceRecords);
        }
    }

    private boolean migrationAlreadyApplied() {
        return Objects.nonNull(configService.get(MigrationCompleted.class));
    }

    private void markMigrationApplied() {
        configService.write(new MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
