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
package org.graylog2.rest.resources.system.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LookupTableCachePurgingNodeResponse {
    public final String nodeId;
    public final boolean success;
    public final String message;

    @JsonCreator
    private LookupTableCachePurgingNodeResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("node_id") String nodeId,
            @JsonProperty("message") String message) {
        this.success = success;
        this.nodeId = nodeId;
        this.message = message;
    }

    public static LookupTableCachePurgingNodeResponse success(String nodeId) {
        return new LookupTableCachePurgingNodeResponse(true, nodeId, "Successfully purged the cache");
    }

    public static LookupTableCachePurgingNodeResponse failure(String nodeId, String message) {
        return new LookupTableCachePurgingNodeResponse(false, nodeId, message);
    }
}
