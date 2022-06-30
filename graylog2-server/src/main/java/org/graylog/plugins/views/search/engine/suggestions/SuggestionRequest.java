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
package org.graylog.plugins.views.search.engine.suggestions;

import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.rest.ExecutionStateGlobalOverride;
import org.graylog.plugins.views.search.rest.FieldTypesForStreamsRequest;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

@AutoValue
public abstract class SuggestionRequest {

    public abstract String field();
    public abstract String input();
    public abstract TimeRange timerange();
    public abstract Set<String> streams();
    public abstract int size();
    public abstract Optional<String> filteringQuery();

    public static Builder builder() {
        return new AutoValue_SuggestionRequest.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract SuggestionRequest.Builder field(String field);
        public abstract SuggestionRequest.Builder input(String input);
        public abstract SuggestionRequest.Builder streams(Set<String> streams);
        public abstract SuggestionRequest.Builder timerange(TimeRange timerange);
        public abstract SuggestionRequest.Builder size(int size);
        public abstract SuggestionRequest.Builder filteringQuery(@Nullable String filteringQuery);

        public abstract SuggestionRequest build();

        public static SuggestionRequest.Builder builder() {
            return new AutoValue_SuggestionRequest.Builder();
        }

    }
}
