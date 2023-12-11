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
import de.undercouch.bson4jackson.types.Timestamp;
import org.graylog2.cluster.Node;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public abstract class NodeDto implements Node {

    @JsonProperty("object_id")
    @Nullable
    public abstract String getObjectId();

    @JsonProperty("id")
    public abstract String getId();

    @JsonProperty("node_id")
    public String getNodeId() {
        return getId();
    }

    @JsonProperty("transport_address")
    public abstract String getTransportAddress();

    @JsonProperty("last_seen")
    @Nullable
    public abstract DateTime getLastSeen();

    @JsonProperty("hostname")
    @Nullable
    public abstract String getHostname();

    @JsonProperty("is_leader")
    public abstract boolean isLeader();

    @JsonProperty("short_node_id")
    public String getShortNodeId() {
        return getId().split("-")[0];
    }

    public Map<String, Object> toEntityParameters() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("node_id", getNodeId());
        params.put("transport_address", getTransportAddress());
        params.put("is_leader", isLeader());
        params.put("hostname", getHostname());
        return params;
    }

    public abstract static class Builder<B extends Builder<B>> {
        @JsonProperty("_id")
        public abstract B setObjectId(String id);

        @JsonProperty("node_id")
        public abstract B setId(String id);

        @JsonProperty("transport_address")
        public abstract B setTransportAddress(String transportAddress);

        @JsonProperty("last_seen")
        public B setLastSeen(Timestamp timestamp) {
            return setLastSeen(new DateTime(timestamp.getTime() * 1000L, DateTimeZone.UTC));
        }

        public abstract B setLastSeen(DateTime lastSeen);

        @JsonProperty("hostname")
        public abstract B setHostname(String hostname);

        @JsonProperty("is_leader")
        public abstract B setLeader(boolean leader);

    }

}
