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
package org.graylog.datanode.cluster;

import org.graylog.datanode.Configuration;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.database.MongoConnection;

import javax.inject.Inject;

public class DataNodeServiceImpl extends NodeServiceImpl {
    @Inject
    public DataNodeServiceImpl(final MongoConnection mongoConnection, Configuration configuration) {
        super(mongoConnection, configuration.getStaleLeaderTimeout());
    }

    @Override
    public Node.Type type() {
        return Node.Type.DATANODE;
    }
}
