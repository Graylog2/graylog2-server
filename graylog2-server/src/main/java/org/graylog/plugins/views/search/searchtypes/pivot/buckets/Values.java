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

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@JsonTypeName(Values.NAME)
@JsonDeserialize(builder = Values.Builder.class)
public abstract class Values implements BucketSpec {
    public static final String NAME = "values";
    public static final int DEFAULT_LIMIT = 15;
    private static final String FIELD_SKIP_EMPTY_VALUES = "skip_empty_values";
    private static final String FIELD_KEEP_LISTS_WHOLE = "keep_lists_whole";

    @Override
    public abstract String type();

    @Override
    @JsonProperty
    public abstract List<String> fields();

    @JsonProperty
    public abstract Integer limit();

    @JsonProperty(FIELD_SKIP_EMPTY_VALUES)
    public abstract boolean skipEmptyValues();

    /**
     * A field can hold several values in one message. By default, a grouping by a single field puts such a message
     * into one group per value, while a grouping by several fields keeps the list whole as one value. When this is
     * {@code true}, lists stay whole in both cases.
     * <p>
     * A whole list compares as stored in the doc values. Keyword doc values are sorted and hold each value once, so
     * {@code [bob, admin, bob]} and {@code [admin, bob]} are the same group. Numeric and date doc values are sorted but
     * keep repeated values, so {@code [80, 443, 443]} and {@code [80, 443]} are different groups. A keyword list that
     * holds one distinct value, such as {@code [bob, bob]}, is the same group as {@code bob}.
     * <p>
     * The group key of a whole list is its string form, for example {@code "[admin, bob]"}. It is meant for display
     * and comparison; it cannot be searched for as a field value. Values that contain {@code ", "} or look like a list
     * can share a key with a different list, for example {@code ["a, b", "c"]} and {@code [a, b, c]}.
     */
    @JsonProperty(FIELD_KEEP_LISTS_WHOLE)
    public abstract boolean keepListsWhole();

    public static Values.Builder builder() {
        return new AutoValue_Values.Builder()
                .type(NAME)
                .limit(DEFAULT_LIMIT)
                .skipEmptyValues(false)
                .keepListsWhole(false);
    }

    public Values withLimit(int limit) {
        return toBuilder().limit(limit).build();
    }

    abstract Builder toBuilder();

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

        public abstract Builder skipEmptyValues(Boolean skipEmptyValues);

        public Builder skipEmptyValues() {
            return skipEmptyValues(true);
        }

        @JsonProperty(FIELD_SKIP_EMPTY_VALUES)
        public Builder setSkipEmptyValues(@Nullable Boolean skipEmptyValues) {
            return skipEmptyValues(firstNonNull(skipEmptyValues, false));
        }

        public abstract Builder keepListsWhole(boolean keepListsWhole);

        public Builder keepListsWhole() {
            return keepListsWhole(true);
        }

        @JsonProperty(FIELD_KEEP_LISTS_WHOLE)
        public Builder setKeepListsWhole(@Nullable Boolean keepListsWhole) {
            return keepListsWhole(firstNonNull(keepListsWhole, false));
        }
    }
}

