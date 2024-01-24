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
package org.graylog.plugins.views.storage.migration.state.persistence;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.Optional;

@Singleton
public class DatanodeMigrationConfigurationImpl implements DatanodeMigrationPersistence {

    private final ClusterConfigService clusterConfigService;

    @Inject
    public DatanodeMigrationConfigurationImpl(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public Optional<DatanodeMigrationConfiguration> getConfiguration() {
        return Optional.ofNullable(clusterConfigService.get(DatanodeMigrationConfiguration.class));
    }

    @Override
    public void saveConfiguration(DatanodeMigrationConfiguration configuration) {
        clusterConfigService.write(configuration);
    }
}
