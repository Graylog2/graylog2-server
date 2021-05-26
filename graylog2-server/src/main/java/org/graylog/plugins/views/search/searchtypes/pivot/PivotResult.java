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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.SearchType;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class PivotResult implements SearchType.Result {
    private static final String FIELD_EFFECTIVE_TIMERANGE = "effective_timerange";

    @Override
    @JsonProperty
    public abstract String id();

    @Override
    @JsonProperty
    public String type() {
        return Pivot.NAME;
    }

    @JsonProperty
    public abstract ImmutableList<Row> rows();

    @JsonProperty
    public abstract long total();

    @JsonProperty(FIELD_EFFECTIVE_TIMERANGE)
    public abstract AbsoluteRange effectiveTimerange();

    public static Builder builder() {
        return new AutoValue_PivotResult.Builder();
    }

    public static PivotResult empty(String id) {
        return builder().id(id).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder id(String id);

        public abstract Builder name(String name);

        abstract ImmutableList.Builder<Row> rowsBuilder();
        public Builder addRow(Row row) {
            rowsBuilder().add(row);
            return this;
        }
        public Builder addAllRows(List<Row> rows) {
            rowsBuilder().addAll(rows);
            return this;
        }

        public abstract Builder total(long total);

        public abstract Builder effectiveTimerange(AbsoluteRange effectiveTimerange);

        public abstract PivotResult build();
    }

    @AutoValue
    public static abstract class Row {

        @JsonProperty
        public abstract List<String> key();

        @JsonProperty
        public abstract ImmutableList<Value> values();

        @JsonProperty
        public abstract String source();

        public static Builder builder() {
            return new AutoValue_PivotResult_Row.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder key(List<String> key);

            abstract ImmutableList.Builder<Value> valuesBuilder();
            //public abstract Builder values(ImmutableList<Value> values);
            public Builder addValue(Value value) {
                valuesBuilder().add(value);
                return this;
            }
            public Builder addAllValues(List<Value> values) {
                valuesBuilder().addAll(values);
                return this;
            }

            public abstract Builder source(String source);

            public abstract Row build();
        }
    }

    @AutoValue
    public static abstract class Value {

        @JsonProperty
        public abstract List<String> key();

        @JsonProperty
        @Nullable
        public abstract Object value();

        @JsonProperty
        public abstract boolean rollup();

        @JsonProperty
        public abstract String source();

        public static Value create(Collection<String> key, @Nullable Object value, boolean rollup, String source) {
            return new AutoValue_PivotResult_Value(copy(key), value, rollup, source);
        }

        private static List<String> copy(Collection<String> key) {
            return Collections.unmodifiableList(key.stream().collect(Collectors.toList()));
        }
    }
}
