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
package org.graylog2.contentstream.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class ContentStreamSettings {
    private static final String FIELD_ENABLED = "content_stream_enabled";
    private static final String FIELD_TOPICS = "content_stream_topics";

    @JsonProperty(FIELD_ENABLED)
    public abstract Boolean contentStreamEnabled();

    @JsonProperty(FIELD_TOPICS)
    public abstract List<String> topics();

    public static Builder builder() {
        return new AutoValue_ContentStreamSettings.Builder();
    }

    @JsonCreator
    public static ContentStreamSettings create(
            @JsonProperty(FIELD_ENABLED) Boolean enabled,
            @JsonProperty(FIELD_TOPICS) List<String> topics
    ) {
        return builder()
                .contentStreamEnabled(enabled)
                .topics(topics)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty(FIELD_ENABLED)
        public abstract Builder contentStreamEnabled(Boolean enabled);

        @JsonProperty(FIELD_TOPICS)
        public abstract Builder topics(List<String> topics);

        public abstract ContentStreamSettings build();
    }
}
