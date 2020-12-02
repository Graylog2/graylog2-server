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
package org.graylog2.lookup.dto;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_CacheDto.Builder.class)
public abstract class CacheDto {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_NAME = "name";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty("content_pack")
    @Nullable
    public abstract String contentPack();

    @JsonProperty("config")
    public abstract LookupCacheConfiguration config();

    public static Builder builder() {
        return new AutoValue_CacheDto.Builder();
    }

    @JsonAutoDetect
    @AutoValue.Builder
    public abstract static class Builder {
        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(@Nullable String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty("content_pack")
        public abstract Builder contentPack(@Nullable String contentPack);

        @JsonProperty("config")
        public abstract Builder config(LookupCacheConfiguration config);

        public abstract CacheDto build();
    }
}
