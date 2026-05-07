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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.bson.BsonTimestamp;
import org.graylog2.cluster.Node;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class NodeDto implements Node, MongoEntity {
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_HOSTNAME = "hostname";

    @Override
    public String id() {
        return getObjectId();
    }

    @JsonProperty("object_id")
    @Nullable
    public abstract String getObjectId();

    @JsonProperty("id")
    public abstract String getId();

    @JsonProperty(FIELD_NODE_ID)
    public String getNodeId() {
        return getId();
    }

    @Nullable
    @JsonProperty("transport_address")
    public abstract String getTransportAddress();

    @JsonProperty("last_seen")
    @Nullable
    public abstract DateTime getLastSeen();

    @JsonProperty(FIELD_HOSTNAME)
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
        final String transportAddress = getTransportAddress();
        if(transportAddress != null) {
            params.put("transport_address", getTransportAddress());
        }
        params.put("is_leader", isLeader());
        if (Objects.nonNull(getHostname())) {
            params.put("hostname", getHostname());
        }
        return params;
    }

    public abstract static class Builder<B extends Builder<B>> {
        @JsonProperty("_id")
        public abstract B setObjectId(String id);

        @JsonProperty("node_id")
        public abstract B setId(String id);

        @JsonProperty("transport_address")
        public abstract B setTransportAddress(@Nullable String transportAddress);

        @JsonProperty("last_seen")
        @JsonDeserialize(using = LastSeenDeserializer.class)
        public abstract B setLastSeen(DateTime lastSeen);

        @JsonProperty("hostname")
        public abstract B setHostname(String hostname);

        @JsonProperty("is_leader")
        public abstract B setLeader(boolean leader);

    }

    /**
     * Reads {@code last_seen} as either a Mongo Date (current) or a numeric epoch-seconds value (legacy).
     */
    static class LastSeenDeserializer extends JsonDeserializer<DateTime> {
        @Override
        public DateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
                final Object embedded = p.getEmbeddedObject();
                if (embedded instanceof Date date) {
                    return new DateTime(date, DateTimeZone.UTC);
                }
                if (embedded instanceof BsonTimestamp ts) {
                    return new DateTime(ts.getTime() * 1000L, DateTimeZone.UTC);
                }
                return (DateTime) ctxt.handleUnexpectedToken(DateTime.class, p);
            }
            if (t.isNumeric()) {
                return new DateTime(p.getLongValue() * 1000L, DateTimeZone.UTC);
            }
            return DateTime.parse(p.getValueAsString()).withZone(DateTimeZone.UTC);
        }
    }
}
