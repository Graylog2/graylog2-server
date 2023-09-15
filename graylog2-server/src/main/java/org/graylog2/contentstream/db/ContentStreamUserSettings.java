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
package org.graylog2.contentstream.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class ContentStreamUserSettings {

    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_CONTENT_ENABLED = "content_stream_enabled";
    public static final String FIELD_RELEASES_ENABLED = "releases_enabled";
    public static final String FIELD_TOPICS = "content_stream_topics";

    public static Builder builder() {
        return new AutoValue_ContentStreamUserSettings.Builder();
    }

    @JsonCreator
    public static ContentStreamUserSettings create(@JsonProperty("id") @Id @ObjectId String id,
                                                   @JsonProperty(FIELD_USER_ID) String userId,
                                                   @JsonProperty(FIELD_CONTENT_ENABLED) Boolean contentStreamEnabled,
                                                   @JsonProperty(FIELD_RELEASES_ENABLED) Boolean releasesEnabled,
                                                   @JsonProperty(FIELD_TOPICS) List<String> topicList
    ) {
        return builder()
                .id(id)
                .userId(userId)
                .contentStreamEnabled(contentStreamEnabled)
                .releasesEnabled(releasesEnabled)
                .topics(topicList != null ? topicList : new ArrayList<>())
                .build();
    }

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty(FIELD_USER_ID)
    public abstract String userId();

    @JsonProperty(FIELD_CONTENT_ENABLED)
    public abstract Boolean contentStreamEnabled();

    @JsonProperty(FIELD_RELEASES_ENABLED)
    public abstract Boolean releasesEnabled();

    @JsonProperty(FIELD_TOPICS)
    public abstract List<String> topics();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder id(String id);

        public abstract Builder userId(String userId);

        public abstract Builder contentStreamEnabled(Boolean contentStreamEnabled);

        public abstract Builder releasesEnabled(Boolean releasesEnabled);

        public abstract Builder topics(List<String> topicList);

        public abstract ContentStreamUserSettings build();
    }
}
