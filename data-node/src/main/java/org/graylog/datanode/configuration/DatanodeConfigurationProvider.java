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
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog2.plugin.Tools;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class DatanodeConfigurationProvider implements Provider<DatanodeConfiguration> {

    private final DatanodeConfiguration datanodeConfiguration;

    @Inject
    public DatanodeConfigurationProvider(final Configuration localConfiguration, IndexerJwtAuthTokenProvider jwtTokenProvider, OpensearchDistributionProvider opensearchDistribution) throws IOException {
        datanodeConfiguration = new DatanodeConfiguration(
                opensearchDistribution,
                getNodesFromConfig(localConfiguration.getDatanodeNodeName()),
                localConfiguration.getProcessLogsBufferSize(),
                jwtTokenProvider
        );
    }

    public static String getNodesFromConfig(final String configProperty) {
        if(configProperty != null) return configProperty;
        return Tools.getLocalCanonicalHostname();
    }

    @Override
    public DatanodeConfiguration get() {
        return datanodeConfiguration;
    }

}
