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
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = StoredEntityListPreferences.Builder.class)
public abstract class StoredEntityListPreferences {
    @Id
    public abstract StoredEntityListPreferencesId preferencesId();

    @JsonUnwrapped
    public abstract EntityListPreferences preferences();

    @JsonCreator
    public static StoredEntityListPreferences.Builder builder() {
        return new AutoValue_StoredEntityListPreferences.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @Id
        public abstract Builder preferencesId(final StoredEntityListPreferencesId preferencesId);

        @JsonUnwrapped
        public abstract Builder preferences(final EntityListPreferences preferences);

        public abstract StoredEntityListPreferences build();

        @JsonCreator
        public static Builder create() {
            return builder();
        }
    }

}
