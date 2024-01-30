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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.undercouch.bson4jackson.types.Timestamp;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;
import org.graylog2.cluster.Node;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractNode<DTO extends NodeDto> extends PersistedImpl implements Node {

    public AbstractNode(Map<String, Object> fields) {
        super(fields);
    }

    public AbstractNode(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public String getNodeId() {
        return (String) fields.get("node_id");
    }

    @Override
    @JsonProperty("is_leader")
    public boolean isLeader() {
        return (boolean) fields.get("is_leader");
    }

    @Override
    public String getTransportAddress() {
        return (String) fields.get("transport_address");
    }

    @Override
    public DateTime getLastSeen() {
        final Object rawLastSeen = fields.get("last_seen");
        if (rawLastSeen == null) {
            throw new IllegalStateException("Last seen timestamp of node is unexpectedly null!");
        }
        if (rawLastSeen instanceof BSONTimestamp) {
            return new DateTime(((BSONTimestamp) rawLastSeen).getTime() * 1000L, DateTimeZone.UTC);
        }
        if (rawLastSeen instanceof Timestamp ts) {
            return new DateTime(ts.getTime() * 1000L, DateTimeZone.UTC);
        }
        return new DateTime(((Integer) rawLastSeen) * 1000L, DateTimeZone.UTC);
    }

    @Override
    public String getHostname() {
        return (String) fields.get("hostname");
    }

    @Override
    @JsonIgnore
    public Map<String, Validator> getValidations() {
        return Collections.emptyMap();
    }

    @Override
    @JsonIgnore
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public abstract DTO toDto();

}
