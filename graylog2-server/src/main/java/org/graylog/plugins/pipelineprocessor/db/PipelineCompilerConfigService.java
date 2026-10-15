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
package org.graylog.plugins.pipelineprocessor.db;

import org.graylog.plugins.pipelineprocessor.events.PipelineCompilerConfigChangedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;

import jakarta.inject.Inject;

public class PipelineCompilerConfigService {
    private final ClusterConfigService clusterConfigService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public PipelineCompilerConfigService(ClusterConfigService clusterConfigService, ClusterEventBus clusterEventBus) {
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = clusterEventBus;
    }

    public PipelineCompilerConfigDto save(PipelineCompilerConfigDto config) {
        clusterConfigService.write(config);
        clusterEventBus.post(PipelineCompilerConfigChangedEvent.create(config.enabled()));
        return get();
    }

    public PipelineCompilerConfigDto get() {
        return clusterConfigService.getOrDefault(PipelineCompilerConfigDto.class, PipelineCompilerConfigDto.createDefault());
    }
}
