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

import org.graylog.datanode.Configuration;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.IndexerJwtAuthTokenProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;

@Singleton
public class DatanodeConfigurationProvider implements Provider<DatanodeConfiguration> {

    private final DatanodeConfiguration datanodeConfiguration;

    @Inject
    public DatanodeConfigurationProvider(final Configuration localConfiguration, IndexerJwtAuthTokenProvider jwtTokenProvider, final NodeId nodeId) throws IOException {
        final OpensearchDistribution opensearchDistribution = detectOpensearchDistribution(localConfiguration);
        datanodeConfiguration = new DatanodeConfiguration(
                opensearchDistribution,
                getNodesFromConfig(localConfiguration.getDatanodeNodeName(), nodeId),
                localConfiguration.getProcessLogsBufferSize(),
                jwtTokenProvider
        );
    }

    public static String getNodesFromConfig(final String configProperty, final NodeId nodeId) {
        if(configProperty != null) return configProperty;
        return nodeId.getShortNodeId();
    }

    @Override
    public DatanodeConfiguration get() {
        return datanodeConfiguration;
    }

    private static OpensearchDistribution detectOpensearchDistribution(final Configuration configuration) throws IOException {
        return OpensearchDistribution.detectInDirectory(Path.of(configuration.getOpensearchDistributionRoot()));
    }
}
