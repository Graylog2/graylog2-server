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
package org.graylog.plugins.views.search.searchtypes.pivot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.SearchType;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonTypeName(Pivot.NAME)
@JsonDeserialize(builder = PivotResult.Builder.class)
public abstract class PivotResult implements SearchType.Result {
    private static final String FIELD_EFFECTIVE_TIMERANGE = "effective_timerange";

    @Override
    @JsonProperty
    public abstract String id();

    @Override
    public abstract String type();

    @JsonProperty
    public abstract ImmutableList<Row> rows();

    @JsonProperty
    public abstract long total();

    @JsonProperty(FIELD_EFFECTIVE_TIMERANGE)
    public abstract AbsoluteRange effectiveTimerange();

    public static Builder builder() {
        return new AutoValue_PivotResult.Builder().type(Pivot.NAME);
    }

    public static PivotResult empty(String id) {
        return builder().id(id).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static PivotResult.Builder create() {
            return new AutoValue_PivotResult.Builder().type(Pivot.NAME);
        }

        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder name(String name);

        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder rows(ImmutableList<Row> rows);

        abstract ImmutableList.Builder<Row> rowsBuilder();
        public Builder addRow(Row row) {
            rowsBuilder().add(row);
            return this;
        }

        @JsonProperty
        public abstract Builder total(long total);

        @JsonProperty
        public abstract Builder effectiveTimerange(AbsoluteRange effectiveTimerange);

        public abstract PivotResult build();
    }

    @AutoValue
    @JsonDeserialize(builder = PivotResult.Row.Builder.class)
    public static abstract class Row {

        @JsonCreator
        public static PivotResult.Row.Builder create() {
            return builder();
        }

        @JsonProperty
        public abstract ImmutableList<String> key();

        @JsonProperty
        public abstract ImmutableList<Value> values();

        @JsonProperty
        public abstract String source();

        public static Builder builder() {
            return new AutoValue_PivotResult_Row.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {

            @JsonCreator
            public static PivotResult.Row.Builder create() {
                return new AutoValue_PivotResult_Row.Builder();
            }

            @JsonProperty
            public abstract Builder key(ImmutableList<String> key);

            @JsonProperty
            public abstract Builder values(ImmutableList<Value> values);

            @JsonProperty
            public abstract Builder source(String source);

            abstract ImmutableList.Builder<Value> valuesBuilder();

            public Builder addValue(Value value) {
                valuesBuilder().add(value);
                return this;
            }

            public abstract Row build();
        }
    }

    public record Value(@JsonProperty ImmutableList<String> key,
                        @JsonProperty @Nullable Object value,
                        @JsonProperty boolean rollup,
                        @JsonProperty String source) {

        @JsonCreator
        public static Value create(@JsonProperty("key") Collection<String> key,
                                   @JsonProperty("value") @Nullable Object value,
                                   @JsonProperty("rollup") boolean rollup,
                                   @JsonProperty("source") String source) {
            return new Value(ImmutableList.copyOf(key), value, rollup, source);
        }
    }
}
