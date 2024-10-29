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
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.inputs.WithInputConfiguration;

import javax.annotation.Nullable;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class InputCreateRequest implements WithInputConfiguration<InputCreateRequest> {
    @JsonProperty
    public abstract String title();

    @Override
    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract boolean global();

    @Override
    @JsonProperty
    public abstract Map<String, Object> configuration();

    @JsonProperty
    @Nullable
    public abstract String node();

    public abstract Builder toBuilder();

    @Override
    public InputCreateRequest withConfiguration(Map<String, Object> configuration) {
        return toBuilder().configuration(configuration).build();
    }

    @JsonCreator
    public static InputCreateRequest create(@JsonProperty("title") String title,
                                            @JsonProperty("type") String type,
                                            @JsonProperty("global") boolean global,
                                            @JsonProperty("configuration") Map<String, Object> configuration,
                                            @JsonProperty("node") String node) {
        return new AutoValue_InputCreateRequest(title, type, global, configuration, node);
    }

    public static Builder builder() {
        return new AutoValue_InputCreateRequest.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder title(String title);

        public abstract Builder type(String type);

        public abstract Builder global(boolean global);

        public abstract Builder configuration(Map<String, Object> configuration);

        public abstract Builder node(String node);

        public abstract InputCreateRequest build();
    }
}
