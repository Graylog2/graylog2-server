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
package org.graylog2.notifications;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SystemNotificationDto.Builder.class)
public abstract class SystemNotificationDto implements BuildableMongoEntity<SystemNotificationDto, SystemNotificationDto.Builder> {

    public static final String FIELD_TYPE = "type";
    public static final String FIELD_KEY = "key";
    public static final String FIELD_PRIORITY = "priority";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_DETAILS = "details";
    public static final String FIELD_IS_READ = "is_read";
    public static final String FIELD_ACTOR = "actor";
    public static final String FIELD_LAST_CHANGED = "last_changed";
    public static final String FIELD_TRIGGERED_AT = "triggered_at";

    @Override
    @Nullable
    @Id
    @ObjectId
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @Nullable
    @JsonProperty(FIELD_KEY)
    public abstract String key();

    @JsonProperty(FIELD_PRIORITY)
    public abstract String priority();

    @JsonProperty(FIELD_NODE_ID)
    public abstract String nodeId();

    @Nullable
    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @Nullable
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_DETAILS)
    public abstract Map<String, Object> details();

    @JsonProperty(FIELD_IS_READ)
    public abstract boolean isRead();

    @Nullable
    @JsonProperty(FIELD_ACTOR)
    public abstract Actor actor();

    @Nullable
    @JsonProperty(FIELD_LAST_CHANGED)
    @JsonSerialize(using = MongoInstantSerializer.class)
    @JsonDeserialize(using = MongoInstantDeserializer.class)
    public abstract Instant lastChanged();

    @JsonProperty(FIELD_TRIGGERED_AT)
    @JsonSerialize(using = MongoInstantSerializer.class)
    @JsonDeserialize(using = MongoInstantDeserializer.class)
    public abstract Instant triggeredAt();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public abstract Builder toBuilder();

    @AutoValue
    @JsonAutoDetect
    @JsonDeserialize(builder = Actor.Builder.class)
    public abstract static class Actor {

        @JsonProperty("id")
        public abstract String id();

        @JsonProperty("name")
        public abstract String name();

        public static Actor system() {
            return Actor.Builder.create().id("system").name("system").build();
        }

        public static Actor create(String id, String name) {
            return Actor.Builder.create().id(id).name(name).build();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_SystemNotificationDto_Actor.Builder();
            }

            @JsonProperty("id")
            public abstract Builder id(String id);

            @JsonProperty("name")
            public abstract Builder name(String name);

            public abstract Actor build();
        }
    }

    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<SystemNotificationDto, Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SystemNotificationDto.Builder()
                    .isRead(false)
                    .details(Map.of());
        }

        @Override
        @Id
        @ObjectId
        @JsonProperty("id")
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_KEY)
        public abstract Builder key(@Nullable String key);

        @JsonProperty(FIELD_PRIORITY)
        public abstract Builder priority(String priority);

        @JsonProperty(FIELD_NODE_ID)
        public abstract Builder nodeId(String nodeId);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(@Nullable String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(@Nullable String description);

        @JsonProperty(FIELD_DETAILS)
        public abstract Builder details(Map<String, Object> details);

        @JsonProperty(FIELD_IS_READ)
        public abstract Builder isRead(boolean isRead);

        @JsonProperty(FIELD_ACTOR)
        public abstract Builder actor(@Nullable Actor actor);

        @JsonProperty(FIELD_LAST_CHANGED)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        public abstract Builder lastChanged(@Nullable Instant lastChanged);

        @JsonProperty(FIELD_TRIGGERED_AT)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        public abstract Builder triggeredAt(Instant triggeredAt);

        public abstract SystemNotificationDto build();
    }
}
