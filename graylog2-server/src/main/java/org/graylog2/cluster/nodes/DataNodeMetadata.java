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
package org.graylog2.cluster.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.database.MongoEntity;
import org.mongojack.Id;

import javax.annotation.Nullable;

public record DataNodeMetadata(
        @Id @JsonProperty(FIELD_ID) @Nullable String id,
        @JsonProperty(FIELD_NODE_ID) String nodeId,
        @JsonProperty(FIELD_CURRENT_OPENSEARCH_VERSION) String currentOpensearchVersion,
        @JsonProperty(FIELD_LATEST_AVAILABLE_OPENSEARCH_VERSION) @Nullable String latestAvailableOpensearchVersion
) implements MongoEntity {
    public static final String COLLECTION_NAME = "datanode_metadata";

    public static final String FIELD_ID = "id";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_CURRENT_OPENSEARCH_VERSION = "current_opensearch_version";
    public static final String FIELD_LATEST_AVAILABLE_OPENSEARCH_VERSION = "latest_available_opensearch_version";

    public DataNodeMetadata(String nodeId, String currentOpensearchVersion) {
        this(null, nodeId, currentOpensearchVersion, null);
    }
}
