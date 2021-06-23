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
package org.graylog.plugins.views.search.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;

import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_FIELDS;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;

@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = ResultFormat.Builder.class)
public abstract class ResultFormat {
    private static final String FIELD_FIELDS = "fields_in_order";

    @JsonProperty(FIELD_FIELDS)
    @NotEmpty
    public abstract LinkedHashSet<String> fieldsInOrder();

    @JsonProperty
    public abstract Optional<AbsoluteRange> timerange();

    @JsonProperty
    public abstract Optional<Integer> limit();

    @JsonProperty
    public abstract Map<String, Object> executionState();

    @JsonProperty
    public abstract Optional<String> filename();

    public static ResultFormat.Builder builder() {
        return ResultFormat.Builder.create();
    }

    public static ResultFormat empty() {
        return ResultFormat.builder().build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_FIELDS)
        public abstract Builder fieldsInOrder(LinkedHashSet<String> fieldsInOrder);

        public Builder fieldsInOrder(String... fields) {
            return fieldsInOrder(linkedHashSetOf(fields));
        }

        @JsonProperty
        public abstract Builder limit(@Positive @Nullable Integer limit);

        @JsonProperty
        public abstract Builder executionState(Map<String, Object> executionState);

        @JsonProperty
        public abstract Builder timerange(@Nullable AbsoluteRange timeRange);

        @JsonProperty
        public abstract Builder filename(@Nullable String filename);

        public abstract ResultFormat build();

        @JsonCreator
        public static ResultFormat.Builder create() {
            return new AutoValue_ResultFormat.Builder()
                    .fieldsInOrder(DEFAULT_FIELDS)
                    .executionState(Collections.emptyMap());
        }
    }
}
