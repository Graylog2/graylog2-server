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
package org.graylog2.rest.models.messages.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ResultMessageSummary {
    private static final String FIELD_HIGHLIGHT_RANGES = "highlight_ranges";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_INDEX = "index";
    private static final String FIELD_DECORATION_STATS = "decoration_stats";

    @JsonProperty(FIELD_HIGHLIGHT_RANGES)
    @Nullable
    public abstract Multimap<String, Range<Integer>> highlightRanges();

    @JsonProperty(FIELD_MESSAGE)
    public abstract Map<String, Object> message();

    @JsonProperty(FIELD_INDEX)
    public abstract String index();

    @JsonProperty(FIELD_DECORATION_STATS)
    @Nullable
    public abstract DecorationStats decorationStats();

    private static Builder builder() {
        return new AutoValue_ResultMessageSummary.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static ResultMessageSummary create(@Nullable @JsonProperty(FIELD_HIGHLIGHT_RANGES) Multimap<String, Range<Integer>> highlightRanges,
                                              @JsonProperty(FIELD_MESSAGE) Map<String, Object> message,
                                              @JsonProperty(FIELD_INDEX) String index,
                                              @JsonProperty(FIELD_DECORATION_STATS) DecorationStats decorationStats) {
        return builder()
            .decorationStats(decorationStats)
            .highlightRanges(highlightRanges)
            .index(index)
            .message(message)
            .build();
    }

    public static ResultMessageSummary create(@Nullable @JsonProperty(FIELD_HIGHLIGHT_RANGES) Multimap<String, Range<Integer>> highlightRanges,
                                              @JsonProperty(FIELD_MESSAGE) Map<String, Object> message,
                                              @JsonProperty(FIELD_INDEX) String index) {
        return builder()
            .highlightRanges(highlightRanges)
            .index(index)
            .message(message)
            .build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder highlightRanges(Multimap<String, Range<Integer>> highlightRanges);

        public abstract Builder message(Map<String, Object> message);

        public abstract Builder index(String index);

        public abstract Builder decorationStats(DecorationStats decorationStats);

        public abstract ResultMessageSummary build();
    }
}
