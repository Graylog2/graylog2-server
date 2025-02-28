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
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.NonDeletableSystemScope;
import org.graylog2.database.entities.ScopedEntity;
import org.graylog2.indexer.indexset.IndexSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Objects;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog.events.processor.DBEventDefinitionService.COLLECTION_NAME;
import static org.graylog.events.processor.DBEventDefinitionService.SYSTEM_NOTIFICATION_EVENT_DEFINITION;
import static org.graylog.events.processor.EventDefinitionDto.FIELD_TITLE;
import static org.graylog2.database.utils.MongoUtils.idEq;

/**
 * Make system index sets (regular==false) non-deletable by assigning NonDeletableSystemScope
 */
public class V20241212102900_ScopeMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20241212102900_ScopeMigration.class);

    private final IndexSetService indexSetService;
    private final MongoCollection<EventDefinitionDto> collection;

    @Inject
    public V20241212102900_ScopeMigration(final IndexSetService indexSetService,
                                          MongoCollections mongoCollections) {
        this.indexSetService = indexSetService;
        this.collection = mongoCollections.collection(COLLECTION_NAME, EventDefinitionDto.class);
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
        indexSetService.findAll().forEach(indexSetConfig -> {
            if (!indexSetConfig.isRegularIndex() && !indexSetConfig.scope().equalsIgnoreCase(NonDeletableSystemScope.NAME)) {
                indexSetService.save(indexSetConfig.toBuilder().scope(NonDeletableSystemScope.NAME).build());
                LOG.info("Successfully updated scope for index set: {}", indexSetConfig.title());
            }
        });
    }

    private void updateSystemEventScopes() {
        collection.find(
                        Filters.or(
                                eq(FIELD_TITLE, SYSTEM_NOTIFICATION_EVENT_DEFINITION),
                                eq(ScopedEntity.FIELD_SCOPE, "SYSTEM_NOTIFICATION_EVENT")))
                .forEach(eventDefinition -> {
                    if (!eventDefinition.scope().equalsIgnoreCase(NonDeletableSystemScope.NAME)) {
                        final EventDefinitionDto entity = eventDefinition.toBuilder().scope(NonDeletableSystemScope.NAME).build();
                        collection.replaceOne(idEq(Objects.requireNonNull(entity.id())), entity);
                        LOG.info("Successfully updated scope for event definition: {}", entity.title());
                    }
                });
    }
}
