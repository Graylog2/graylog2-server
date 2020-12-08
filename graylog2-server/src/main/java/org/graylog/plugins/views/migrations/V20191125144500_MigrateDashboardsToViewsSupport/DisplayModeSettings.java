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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

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
abstract class DisplayModeSettings {
    private static final String FIELD_POSITIONS = "positions";

    @JsonProperty(FIELD_POSITIONS)
    abstract Map<String, ViewWidgetPosition> positions();

    static DisplayModeSettings empty() {
        return Builder.create().build();
    }

    @AutoValue.Builder
    static abstract class Builder {
        @JsonProperty(FIELD_POSITIONS)
        abstract Builder positions(Map<String, ViewWidgetPosition> positions);

        abstract DisplayModeSettings build();

        @JsonCreator
        static Builder create() {
            return new AutoValue_DisplayModeSettings.Builder()
                    .positions(Collections.emptyMap());
        }
    }
}
