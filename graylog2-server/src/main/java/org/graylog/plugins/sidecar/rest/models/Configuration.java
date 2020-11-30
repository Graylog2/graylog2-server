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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class Configuration {
    public static final String FIELD_ID = "id";
    public static final String FIELD_COLLECTOR_ID = "collector_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_COLOR = "color";
    public static final String FIELD_TEMPLATE = "template";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_COLLECTOR_ID)
    public abstract String collectorId();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_COLOR)
    public abstract String color();

    @JsonProperty(FIELD_TEMPLATE)
    public abstract String template();

    @JsonCreator
    public static Configuration create(@JsonProperty(FIELD_ID) String id,
                                       @JsonProperty(FIELD_COLLECTOR_ID) String collectorId,
                                       @JsonProperty(FIELD_NAME) String name,
                                       @JsonProperty(FIELD_COLOR) String color,
                                       @JsonProperty(FIELD_TEMPLATE) String template) {
        return builder()
                .id(id)
                .collectorId(collectorId)
                .name(name)
                .color(color)
                .template(template)
                .build();
    }

    public static Configuration create(String collectorId,
                                       String name,
                                       String color,
                                       String template) {
        return create(new org.bson.types.ObjectId().toHexString(),
                collectorId,
                name,
                color,
                template);
    }

    public static Builder builder() {
        return new AutoValue_Configuration.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder collectorId(String collectorId);

        public abstract Builder name(String name);

        public abstract Builder color(String color);

        public abstract Builder template(String template);

        public abstract Configuration build();
    }
}
