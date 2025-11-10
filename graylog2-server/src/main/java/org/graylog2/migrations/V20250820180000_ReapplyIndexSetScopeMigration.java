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
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.NonDeletableSystemScope;
import org.graylog2.database.entities.ScopedEntity;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Objects;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.indexer.indexset.IndexSetConfig.FIELD_REGULAR;

/**
 * Make system index sets (regular==false) non-deletable by assigning NonDeletableSystemScope - again. This migration
 * was originally applied in {@link V20250304102900_ScopeMigration} but could have potentially been modified when scope
 * modification was not blocked. That issue was resolved in
 * <a href="https://github.com/Graylog2/graylog2-server/pull/23413">this PR.</a> The bug did not affect the system
 * event definitions so they have been omitted from this migration.
 */
public class V20250820180000_ReapplyIndexSetScopeMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20250820180000_ReapplyIndexSetScopeMigration.class);

    private final ClusterConfigService configService;
    private final MongoCollection<IndexSetConfig> indexSetCollection;

    @Inject
    public V20250820180000_ReapplyIndexSetScopeMigration(ClusterConfigService configService, MongoCollections mongoCollections) {
        this.configService = configService;
        this.indexSetCollection = mongoCollections.collection(MongoIndexSetService.COLLECTION_NAME, IndexSetConfig.class);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-08-20T18:00:00Z");
    }

    @Override
    public void upgrade() {
        if (migrationAlreadyApplied()) {
            return;
        }
        updateSystemIndexScope();
        markMigrationApplied();
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

    private boolean migrationAlreadyApplied() {
        return Objects.nonNull(configService.get(MigrationCompleted.class));
    }

    private void markMigrationApplied() {
        configService.write(new MigrationCompleted());
    }

    // Just a marker class to indicate that the migration has been applied
    public record MigrationCompleted() {}
}
