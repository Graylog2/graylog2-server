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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.validation.constraints.NotNull;
import java.util.Set;

@AutoValue
public abstract class ValidationRequest {

    @NotNull
    public abstract BackendQuery query();

    @NotNull
    public abstract TimeRange timerange();

    @NotNull
    public abstract Set<String> streams();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty
        public abstract Builder query(@NotNull BackendQuery query);

        public abstract Builder streams(@NotNull Set<String> streams);

        public abstract Builder timerange(@NotNull TimeRange timerange);

        public abstract ValidationRequest build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_ValidationRequest.Builder();
        }
    }
}
