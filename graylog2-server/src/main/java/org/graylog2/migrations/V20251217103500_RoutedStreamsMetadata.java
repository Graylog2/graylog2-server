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
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Migration to add routed_streams field to the pipeline metadata collections
 */
public class V20251217103500_RoutedStreamsMetadata extends Migration {
    private final ClusterConfigService configService;
    private final V20251021083100_CreatePipelineMetadata createPipelineMetadata;

    @Inject
    public V20251217103500_RoutedStreamsMetadata(ClusterConfigService configService,
                                                 V20251021083100_CreatePipelineMetadata createPipelineMetadata) {
        this.configService = configService;
        this.createPipelineMetadata = createPipelineMetadata;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-12-17T10:35:00Z");
    }

    @Override
    public void upgrade() {
        if (migrationAlreadyApplied()) {
            return;
        }

        // force a rerun of metadata creation to add routed_streams field
        createPipelineMetadata.doUpgrade();

        // Migration marker is set from V20251021083100_CreatePipelineMetadata
    }

    private boolean migrationAlreadyApplied() {
        return Objects.nonNull(configService.get(V20251217103500_RoutedStreamsMetadata.MigrationCompleted.class));
    }

    public record MigrationCompleted() {}
}
