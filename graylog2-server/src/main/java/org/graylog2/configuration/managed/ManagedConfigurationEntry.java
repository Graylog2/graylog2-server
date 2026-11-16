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
package org.graylog2.configuration.managed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;

/**
 * The stored state of a single configuration parameter, either for one node or, if the node ID is {@code null}, for
 * all nodes of the type the collection belongs to.
 * <p>
 * A row has two independent writers which never touch each other's fields: the node reports what it resolved from its
 * defaults, configuration file, environment or system properties, and the API stores what an administrator set. The
 * effective value isn't stored, it is derived with {@link #effectiveValue()} and {@link #effectiveSource()}: a value
 * set through the API always wins.
 *
 * @param nodeValue  The value this node resolved without taking the database into account
 * @param nodeSource Where {@link #nodeValue} came from
 * @param value      The value set through the API, {@code null} if nobody set one
 */
public record ManagedConfigurationEntry(
        @ObjectId @Id @Nullable @JsonProperty(FIELD_ID) String id,
        @JsonProperty(FIELD_NODE_ID) @Nullable String nodeId,
        @JsonProperty(FIELD_NAME) String name,
        @JsonProperty(FIELD_NODE_VALUE) @Nullable String nodeValue,
        @JsonProperty(FIELD_NODE_SOURCE) @Nullable ConfigurationSource nodeSource,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_NODE_UPDATED_AT) @Nullable Instant nodeUpdatedAt,
        @JsonProperty(FIELD_VALUE) @Nullable String value,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_UPDATED_AT) @Nullable Instant updatedAt,
        @JsonProperty(FIELD_UPDATED_BY) @Nullable String updatedBy
) implements MongoEntity {
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_NODE_VALUE = "node_value";
    public static final String FIELD_NODE_SOURCE = "node_source";
    public static final String FIELD_NODE_UPDATED_AT = "node_updated_at";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_UPDATED_AT = "updated_at";
    public static final String FIELD_UPDATED_BY = "updated_by";

    /**
     * The value which applies, which is the one set through the API if there is one.
     */
    @Nullable
    public String effectiveValue() {
        return value != null ? value : nodeValue;
    }

    /**
     * Where {@link #effectiveValue()} came from, {@code null} if the node hasn't reported a value yet and nobody set
     * one through the API.
     */
    @Nullable
    public ConfigurationSource effectiveSource() {
        return value != null ? ConfigurationSource.DATABASE : nodeSource;
    }
}
