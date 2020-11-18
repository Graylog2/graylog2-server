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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = DisplayModeSettings.Builder.class)
public abstract class DisplayModeSettings {
    private static final String FIELD_POSITIONS = "positions";

    @JsonProperty(FIELD_POSITIONS)
    public abstract Map<String, WidgetPositionDTO> positions();

    public static DisplayModeSettings empty() {
        return Builder.create().build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_POSITIONS)
        public abstract Builder positions(Map<String, WidgetPositionDTO> positions);

        public abstract DisplayModeSettings build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_DisplayModeSettings.Builder()
                    .positions(Collections.emptyMap());
        }
    }
}
