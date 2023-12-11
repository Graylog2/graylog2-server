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
package org.graylog.datanode.bindings;

import com.google.inject.AbstractModule;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeConfigurationProvider;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParser;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParserImpl;
import org.graylog.datanode.filesystem.index.statefile.StateFileParser;
import org.graylog.datanode.filesystem.index.statefile.StateFileParserImpl;
import org.graylog2.plugin.system.FilePersistedNodeIdProvider;
import org.graylog2.plugin.system.NodeId;
import org.graylog.datanode.configuration.OpensearchDistributionProvider;
import org.graylog.datanode.OpensearchDistribution;

public class DatanodeConfigurationBindings extends AbstractModule {
    @Override
    protected void configure() {
        bind(NodeId.class).toProvider(FilePersistedNodeIdProvider.class).asEagerSingleton();
        bind(DatanodeConfiguration.class).toProvider(DatanodeConfigurationProvider.class);
        bind(OpensearchDistribution.class).toProvider(OpensearchDistributionProvider.class);
        bind(StateFileParser.class).to(StateFileParserImpl.class);
        bind(ShardStatsParser.class).to(ShardStatsParserImpl.class);
    }
}
