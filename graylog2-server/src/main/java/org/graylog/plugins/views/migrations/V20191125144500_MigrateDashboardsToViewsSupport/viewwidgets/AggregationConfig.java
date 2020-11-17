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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AutoValue
public abstract class AggregationConfig implements ViewWidgetConfig {
    public static final String NAME = "aggregation";
    static final String FIELD_ROW_PIVOTS = "row_pivots";
    static final String FIELD_COLUMN_PIVOTS = "column_pivots";
    static final String FIELD_SERIES = "series";
    static final String FIELD_SORT = "sort";
    static final String FIELD_VISUALIZATION = "visualization";
    static final String FIELD_VISUALIZATION_CONFIG = "visualization_config";
    static final String FIELD_ROLLUP = "rollup";
    static final String FIELD_FORMATTING_SETTINGS = "formatting_settings";

    @JsonProperty(FIELD_ROW_PIVOTS)
    public abstract List<Pivot> rowPivots();

    @JsonProperty(FIELD_COLUMN_PIVOTS)
    public abstract List<Pivot> columnPivots();

    @JsonProperty(FIELD_SERIES)
    public abstract List<Series> series();

    @JsonProperty(FIELD_SORT)
    public abstract List<SortConfig> sort();

    @JsonProperty(FIELD_VISUALIZATION)
    public abstract String visualization();

    @JsonProperty(FIELD_VISUALIZATION_CONFIG)
    public abstract Optional<VisualizationConfig> visualizationConfig();

    @JsonProperty(FIELD_FORMATTING_SETTINGS)
    @Nullable
    public Object formattingSettings() { return null; }

    @JsonProperty(FIELD_ROLLUP)
    public abstract boolean rollup();

    public static Builder builder() {
        return new AutoValue_AggregationConfig.Builder()
                .columnPivots(Collections.emptyList())
                .rowPivots(Collections.emptyList())
                .sort(Collections.emptyList())
                .rollup(true);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder rowPivots(List<Pivot> rowPivots);
        public Builder rowPivots(Pivot... rowPivots) {
            return rowPivots(Arrays.asList(rowPivots));
        }
        public abstract Builder columnPivots(List<Pivot> columnPivots);
        public Builder columnPivots(Pivot... columnPivots) {
            return columnPivots(Arrays.asList(columnPivots));
        }
        public abstract Builder series(List<Series> series);
        public abstract Builder sort(List<SortConfig> sort);
        public abstract Builder visualization(String visualization);
        public abstract Builder visualizationConfig(VisualizationConfig visualizationConfig);
        public abstract Builder rollup(boolean roolup);

        public abstract AggregationConfig build();
    }
}
