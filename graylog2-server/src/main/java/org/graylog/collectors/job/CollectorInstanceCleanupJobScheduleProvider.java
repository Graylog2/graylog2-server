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
package org.graylog.collectors.job;

import jakarta.inject.Inject;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.scheduler.JobSchedule;
import org.graylog.scheduler.system.SystemJobScheduleProvider;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.Optional;

public class CollectorInstanceCleanupJobScheduleProvider implements SystemJobScheduleProvider<CollectorInstanceCleanupJob.Config> {
    private final ClusterConfigService clusterConfigService;

    @Inject
    public CollectorInstanceCleanupJobScheduleProvider(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public Optional<JobSchedule> getSchedule() throws Exception {
        final var config = clusterConfigService.get(CollectorsConfig.class);

        // Get schedule from config

        return Optional.empty();
    }

    @Override
    public CollectorInstanceCleanupJob.Config getConfig() {
        return null;
    }
}
