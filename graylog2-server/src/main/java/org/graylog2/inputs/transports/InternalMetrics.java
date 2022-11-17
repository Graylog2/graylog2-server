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
package org.graylog2.inputs.transports;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = InternalMetrics.Builder.class)
public abstract class InternalMetrics {
    @JsonProperty
    public abstract DateTime timestamp();

    @JsonProperty
    public abstract Map<String, Object> gauges();

    public static Builder builder() {
        return new AutoValue_InternalMetrics.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return InternalMetrics.builder();
        }
        @JsonProperty
        public abstract Builder timestamp(DateTime timestamp);
        @JsonProperty
        public abstract Builder gauges(Map<String, Object> gauges);
        public abstract InternalMetrics build();
    }
}
