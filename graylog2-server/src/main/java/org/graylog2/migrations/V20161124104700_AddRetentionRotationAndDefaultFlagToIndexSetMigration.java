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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

public class V20161124104700_AddRetentionRotationAndDefaultFlagToIndexSetMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161124104700_AddRetentionRotationAndDefaultFlagToIndexSetMigration.class);

    private final ClusterConfigService clusterConfigService;
    private final IndexSetService indexSetService;

    @Inject
    public V20161124104700_AddRetentionRotationAndDefaultFlagToIndexSetMigration(final IndexSetService indexSetService,
                                                                                 final ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.indexSetService = indexSetService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-11-24T10:47:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }

        final ImmutableSet.Builder<String> updatedIds = ImmutableSet.builder();
        final ImmutableSet.Builder<String> skippedIds = ImmutableSet.builder();

        final IndexManagementConfig indexManagementConfig = clusterConfigService.get(IndexManagementConfig.class);

        checkState(indexManagementConfig != null, "Couldn't find index management configuration");

        for (final IndexSetConfig indexSetConfig : indexSetService.findAll()) {
            final IndexSetConfig.Builder updated = indexSetConfig.toBuilder();

            if (isNullOrEmpty(indexSetConfig.rotationStrategyClass())) {
                // Paranoia
                checkState(indexSetConfig.rotationStrategy().type().startsWith(indexManagementConfig.rotationStrategy()),
                        "rotation strategy config type <%s> does not match rotation strategy <%s>",
                        indexSetConfig.rotationStrategy().type(), indexManagementConfig.rotationStrategy());

                LOG.info("Adding rotation_strategy_class <{}> to index set <{}>", indexManagementConfig.rotationStrategy(), indexSetConfig.id());
                updated.rotationStrategyClass(indexManagementConfig.rotationStrategy());
            }
            if (isNullOrEmpty(indexSetConfig.retentionStrategyClass())) {
                // Paranoia
                checkState(indexSetConfig.retentionStrategy().type().startsWith(indexManagementConfig.retentionStrategy()),
                        "retention strategy config type <%s> does not match retention strategy <%s>",
                        indexSetConfig.retentionStrategy().type(), indexManagementConfig.retentionStrategy());

                LOG.info("Adding retention_strategy_class <{}> to index set <{}>", indexManagementConfig.retentionStrategy(), indexSetConfig.id());
                updated.retentionStrategyClass(indexManagementConfig.retentionStrategy());
            }

            if (!indexSetConfig.equals(updated.build())) {
                indexSetService.save(updated.build());

                updatedIds.add(Optional.ofNullable(indexSetConfig.id()).orElseThrow(() -> new IllegalStateException("no id??")));
            } else {
                skippedIds.add(Optional.ofNullable(indexSetConfig.id()).orElseThrow(() -> new IllegalStateException("no id??")));
            }
        }

        // Mark the oldest index set (there should be only one at this point, though) as default.
        final IndexSetConfig defaultIndexSetConfig = indexSetService.findAll().stream()
                .sorted(Comparator.comparing(IndexSetConfig::creationDate))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find any index set - this should not happen!"));

        LOG.info("Setting index set <{}> as default", defaultIndexSetConfig.id());
        clusterConfigService.write(DefaultIndexSetConfig.create(defaultIndexSetConfig.id()));

        clusterConfigService.write(MigrationCompleted.create(updatedIds.build(), skippedIds.build(), defaultIndexSetConfig.id()));
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("updated_index_set_ids")
        public abstract Set<String> updatedIndexSetIds();

        @JsonProperty("skipped_index_set_ids")
        public abstract Set<String> skippedIndexSetIds();

        @JsonProperty("default_index_set")
        public abstract String defaultIndexSet();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("updated_index_set_ids") Set<String> updatedIndexSetIds,
                                                @JsonProperty("skipped_index_set_ids") Set<String> skippedIndexSetIds,
                                                @JsonProperty("default_index_set") String defaultIndexSet) {
            return new AutoValue_V20161124104700_AddRetentionRotationAndDefaultFlagToIndexSetMigration_MigrationCompleted(updatedIndexSetIds, skippedIndexSetIds, defaultIndexSet);
        }
    }
}
