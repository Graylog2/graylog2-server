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
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.time.ZonedDateTime;
import java.util.Objects;

import static org.graylog.plugins.pipelineprocessor.rest.PipelineResource.GL_INPUT_ROUTING_PIPELINE;

/**
 * Rename "All Messages Routing" pipeline to "Default Routing" to be consistent with index set and stream names.
 * TODO: Remove this migration in the next point release (6.3).
 */
public class V20250327120900_RenameDefaultPipeline extends Migration {
    private static final String OLD_PIPELINE_NAME = "All Messages Routing";

    private final PipelineService pipelineService;
    private final ClusterConfigService configService;

    @Inject
    public V20250327120900_RenameDefaultPipeline(
            PipelineService pipelineService,
            ClusterConfigService configService
    ) {
        this.pipelineService = pipelineService;
        this.configService = configService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-02-19T13:42:00Z");
    }

    @Override
    public void upgrade() {
        if (migrationAlreadyApplied()) {
            return;
        }

        PipelineDao dao;
        try {
            dao = pipelineService.loadByName(OLD_PIPELINE_NAME);
            final PipelineDao toSave = dao.toBuilder()
                    .title(GL_INPUT_ROUTING_PIPELINE)
                    .source(dao.source().replace(OLD_PIPELINE_NAME, GL_INPUT_ROUTING_PIPELINE))
                    .modifiedAt(DateTime.now(DateTimeZone.UTC))
                    .build();

            pipelineService.save(toSave, false);

        } catch (NotFoundException e) {
            // no pipeline - nothing to do
        }

        markMigrationApplied();
    }

    private boolean migrationAlreadyApplied() {
        return Objects.nonNull(configService.get(V20250327120900_RenameDefaultPipeline.MigrationCompleted.class));
    }

    private void markMigrationApplied() {
        this.configService.write(new V20250327120900_RenameDefaultPipeline.MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
