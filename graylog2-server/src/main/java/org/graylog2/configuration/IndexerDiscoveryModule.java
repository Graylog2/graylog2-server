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
package org.graylog2.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.bootstrap.preflight.PreflightConfigServiceImpl;
import org.graylog2.cluster.nodes.DataNodeClusterService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.database.MongoConnection;

import java.net.URI;
import java.util.List;

public class IndexerDiscoveryModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<List<URI>>() {}).annotatedWith(IndexerHosts.class).toProvider(IndexerDiscoveryProvider.class).asEagerSingleton();
        bind(Boolean.class).annotatedWith(RunsWithDataNode.class).toProvider(RunsWithDataNodeDiscoveryProvider.class).asEagerSingleton();
        bind(new TypeLiteral<NodeService<DataNodeDto>>() {}).to(DataNodeClusterService.class);
        bind(PreflightConfigService.class).to(PreflightConfigServiceImpl.class);
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);
    }
}
