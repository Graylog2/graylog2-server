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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = FieldTypesForStreamsRequest.Builder.class)
public abstract class FieldTypesForStreamsRequest {
    private static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        public abstract FieldTypesForStreamsRequest build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_FieldTypesForStreamsRequest.Builder();
        }
    }
}
