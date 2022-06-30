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
package org.graylog.plugins.views.search.rest.suggestions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SuggestionsRequestDTO.Builder.class)
public abstract class SuggestionsRequestDTO {

    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_FILTERING_QUERY = "filtering_query";
    public static final int DEFAULT_SUGGESTIONS_COUNT = 10;

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract String input();

    @JsonProperty
    public abstract int size();

    @Nullable
    @JsonProperty(FIELD_TIMERANGE)
    public abstract TimeRange timerange();

    @Nullable
    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @Nullable
    @JsonProperty(FIELD_FILTERING_QUERY)
    public abstract String filteringQuery();


    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty
        public abstract SuggestionsRequestDTO.Builder field(String field);

        @JsonProperty
        public abstract SuggestionsRequestDTO.Builder input(String input);

        @JsonProperty(FIELD_STREAMS)
        public abstract SuggestionsRequestDTO.Builder streams(@Nullable Set<String> streams);

        @JsonProperty(FIELD_TIMERANGE)
        public abstract SuggestionsRequestDTO.Builder timerange(@Nullable TimeRange timerange);

        @JsonProperty
        public abstract SuggestionsRequestDTO.Builder size(int size);

        @JsonProperty
        public abstract SuggestionsRequestDTO.Builder filteringQuery(@Nullable String filteringQuery);

        public abstract SuggestionsRequestDTO build();

        @JsonCreator
        public static SuggestionsRequestDTO.Builder builder() {
            return new AutoValue_SuggestionsRequestDTO.Builder()
                    .size(DEFAULT_SUGGESTIONS_COUNT);
        }
    }
}
