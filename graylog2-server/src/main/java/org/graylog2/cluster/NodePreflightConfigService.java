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
package org.graylog2.cluster;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.graylog2.cluster.NodePreflightConfig.FIELD_NODEID;

public class NodePreflightConfigService extends PaginatedDbService<NodePreflightConfig> {
    public static final String COLLECTION_NAME = "node_preflight_config";

    private static final Logger LOG = LoggerFactory.getLogger(NodePreflightConfigService.class);
    private final JacksonDBCollection<NodePreflightConfig, String> dbCollection;

    @Inject
    public NodePreflightConfigService(final MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                                      final MongoConnection mongoConnection) {
        super(mongoConnection, mongoJackObjectMapperProvider, NodePreflightConfig.class, COLLECTION_NAME);
        this.dbCollection = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME), NodePreflightConfig.class, String.class, mongoJackObjectMapperProvider.get());
    }

    public NodePreflightConfig getPreflightConfigFor(Node node) {
        return getPreflightConfigFor(node.getNodeId());
    }

    public NodePreflightConfig getPreflightConfigFor(String nodeId) {
        return dbCollection.findOne(DBQuery.is(FIELD_NODEID, nodeId));
    }

    public void deleteAll() {
        dbCollection.drop();
    }
}
