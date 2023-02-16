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
package org.graylog.datanode.configuration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Optional;

public class DataNodeConfigProvider implements Provider<DataNodeConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodeConfigProvider.class);

    private final ClusterConfigService clusterConfigService;

    @Inject
    public DataNodeConfigProvider(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public DataNodeConfig get() {
        return loadFromDatabase().orElse(getDefaultConfig());
    }

    public DataNodeConfig getDefaultConfig() {
        return new DataNodeConfig(null);
    }

    @NotNull
    public Optional<DataNodeConfig> loadFromDatabase() {
        try {
            return Optional.ofNullable(clusterConfigService.get(DataNodeConfig.class));
        } catch (Exception e) {
            LOG.error("Failed to fetch datanode configuration from database", e);
            return Optional.empty();
        }
    }

}
