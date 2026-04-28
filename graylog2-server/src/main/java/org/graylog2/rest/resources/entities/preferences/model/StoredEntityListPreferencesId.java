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
package org.graylog2.rest.resources.entities.preferences.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = StoredEntityListPreferencesId.Builder.class)
public abstract class StoredEntityListPreferencesId {

    public static final String USER_ID_SUB_FIELD = "user_id";
    public static final String GENERAL_LAYOUT_VARIANT = "#general#";

    @Nullable
    @JsonProperty(USER_ID_SUB_FIELD)
    public abstract String userId();

    @JsonProperty("entity_list_id")
    public abstract String entityListId();

    @Nullable
    @JsonProperty("layout_variant")
    public abstract String layoutVariant();

    @JsonCreator
    public static Builder builder() {
        return new AutoValue_StoredEntityListPreferencesId.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonProperty(USER_ID_SUB_FIELD)
        public abstract Builder userId(@Nullable final String userId);

        @JsonProperty("entity_list_id")
        public abstract Builder entityListId(final String entityListId);

        @JsonProperty("layout_variant")
        public abstract Builder layoutVariant(@Nullable final String layoutVariant);

        public abstract StoredEntityListPreferencesId build();

        @JsonCreator
        public static Builder create() {
            return builder();
        }
    }
}
