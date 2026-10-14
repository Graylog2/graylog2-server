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
package org.graylog2.configuration.overrides;

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
 * A configuration parameter value that overrides the configured value on a single node.
 *
 * @param value     The raw value, as it would appear in the configuration file
 * @param updatedBy The name of the user who last changed the value
 */
public record NodeConfigurationOverride(
        @ObjectId @Id @Nullable @JsonProperty(FIELD_ID) String id,
        @JsonProperty(FIELD_NODE_ID) String nodeId,
        @JsonProperty(FIELD_NODE_TYPE) NodeType nodeType,
        @JsonProperty(FIELD_NAME) String name,
        @JsonProperty(FIELD_VALUE) String value,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_UPDATED_AT) Instant updatedAt,
        @JsonProperty(FIELD_UPDATED_BY) String updatedBy
) implements MongoEntity {
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_NODE_TYPE = "node_type";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_UPDATED_AT = "updated_at";
    public static final String FIELD_UPDATED_BY = "updated_by";
}
