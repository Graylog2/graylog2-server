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
package org.graylog2.bindings.providers;

import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class MongoConnectionProvider implements Provider<MongoConnection> {
    private static final Logger LOG = LoggerFactory.getLogger(MongoConnectionProvider.class);
    private static MongoConnection mongoConnection = null;

    @Inject
    public MongoConnectionProvider(MongoDbConfiguration configuration) {
        if (mongoConnection == null) {
            try {
                mongoConnection = new MongoConnectionImpl(configuration);

                mongoConnection.connect();
            } catch (Exception e) {
                LOG.error("Error connecting to MongoDB: {}", e.getMessage());
                throw e;
            }
        }
    }

    @Override
    public MongoConnection get() {
        return mongoConnection;
    }
}
