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
package org.graylog.testing.cluster;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Set;
import java.util.UUID;

/**
 * Provides a {@link ClusterConfigService} instance for injection in tests.
 */
public class ClusterConfigServiceExtension implements ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, @NonNull ExtensionContext context) throws ParameterResolutionException {
        return ClusterConfigService.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(@NonNull ParameterContext parameterContext, @NonNull ExtensionContext context) throws ParameterResolutionException {
        try {
            final var mongodb = MongoDBExtension.getInstance(context);

            return new ClusterConfigServiceImpl(
                    new MongoJackObjectMapperProvider(new ObjectMapperProvider().get()),
                    mongodb.mongoConnection(),
                    new SimpleNodeId(UUID.randomUUID().toString()),
                    new RestrictedChainingClassLoader(new ChainingClassLoader(ClusterConfigService.class.getClassLoader()), new SafeClasses(Set.of())),
                    new ClusterEventBus()
            );
        } catch (NullPointerException e) {
            throw new IllegalStateException("MongoDBExtension hasn't been initialized. Make sure to add the MongoDBExtension to your test class!", e);
        }
    }
}
