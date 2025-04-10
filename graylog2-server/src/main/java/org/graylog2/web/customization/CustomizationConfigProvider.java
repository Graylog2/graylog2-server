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
package org.graylog2.web.customization;

import com.google.common.base.Suppliers;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.function.Supplier;

@Singleton
public class CustomizationConfigProvider implements Provider<Config> {
    private final ClusterConfigService clusterConfigService;
    private final Supplier<Config> configSupplier;

    @Inject
    public CustomizationConfigProvider(ClusterConfigService clusterConfigService, @Named("isDevelopmentServer") boolean isDevelopment) {
        this.clusterConfigService = clusterConfigService;
        this.configSupplier = isDevelopment ? this::retrieve : Suppliers.memoize(this::retrieve);
    }

    private Config retrieve() {
        return clusterConfigService.get(Config.class);
    }

    @Override
    public Config get() {
        return configSupplier.get();
    }
}
