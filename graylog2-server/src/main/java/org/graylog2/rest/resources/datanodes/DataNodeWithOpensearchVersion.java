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
package org.graylog2.rest.resources.datanodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.annotation.Nullable;
import org.graylog2.cluster.nodes.DataNodeDto;

/**
 * A data node, enriched with the OpenSearch version it is currently running. That version is not part of the
 * {@code datanodes} collection, it lives in {@code datanode_metadata} and is written by the data node itself once
 * OpenSearch has come up.
 * <p>
 * The data node is unwrapped, so the JSON representation is flat and remains backwards compatible with the plain
 * {@link DataNodeDto} representation.
 */
public record DataNodeWithOpensearchVersion(
        @JsonUnwrapped DataNodeDto dataNode,
        @JsonProperty(FIELD_OPENSEARCH_VERSION) @Nullable String opensearchVersion
) {
    public static final String FIELD_OPENSEARCH_VERSION = "opensearch_version";
}
