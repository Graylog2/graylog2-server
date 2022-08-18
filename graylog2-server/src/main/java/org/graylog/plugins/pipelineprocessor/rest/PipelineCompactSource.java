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
package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class PipelineCompactSource {

    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    public abstract String title();

    public static Builder builder() {
        return new AutoValue_PipelineCompactSource.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static PipelineCompactSource create(@JsonProperty("id") @Id @ObjectId @Nullable String id,
                                               @JsonProperty("title") String title) {
        return builder()
                .id(id)
                .title(title)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract PipelineCompactSource build();

        public abstract Builder id(String id);

        public abstract Builder title(String title);
    }
}
