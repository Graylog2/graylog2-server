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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class AggregationConfig {
    public static final String NAME = "aggregation";
    private static final String TIMESTAMP_FIELD = "timestamp";

    static final String FIELD_ROW_PIVOTS = "row_pivots";
    static final String FIELD_COLUMN_PIVOTS = "column_pivots";
    static final String FIELD_SERIES = "series";
    static final String FIELD_SORT = "sort";
    static final String FIELD_VISUALIZATION = "visualization";
    static final String FIELD_ROLLUP = "rollup";
    static final String FIELD_FORMATTING_SETTINGS = "formatting_settings";

    @JsonProperty(FIELD_ROW_PIVOTS)
    public List<Pivot> rowPivots() {
        return Collections.singletonList(Pivot.timeBuilder()
                .config(TimeHistogramConfig.create())
                .field(TIMESTAMP_FIELD)
                .build());
    }

    @JsonProperty(FIELD_COLUMN_PIVOTS)
    List<Pivot> columnPivots() {
        return Collections.emptyList();
    }

    @JsonProperty(FIELD_SERIES)
    public List<Series> series() {
        return Collections.singletonList(Series.create());
    }

    @JsonProperty(FIELD_SORT)
    public List<Object> sort() {
        return Collections.emptyList();
    }

    @JsonProperty(FIELD_VISUALIZATION)
    public String visualization() {
        return "bar";
    }

    @JsonProperty(FIELD_FORMATTING_SETTINGS)
    @Nullable
    public Object formattingSettings() { return null; }

    @JsonProperty(FIELD_ROLLUP)
    public boolean rollup() {
        return true;
    }

    public static AggregationConfig create() {
        return new AutoValue_AggregationConfig();
    }
}
