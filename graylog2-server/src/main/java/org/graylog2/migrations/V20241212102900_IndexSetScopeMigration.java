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
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.SystemIndexSetScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Make system index sets (regular==false) non-deletable by assigning SystemIndexSetScope
 */
public class V20241212102900_IndexSetScopeMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20241212102900_IndexSetScopeMigration.class);

    private final IndexSetService indexSetService;

    @Inject
    public V20241212102900_IndexSetScopeMigration(final IndexSetService indexSetService) {
        this.indexSetService = indexSetService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-12-12T10:29:00Z");
    }

    @Override
    public void upgrade() {
        indexSetService.findAll().forEach(indexSetConfig -> {
            if (!indexSetConfig.isRegularIndex() && !indexSetConfig.scope().equalsIgnoreCase(SystemIndexSetScope.NAME)) {
                indexSetService.save(indexSetConfig.toBuilder().scope(SystemIndexSetScope.NAME).build());
                LOG.info("Successfully updated scope for index set: {}", indexSetConfig.title());
            }
        });
    }

}
