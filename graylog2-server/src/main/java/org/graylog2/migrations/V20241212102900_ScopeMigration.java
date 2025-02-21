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
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.NonDeletableSystemScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Make system index sets (regular==false) non-deletable by assigning NonDeletableSystemScope
 */
public class V20241212102900_ScopeMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20241212102900_ScopeMigration.class);

    private final IndexSetService indexSetService;
    private final DBEventDefinitionService dbService;

    @Inject
    public V20241212102900_ScopeMigration(final IndexSetService indexSetService,
                                          DBEventDefinitionService dbService) {
        this.indexSetService = indexSetService;
        this.dbService = dbService;
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
        try (java.util.stream.Stream<EventDefinitionDto> eventDefinitionStream = dbService.streamSystemEventDefinitions()) {
            eventDefinitionStream.forEach(eventDefinition -> {
                if (!eventDefinition.scope().equalsIgnoreCase(NonDeletableSystemScope.NAME)) {
                    dbService.save(eventDefinition.toBuilder().scope(NonDeletableSystemScope.NAME).build());
                    LOG.info("Successfully updated scope for event definition: {}", eventDefinition.title());
                }
            });
        }
    }
}
