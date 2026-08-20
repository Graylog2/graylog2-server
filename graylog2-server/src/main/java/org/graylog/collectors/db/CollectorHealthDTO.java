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
package org.graylog.collectors.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;

import java.time.Instant;

@AutoValue
@JsonDeserialize(builder = CollectorHealthDTO.Builder.class)
public abstract class CollectorHealthDTO {

    public static final String FIELD_COMPONENT_HEALTH = "component_health";
    public static final String FIELD_HEALTHY_CHANGED_AT = "healthy_changed_at";

    @JsonProperty(FIELD_HEALTHY_CHANGED_AT)
    @JsonSerialize(using = MongoInstantSerializer.class)
    public abstract Instant healthyChangedAt();

    @JsonProperty(FIELD_COMPONENT_HEALTH)
    public abstract ComponentHealthDTO componentHealth();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_CollectorHealthDTO.Builder();
        }

        @JsonProperty(FIELD_HEALTHY_CHANGED_AT)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        public abstract Builder healthyChangedAt(Instant healthyChangedAt);

        @JsonProperty(FIELD_COMPONENT_HEALTH)
        public abstract Builder componentHealth(ComponentHealthDTO componentHealth);

        public abstract CollectorHealthDTO build();
    }
}
