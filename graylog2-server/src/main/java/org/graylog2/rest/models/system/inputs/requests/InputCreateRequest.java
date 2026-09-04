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
package org.graylog2.rest.models.system.inputs.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.inputs.WithInputConfiguration;

import javax.annotation.Nullable;
import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class InputCreateRequest implements WithInputConfiguration<InputCreateRequest> {
    @JsonProperty("title")
    public abstract String title();

    @Override
    @JsonProperty("type")
    public abstract String type();

    @JsonProperty("global")
    public abstract boolean global();

    @Override
    @JsonProperty("configuration")
    public abstract Map<String, Object> configuration();

    @JsonProperty("node")
    @Nullable
    public abstract String node();

    public abstract Builder toBuilder();

    @Override
    public InputCreateRequest withConfiguration(Map<String, Object> configuration) {
        return toBuilder().configuration(configuration).build();
    }

    public static Builder builder() {
        return new AutoValue_InputCreateRequest.Builder();
    }

    @JsonCreator
    public static InputCreateRequest create(@JsonProperty("title") String title,
                                            @JsonProperty("type") String type,
                                            @JsonProperty("global") boolean global,
                                            @JsonProperty("configuration") Map<String, Object> configuration,
                                            @JsonProperty("node") String node) {
        return builder()
                .title(title)
                .type(type)
                .global(global)
                .configuration(configuration)
                .node(node)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("type")
        public abstract Builder type(String type);

        @JsonProperty("global")
        public abstract Builder global(boolean global);

        @JsonProperty("configuration")
        public abstract Builder configuration(Map<String, Object> configuration);

        @JsonProperty("node")
        public abstract Builder node(String node);

        public abstract InputCreateRequest build();
    }
}
