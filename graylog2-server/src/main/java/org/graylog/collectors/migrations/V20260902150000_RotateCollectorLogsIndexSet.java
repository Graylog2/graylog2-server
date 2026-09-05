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
package org.graylog.collectors.migrations;

import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog.collectors.CollectorLogsDestinationService;
import org.graylog.collectors.indexer.CollectorLogsIndexTemplateProvider;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSet;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

import static org.graylog2.indexer.indexset.fields.IndexPrefixField.FIELD_INDEX_PREFIX;
import static org.graylog2.indexer.indexset.fields.IndexTemplateTypeField.FIELD_INDEX_TEMPLATE_TYPE;

/**
 * Rotates the collector system logs index set once, so that the reworked index mapping (GIM field
 * names, typed counters, and the unparsed {@code collector_log_attributes} object) takes effect
 * immediately instead of at the next regular rotation.
 * <p>
 * Without the rotation, the pre-existing write index would dynamically map the attributes object,
 * creating a mapping entry per attribute key (an unbounded set) and rejecting documents on leaf
 * type conflicts or once the index field limit is reached.
 * <p>
 * Unlike most migrations, a failure does not abort server startup: ingestion into the old index
 * keeps working (with the degraded mapping), so the migration only logs the error and retries on
 * the next startup by not persisting its completion marker.
 */
public class V20260902150000_RotateCollectorLogsIndexSet extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260902150000_RotateCollectorLogsIndexSet.class);

    private final ClusterConfigService clusterConfigService;
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory mongoIndexSetFactory;

    @Inject
    public V20260902150000_RotateCollectorLogsIndexSet(ClusterConfigService clusterConfigService,
                                                       IndexSetService indexSetService,
                                                       MongoIndexSet.Factory mongoIndexSetFactory) {
        this.clusterConfigService = clusterConfigService;
        this.indexSetService = indexSetService;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-09-02T15:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final var indexSetConfig = indexSetService.findOne(Filters.and(
                Filters.eq(FIELD_INDEX_TEMPLATE_TYPE, CollectorLogsIndexTemplateProvider.COLLECTOR_LOGS_TEMPLATE_TYPE),
                Filters.eq(FIELD_INDEX_PREFIX, CollectorLogsDestinationService.COLLECTOR_LOGS_INDEX_PREFIX)));

        if (indexSetConfig.isEmpty()) {
            // The collector feature has not been used yet. New indices are created from the
            // current template anyway, so there is nothing to rotate.
            clusterConfigService.write(new MigrationCompleted(false));
            return;
        }

        try {
            mongoIndexSetFactory.create(indexSetConfig.get()).cycle();
            clusterConfigService.write(new MigrationCompleted(true));
            LOG.info("Rotated collector system logs index set to apply the current index mapping.");
        } catch (Exception e) {
            LOG.error("Failed to rotate collector system logs index set, will retry on next startup. "
                    + "Until then, system log messages may be indexed with a degraded mapping.", e);
        }
    }

    public record MigrationCompleted(boolean rotated) {}
}
