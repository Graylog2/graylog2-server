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

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog.datanode.shared.system.activities.DataNodeActivityWriter;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.cluster.nodes.DataNodeClusterService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterIdFactory;
import org.graylog2.plugin.cluster.RandomUUIDClusterIdFactory;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.system.activities.ActivityWriter;

public class DatanodeServerBindings extends Graylog2Module {

    public DatanodeServerBindings() {
    }

    @Override
    protected void configure() {
        bindInterfaces();
        bindSingletons();

        bindDynamicFeatures();
        bindExceptionMappers();
    }


    private void bindSingletons() {
        bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class).asEagerSingleton();
    }

    private void bindInterfaces() {
        bind(ActivityWriter.class).to(DataNodeActivityWriter.class);
        OptionalBinder.newOptionalBinder(binder(), ClusterIdFactory.class).setDefault().to(RandomUUIDClusterIdFactory.class);
        bind(new TypeLiteral<NodeService<DataNodeDto>>() {}).to(DataNodeClusterService.class);
    }

    private void bindDynamicFeatures() {
        jerseyDynamicFeatureBinder();
    }

    private void bindExceptionMappers() {
        jerseyExceptionMapperBinder();
    }

}
