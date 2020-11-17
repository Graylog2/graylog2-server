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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Viewport {
    @JsonProperty("center_x")
    public abstract double centerX();

    @JsonProperty("center_y")
    public abstract double centerY();

    @JsonProperty
    public abstract int zoom();

    private static Builder builder() {
        return new AutoValue_Viewport.Builder();
    }

    static Viewport empty() {
        return Viewport.builder()
                .centerX(0)
                .centerY(0)
                .zoom(32)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("center_x")
        public abstract Builder centerX(double centerX);

        @JsonProperty("center_y")
        public abstract Builder centerY(double centerY);

        @JsonProperty
        public abstract Builder zoom(int zoom);

        public abstract Viewport build();
    }
}
