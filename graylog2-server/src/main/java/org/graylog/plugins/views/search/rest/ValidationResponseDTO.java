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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.explain.DataRoutedStream;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.graylog.plugins.views.search.ExplainResults.IndexRangeResult;

@AutoValue
public abstract class ValidationResponseDTO {

    public static ValidationResponseDTO create(final ValidationStatusDTO status,
                                               final List<ValidationMessageDTO> explanations,
                                               final Set<IndexRangeResult> searchedIndexRanges,
                                               final Set<DataRoutedStream> dataRoutedStreams,
                                               final Optional<TimeRange> searchedTimeRange) {
        return new AutoValue_ValidationResponseDTO(status, explanations, Context.create(searchedTimeRange, searchedIndexRanges, dataRoutedStreams));
    }

    @JsonProperty
    public abstract ValidationStatusDTO status();

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public abstract List<ValidationMessageDTO> explanations();

    @JsonProperty
    public abstract Context context();

    @AutoValue
    public static abstract class Context {

        public static final String SEARCHED_INDEX_RANGES = "searched_index_ranges";
        public static final String DATA_ROUTED_STREAMS = "data_routed_streams";
        public static final String SEARCHED_TIME_RANGE = "searched_time_range";

        @JsonCreator
        public static Context create(@JsonProperty(SEARCHED_TIME_RANGE) Optional<TimeRange> searchedTimeRange,
                                     @JsonProperty(SEARCHED_INDEX_RANGES) Set<IndexRangeResult> searchedIndexRanges,
                                     @JsonProperty(DATA_ROUTED_STREAMS) Set<DataRoutedStream> dataRoutedStreams) {
            return new AutoValue_ValidationResponseDTO_Context(searchedTimeRange, searchedIndexRanges, dataRoutedStreams);
        }

        @JsonProperty
        public abstract Optional<TimeRange> searchedTimeRange();

        @JsonProperty(SEARCHED_INDEX_RANGES)
        public abstract Set<IndexRangeResult> searchedIndexRanges();

        @JsonProperty(DATA_ROUTED_STREAMS)
        public abstract Set<DataRoutedStream> dataRoutedStreams();
    }
}
