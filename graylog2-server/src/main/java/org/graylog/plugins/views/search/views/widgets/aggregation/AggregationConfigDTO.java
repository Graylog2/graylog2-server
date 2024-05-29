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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.formatting.units.model.UnitId;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;
import org.graylog.plugins.views.search.views.units.WithConfigurableUnits;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.SortConfigDTO;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@AutoValue
@JsonTypeName(AggregationConfigDTO.NAME)
@JsonDeserialize(builder = AggregationConfigDTO.Builder.class)
public abstract class AggregationConfigDTO implements WidgetConfigDTO, WithConfigurableUnits {
    public static final String NAME = "aggregation";
    private static final String FIELD_ROW_PIVOTS = "row_pivots";
    private static final String FIELD_COLUMN_PIVOTS = "column_pivots";
    private static final String FIELD_SERIES = "series";
    private static final String FIELD_SORT = "sort";
    private static final String FIELD_VISUALIZATION = "visualization";
    private static final String FIELD_VISUALIZATION_CONFIG = "visualization_config";
    private static final String FIELD_ROLLUP = "rollup";
    private static final String FIELD_FORMATTING_SETTINGS = "formatting_settings";
    private static final String FIELD_EVENT_ANNOTATION = "event_annotation";
    private static final String FIELD_ROW_LIMIT = "row_limit";
    private static final String FIELD_COLUMN_LIMIT = "column_limit";

    @JsonProperty(FIELD_ROW_PIVOTS)
    public abstract List<PivotDTO> rowPivots();

    @Override
    @JsonProperty(UNIT_SETTINGS_PROPERTY)
    public abstract Map<String, UnitId> unitSettings();

    @JsonProperty(FIELD_COLUMN_PIVOTS)
    public abstract List<PivotDTO> columnPivots();

    @JsonProperty(FIELD_SERIES)
    public abstract List<SeriesDTO> series();

    @JsonProperty(FIELD_SORT)
    public abstract List<SortConfigDTO> sort();

    @JsonProperty(FIELD_VISUALIZATION)
    public abstract String visualization();

    @JsonProperty(FIELD_VISUALIZATION_CONFIG)
    @Nullable
    public abstract VisualizationConfigDTO visualizationConfig();

    @JsonProperty(FIELD_FORMATTING_SETTINGS)
    @Nullable
    public abstract WidgetFormattingSettings formattingSettings();

    @JsonProperty(FIELD_ROLLUP)
    public abstract boolean rollup();

    @JsonProperty(FIELD_EVENT_ANNOTATION)
    public abstract boolean eventAnnotation();

    @JsonProperty(FIELD_ROW_LIMIT)
    public Optional<Integer> rowLimit() {
        return optionalRowLimit()
                .or(() -> rowPivots().stream()
                        .filter(pivot -> pivot.config() instanceof ValueConfigDTO)
                        .map(pivot -> ((ValueConfigDTO) pivot.config()).limit())
                        .filter(OptionalInt::isPresent)
                        .map(OptionalInt::getAsInt)
                        .max(Integer::compare));
    }

    @JsonIgnore
    abstract Optional<Integer> optionalRowLimit();

    @JsonProperty(FIELD_COLUMN_LIMIT)
    public Optional<Integer> columnLimit() {
        return optionalColumnLimit()
                .or(() -> columnPivots().stream()
                        .filter(pivot -> pivot.config() instanceof ValueConfigDTO)
                        .map(pivot -> ((ValueConfigDTO) pivot.config()).limit())
                        .filter(OptionalInt::isPresent)
                        .map(OptionalInt::getAsInt)
                        .max(Integer::compare));
    }

    @JsonIgnore
    abstract Optional<Integer> optionalColumnLimit();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ROW_PIVOTS)
        public abstract Builder rowPivots(List<PivotDTO> rowPivots);

        @JsonProperty(UNIT_SETTINGS_PROPERTY)
        public abstract Builder unitSettings(Map<String, UnitId> unitSettings);

        abstract List<PivotDTO> rowPivots();

        @JsonProperty(FIELD_COLUMN_PIVOTS)
        public abstract Builder columnPivots(List<PivotDTO> columnPivots);

        abstract List<PivotDTO> columnPivots();

        @JsonProperty(FIELD_SERIES)
        public abstract Builder series(List<SeriesDTO> series);

        @JsonProperty(FIELD_SORT)
        public abstract Builder sort(List<SortConfigDTO> sort);

        @JsonProperty(FIELD_VISUALIZATION)
        public abstract Builder visualization(String visualization);

        @JsonProperty(FIELD_VISUALIZATION_CONFIG)
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = AggregationConfigDTO.FIELD_VISUALIZATION,
                visible = true)
        public abstract Builder visualizationConfig(@Nullable VisualizationConfigDTO visualizationConfig);

        @JsonProperty(FIELD_FORMATTING_SETTINGS)
        public abstract Builder formattingSettings(@Nullable WidgetFormattingSettings formattingSettings);

        @JsonProperty(FIELD_ROLLUP)
        public abstract Builder rollup(boolean rollup);

        @JsonProperty(FIELD_EVENT_ANNOTATION)
        public abstract Builder eventAnnotation(boolean eventAnnotation);

        @JsonProperty(FIELD_ROW_LIMIT)
        public abstract Builder optionalRowLimit(@Nullable Integer limit);

        abstract Optional<Integer> optionalRowLimit();

        @JsonProperty(FIELD_COLUMN_LIMIT)
        public abstract Builder optionalColumnLimit(@Nullable Integer limit);

        abstract Optional<Integer> optionalColumnLimit();

        abstract AggregationConfigDTO autoBuild();

        public AggregationConfigDTO build() {
            var rowPivots = optionalRowLimit().map(limit -> applyLimit(rowPivots(), limit)).orElse(rowPivots());
            var columnPivots = optionalColumnLimit().map(limit -> applyLimit(columnPivots(), limit)).orElse(columnPivots());
            return this.rowPivots(rowPivots)
                    .columnPivots(columnPivots)
                    .optionalRowLimit(null)
                    .optionalColumnLimit(null)
                    .autoBuild();
        }

        private List<PivotDTO> applyLimit(List<PivotDTO> pivots, int limit) {
            return pivots.stream()
                    .map(pivot -> {
                        if (pivot.config() != null && pivot.config() instanceof ValueConfigDTO config) {
                            return pivot.withConfig(config.withLimit(limit));
                        }
                        return pivot;
                    })
                    .toList();
        }

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_AggregationConfigDTO.Builder()
                    .unitSettings(Map.of())
                    .eventAnnotation(false)
                    .rollup(true);
        }
    }
}
