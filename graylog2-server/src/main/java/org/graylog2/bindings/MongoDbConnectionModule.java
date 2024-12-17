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
package org.graylog2.bindings;

import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.inject.Graylog2Module;

import java.util.Set;

/**
 * Provides a basic MongoDB connection ready to be used with mongojack
 */
public class MongoDbConnectionModule extends Graylog2Module {
    @Override
    protected void configure() {
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);
        bind(MongoCollections.class).asEagerSingleton();
        bind(MongoJackObjectMapperProvider.class);
    }

    @Override
    protected Set<Object> getConfigurationBeans() {
        return Set.of(new MongoDbConfiguration());
    }
}
