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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.utilities.lucene.InMemorySearchableEntity;
import org.graylog2.utilities.lucene.LuceneDocBuilder;

import java.util.Optional;

public record MongodbNode(
        @JsonProperty(FIELD_ID) String id,
        @JsonProperty(FIELD_NAME) String name,
        @JsonProperty(FIELD_ROLE) String role,
        @JsonProperty(FIELD_VERSION) String version,
        @JsonProperty(FIELD_PROFILING_LEVEL) ProfilingLevel profilingLevel,
        @JsonProperty(FIELD_REPLICATION_LAG) Long replicationLag,
        @JsonProperty(FIELD_SLOW_QUERY_COUNT) Long slowQueryCount,
        @JsonProperty(FIELD_STORAGE_USED_PERCENT) Double storageUsedPercent,
        @JsonProperty(FIELD_AVAILABLE_CONNECTIONS) Integer availableConnections,
        @JsonProperty(FIELD_CURRENT_CONNECTIONS) Integer currentConnections,
        @JsonProperty(FIELD_CONNECTIONS_USED_PERCENT) Double connectionsUsedPercent
) implements InMemorySearchableEntity {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_PROFILING_LEVEL = "profiling_level";
    public static final String FIELD_REPLICATION_LAG = "replication_lag";
    public static final String FIELD_SLOW_QUERY_COUNT = "slow_query_count";
    public static final String FIELD_STORAGE_USED_PERCENT = "storage_used_percent";
    public static final String FIELD_AVAILABLE_CONNECTIONS = "available_connections";
    public static final String FIELD_CURRENT_CONNECTIONS = "current_connections";
    public static final String FIELD_CONNECTIONS_USED_PERCENT = "connections_used_percent";
    public static final String FIELD_VERSION = "version";

    public MongodbNode(String name, String role) {
        this("0", name, role, "", null, 0L, 0L, 0.0, 0, 0, 0.0);
    }

    @JsonIgnore
    @Override
    public void buildLuceneDoc(LuceneDocBuilder builder) {
        builder.stringVal(FIELD_ID, id);
        builder.stringVal(FIELD_NAME, name);
        builder.stringVal(FIELD_ROLE, role);
        builder.stringVal(FIELD_PROFILING_LEVEL, Optional.ofNullable(profilingLevel).map(Enum::name).orElse(null));
        builder.stringVal(FIELD_VERSION, version);
        builder.longVal(FIELD_REPLICATION_LAG, replicationLag);
        builder.longVal(FIELD_SLOW_QUERY_COUNT, slowQueryCount);
        builder.doubleVal(FIELD_STORAGE_USED_PERCENT, storageUsedPercent);
        builder.intVal(FIELD_AVAILABLE_CONNECTIONS, availableConnections);
        builder.intVal(FIELD_CURRENT_CONNECTIONS, currentConnections);
        builder.doubleVal(FIELD_CONNECTIONS_USED_PERCENT, connectionsUsedPercent);
    }
}
