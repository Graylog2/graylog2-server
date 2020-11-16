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

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class SimulationRequest {
    @JsonProperty
    public abstract String streamId();

    @JsonProperty
    public abstract Map<String, Object> message();

    @JsonProperty
    @Nullable
    public abstract String inputId();

    public static Builder builder() {
        return new AutoValue_SimulationRequest.Builder();
    }

    @JsonCreator
    public static SimulationRequest create (@JsonProperty("stream_id") String streamId,
                                            @JsonProperty("message") Map<String, Object> message,
                                            @JsonProperty("input_id") @Nullable String inputId) {
        return builder()
                .streamId(streamId)
                .message(message)
                .inputId(inputId)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract SimulationRequest build();

        public abstract Builder streamId(String streamId);

        public abstract Builder message(Map<String, Object> message);

        public abstract Builder inputId(String inputId);
    }
}
