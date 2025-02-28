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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.NonDeletableSystemScope;
import org.graylog2.database.entities.ScopedEntity;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Objects;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog.events.processor.DBEventDefinitionService.SYSTEM_NOTIFICATION_EVENT_DEFINITION;
import static org.graylog.events.processor.EventDefinitionDto.FIELD_TITLE;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_REGULAR;

/**
 * Make system index sets (regular==false) non-deletable by assigning NonDeletableSystemScope
 */
public class V20241212102900_ScopeMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20241212102900_ScopeMigration.class);

    private final MongoCollection<EventDefinitionDto> eventDefinitionCollection;
    private final MongoCollection<IndexSetConfig> indexSetCollection;

    @Inject
    public V20241212102900_ScopeMigration(MongoCollections mongoCollections) {
        this.indexSetCollection = mongoCollections.collection(MongoIndexSetService.COLLECTION_NAME, IndexSetConfig.class);
        this.eventDefinitionCollection = mongoCollections.collection(DBEventDefinitionService.COLLECTION_NAME, EventDefinitionDto.class);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-12-12T10:29:00Z");
    }

    @Override
    public void upgrade() {
        updateSystemIndexScope();
        updateSystemEventScopes();
    }

    private void updateSystemIndexScope() {
        indexSetCollection.find(
                        Filters.and(
                                Filters.eq(FIELD_REGULAR, false),
                                Filters.not(Filters.eq(ScopedEntity.FIELD_SCOPE, NonDeletableSystemScope.NAME))))
                .forEach(indexSetConfig -> {
                    final IndexSetConfig newIndexSetConfig = indexSetConfig.toBuilder().scope(NonDeletableSystemScope.NAME).build();
                    indexSetCollection.replaceOne(idEq(Objects.requireNonNull(indexSetConfig.id())), newIndexSetConfig);
                    LOG.info("Successfully updated scope for index set: {}", newIndexSetConfig.title());
                });
    }

    private void updateSystemEventScopes() {
        eventDefinitionCollection.find(
                        Filters.or(
                                eq(FIELD_TITLE, SYSTEM_NOTIFICATION_EVENT_DEFINITION),
                                eq(ScopedEntity.FIELD_SCOPE, "SYSTEM_NOTIFICATION_EVENT")))
                .forEach(eventDefinition -> {
                    if (!eventDefinition.scope().equalsIgnoreCase(NonDeletableSystemScope.NAME)) {
                        final EventDefinitionDto entity = eventDefinition.toBuilder().scope(NonDeletableSystemScope.NAME).build();
                        eventDefinitionCollection.replaceOne(idEq(Objects.requireNonNull(entity.id())), entity);
                        LOG.info("Successfully updated scope for event definition: {}", entity.title());
                    }
                });
    }
}
