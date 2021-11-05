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
package org.graylog.plugins.views.search.engine;

import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.ArrayList;
import java.util.Set;

@AutoValue
public abstract class SuggestRequest {

    public abstract String field();
    public abstract String input();
    public abstract TimeRange timerange();
    public abstract Set<String> streams();

    public static Builder builder() {
        return new AutoValue_SuggestRequest.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract SuggestRequest.Builder field(String field);
        public abstract SuggestRequest.Builder input(String input);
        public abstract SuggestRequest.Builder streams(Set<String> streams);
        public abstract SuggestRequest.Builder timerange(TimeRange timerange);

        public abstract SuggestRequest build();

        public static SuggestRequest.Builder builder() {
            return new AutoValue_SuggestRequest.Builder();
        }
    }
}
