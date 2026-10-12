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
package org.graylog2.telemetry.pipelines;

import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog2.database.entities.DefaultEntityScope;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserPipelines {
    private UserPipelines() {
    }

    /**
     * IDs of user-defined pipelines. Illuminate pipelines load from the bundle rather than MongoDB, and system
     * pipelines use their own scopes, so only DEFAULT-scoped pipelines qualify.
     */
    public static Set<String> ids(PipelineService pipelineService) {
        return pipelineService.loadAll().stream()
                .filter(pipeline -> DefaultEntityScope.NAME.equals(pipeline.scope()))
                .map(PipelineDao::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
