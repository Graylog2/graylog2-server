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
package org.graylog2.configuration.managed;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;

import java.time.Clock;

/**
 * The configuration values of Graylog server nodes.
 */
@Singleton
public class ServerConfigurationService extends ManagedConfigurationService {
    public static final String COLLECTION_NAME = "node_configuration";

    @Inject
    public ServerConfigurationService(MongoCollections mongoCollections,
                                      ClusterEventBus clusterEventBus,
                                      Clock clock) {
        super(mongoCollections, COLLECTION_NAME, NodeType.SERVER, clusterEventBus, clock);
    }
}
