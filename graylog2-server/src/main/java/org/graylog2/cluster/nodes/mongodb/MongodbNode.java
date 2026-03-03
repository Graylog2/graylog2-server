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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.graylog2.utilities.lucene.InMemorySearchableEntity;
import org.graylog2.utilities.lucene.LuceneDocBuilder;

public record MongodbNode(
        String id,
        String name,
        String role,
        String version,
        int profilingLevel,
        long replicationLag,
        Long slowQueryCount,
        double storageUsedPercent,
        Integer availableConnections,
        Integer currentConnections,
        double connectionsUsedPercent
) implements InMemorySearchableEntity {

    @JsonIgnore
    @Override
    public void buildLuceneDoc(LuceneDocBuilder builder) {
        builder.stringVal("id", id);
        builder.stringVal("name", name);
        builder.stringVal("role", role);
        builder.intVal("profilingLevel", profilingLevel);
        builder.longVal("replicationLag", replicationLag);
        builder.longVal("slowQueryCount", slowQueryCount);
        builder.doubleVal("storageUsedPercent", storageUsedPercent);
        builder.intVal("availableConnections", availableConnections);
        builder.intVal("currentConnections", currentConnections);
        builder.doubleVal("connectionsUsedPercent", connectionsUsedPercent);
    }
}
