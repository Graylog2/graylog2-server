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
package org.graylog.collectors.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;

@AutoValue
public abstract class OtelPipelineConfig {

    @JsonProperty("receivers")
    public abstract Set<String> receivers();

    @JsonProperty("exporters")
    public abstract Set<String> exporters();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("processors")
    public abstract Set<String> processors();

    public static Builder builder() {
        return new AutoValue_OtelPipelineConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder receivers(Set<String> receivers);

        public abstract Builder exporters(Set<String> exporters);

        public abstract Builder processors(@Nullable Set<String> processors);

        public abstract OtelPipelineConfig build();
    }
}
