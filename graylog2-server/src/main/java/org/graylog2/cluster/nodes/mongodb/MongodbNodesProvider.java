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
package org.graylog2.cluster.nodes.mongodb;


import jakarta.inject.Provider;

import java.util.List;
import java.util.Set;

public class MongodbNodesProvider implements Provider<List<MongodbNode>> {

    private final MongodbNodesService activeService;

    public MongodbNodesProvider(Set<MongodbNodesService> services) {

        activeService = services.stream()
                .filter(MongodbNodesService::available)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MongodbNodesService available"));
    }

    @Override
    public List<MongodbNode> get() {
        return activeService.allNodes();
    }
}
