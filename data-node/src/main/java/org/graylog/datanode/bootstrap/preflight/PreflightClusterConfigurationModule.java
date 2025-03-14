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
package org.graylog.datanode.bootstrap.preflight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import org.graylog.datanode.bindings.PreflightObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.shared.plugins.GraylogClassLoader;

public class PreflightClusterConfigurationModule extends AbstractModule {
    private final ChainingClassLoader chainingClassLoader;

    public PreflightClusterConfigurationModule(ChainingClassLoader chainingClassLoader) {
        this.chainingClassLoader = chainingClassLoader;
    }

    @Override
    protected void configure() {
        bind(ChainingClassLoader.class).toInstance(chainingClassLoader);
        bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class).asEagerSingleton();

        bindLimitedObjectMapper();
    }

    private void bindLimitedObjectMapper() {
        bind(ClassLoader.class).annotatedWith(GraylogClassLoader.class).toInstance(chainingClassLoader);
        bind(ObjectMapper.class).toProvider(PreflightObjectMapperProvider.class).asEagerSingleton();
    }
}
