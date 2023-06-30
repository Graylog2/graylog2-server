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
package org.graylog2.contentStream.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.contentStream.rest.AutoValue_ContentStreamUserSettings;

@AutoValue
public abstract class ContentStreamUserSettings {

    public static Builder builder() {
        return new AutoValue_ContentStreamUserSettings.Builder();
    }

    @JsonCreator
    public static ContentStreamUserSettings create(@JsonProperty("contentStream_enabled") Boolean enabled) {
        return builder()
                .contentStreamEnabled(enabled)
                .build();
    }

    @JsonProperty
    public abstract Boolean contentStreamEnabled();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder contentStreamEnabled(Boolean enabled);

        public abstract ContentStreamUserSettings build();
    }
}
