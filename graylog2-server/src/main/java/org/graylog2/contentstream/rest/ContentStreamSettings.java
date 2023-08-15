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

import static org.graylog2.contentstream.db.ContentStreamUserSettings.FIELD_CONTENT_ENABLED;
import static org.graylog2.contentstream.db.ContentStreamUserSettings.FIELD_RELEASES_ENABLED;
import static org.graylog2.contentstream.db.ContentStreamUserSettings.FIELD_TOPICS;

@AutoValue
public abstract class ContentStreamSettings {
    @JsonProperty(FIELD_CONTENT_ENABLED)
    public abstract Boolean contentStreamEnabled();

    @JsonProperty(FIELD_RELEASES_ENABLED)
    public abstract Boolean releasesEnabled();

    @JsonProperty(FIELD_TOPICS)
    public abstract List<String> topics();

    public static Builder builder() {
        return new AutoValue_ContentStreamSettings.Builder();
    }

    @JsonCreator
    public static ContentStreamSettings create(
            @JsonProperty(FIELD_CONTENT_ENABLED) Boolean contentStreamEnabled,
            @JsonProperty(FIELD_RELEASES_ENABLED) Boolean releasesEnabled,
            @JsonProperty(FIELD_TOPICS) List<String> topics
    ) {
        return builder()
                .contentStreamEnabled(contentStreamEnabled)
                .releasesEnabled(releasesEnabled)
                .topics(topics)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty(FIELD_CONTENT_ENABLED)
        public abstract Builder contentStreamEnabled(Boolean enabled);

        @JsonProperty(FIELD_RELEASES_ENABLED)
        public abstract Builder releasesEnabled(Boolean enabled);

        @JsonProperty(FIELD_TOPICS)
        public abstract Builder topics(List<String> topics);

        public abstract ContentStreamSettings build();
    }
}
