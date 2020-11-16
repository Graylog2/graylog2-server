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
public abstract class NumberVisualizationConfig implements VisualizationConfig {
    public static final String NAME = "numeric";
    private static final String FIELD_TREND = "trend";
    private static final String FIELD_TREND_PREFERENCE = "trend_preference";

    public enum TrendPreference {
        LOWER,
        NEUTRAL,
        HIGHER;
    }

    @JsonProperty
    public abstract boolean trend();

    @JsonProperty
    public abstract TrendPreference trendPreference();

    public static Builder builder() {
        return new AutoValue_NumberVisualizationConfig.Builder()
                .trend(false)
                .trendPreference(TrendPreference.NEUTRAL);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder trend(boolean trend);

        public abstract Builder trendPreference(TrendPreference trendPreference);

        public abstract NumberVisualizationConfig build();

    }
}
