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
package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@AutoValue
@JsonTypeName(Values.NAME)
@JsonDeserialize(builder = Values.Builder.class)
public abstract class Values implements BucketSpec {
    public static final String NAME = "values";
    public static final int DEFAULT_LIMIT = 15;

    @Override
    public abstract String type();

    @Override
    @JsonProperty
    public abstract List<String> fields();

    @JsonProperty
    public abstract Integer limit();

    public static Values.Builder builder() {
        return new AutoValue_Values.Builder()
                .type(NAME)
                .limit(DEFAULT_LIMIT);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends TypedBuilder<Values, Builder> {

        @JsonCreator
        public static Builder create() {
            return Values.builder().limit(DEFAULT_LIMIT);
        }

        @JsonProperty
        public Builder field(String field) {
            return fields(Collections.singletonList(field));
        }

        @JsonProperty
        public abstract Builder fields(List<String> fields);

        @JsonProperty
        public abstract Builder limit(Integer limit);

    }

}

