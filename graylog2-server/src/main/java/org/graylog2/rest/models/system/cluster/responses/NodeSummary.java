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
package org.graylog2.rest.models.system.cluster.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class NodeSummary {
    @JsonProperty
    public abstract String clusterId();
    @JsonProperty
    public abstract String nodeId();
    @JsonProperty
    public abstract String type();
    @JsonProperty("is_master")
    public abstract boolean isMaster();
    @JsonProperty
    public abstract String transportAddress();
    @JsonProperty
    public abstract String lastSeen();
    @JsonProperty
    public abstract String shortNodeId();
    @JsonProperty
    public abstract String hostname();

    @JsonCreator
    public static NodeSummary create(@JsonProperty("cluster_id") String clusterId,
                                     @JsonProperty("node_id") String nodeId,
                                     @JsonProperty("type") String type,
                                     @JsonProperty("is_master") boolean isMaster,
                                     @JsonProperty("transport_address") String transportAddress,
                                     @JsonProperty("last_seen") String lastSeen,
                                     @JsonProperty("short_node_id") String shortNodeId,
                                     @JsonProperty("hostname") String hostname) {
        return new AutoValue_NodeSummary(clusterId, nodeId, type, isMaster, transportAddress, lastSeen, shortNodeId, hostname);
    }
}
