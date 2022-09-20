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
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Parameter;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = FieldTypesForQueryRequest.Builder.class)
public abstract class FieldTypesForQueryRequest {

    private static final String FIELD_QUERY = "query";
    private static final String FIELD_PARAMS = "parameters";
    private static final String FIELD_FALLBACK = "fallback";

    @JsonProperty(FIELD_QUERY)
    public abstract QueryDTO query();

    @JsonProperty(FIELD_PARAMS)
    public abstract ImmutableSet<Parameter> parameters();

    @JsonProperty(FIELD_FALLBACK)
    public abstract FieldTypesForStreamsRequest fallback();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty(FIELD_QUERY)
        public abstract FieldTypesForQueryRequest.Builder query(final QueryDTO queryDTO);

        @JsonProperty(FIELD_PARAMS)
        public abstract FieldTypesForQueryRequest.Builder parameters(final ImmutableSet<Parameter> parameters);

        @JsonProperty(FIELD_FALLBACK)
        public abstract FieldTypesForQueryRequest.Builder fallback(final FieldTypesForStreamsRequest fallback);

        public abstract FieldTypesForQueryRequest build();

        @JsonCreator
        public static FieldTypesForQueryRequest.Builder builder() {
            return new AutoValue_FieldTypesForQueryRequest.Builder();
        }
    }
}
