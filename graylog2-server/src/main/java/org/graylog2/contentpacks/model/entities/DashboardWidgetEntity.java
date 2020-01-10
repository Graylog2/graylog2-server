/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;
import org.graylog.plugins.views.search.views.WidgetPositionDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AreaVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.Interpolation;
import org.graylog.plugins.views.search.views.widgets.aggregation.LineVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.NumberVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.PivotDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.SeriesConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.SeriesDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.ValueConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.VisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.PivotSortConfig;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.SortConfigDTO;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class DashboardWidgetEntity {
    private Map<String, ValueReference> parameters;
    private Map<String, Object> config;

    @JsonProperty("id")
    @NotNull
    public abstract ValueReference id();

    @JsonProperty("description")
    @NotNull
    public abstract ValueReference description();

    @JsonProperty("type")
    @NotBlank
    public abstract ValueReference type();

    @JsonProperty("cache_time")
    @PositiveOrZero
    public abstract ValueReference cacheTime();

    @JsonProperty("time_range")
    @NotNull
    public abstract TimeRangeEntity timeRange();

    @JsonProperty("configuration")
    @NotNull
    public abstract ReferenceMap configuration();

    @JsonProperty("position")
    public abstract Optional<Position> position();

    @JsonCreator
    public static DashboardWidgetEntity create(
            @JsonProperty("id") @NotNull ValueReference id,
            @JsonProperty("description") @NotNull ValueReference description,
            @JsonProperty("type") @NotBlank ValueReference type,
            @JsonProperty("cache_time") @PositiveOrZero ValueReference cacheTime,
            @JsonProperty("time_range") @NotNull TimeRangeEntity timeRange,
            @JsonProperty("configuration") @NotNull ReferenceMap configuration,
            @JsonProperty("position") @Nullable Position position) {
        return new AutoValue_DashboardWidgetEntity(id, description, type, cacheTime, timeRange, configuration, Optional.ofNullable(position));
    }

    public Map<String, Object> config() {
        if (config != null) {
            return config;
        }
        config = ReferenceMapUtils.toValueMap(configuration(), parameters);
        return config;
    }

    public List<WidgetEntity> convert(Map<String, ValueReference> parameters) {
        this.parameters = parameters;
        final String type = type().asString(parameters);

        try {
            switch (type) {
                case "SEARCH_RESULT_CHART": {
                    return createHistogramWidget();
                }
                case "FIELD_CHART": {
                    return createFieldChartWidget();
                }

                case "STACKED_CHART": {
                    return createStackedChartWidget();
                }
                case "STATS_COUNT": {
                    return createStatsCountWidget();
                }
                case "QUICKVALUES":
                    return createQuickValueWidgets();
                default: {
                    throw new IllegalArgumentException(
                            "The provided entity does not have a valid Widget type: " + type);
                }
            }
        } catch (InvalidRangeParametersException e) {
            throw new IllegalArgumentException(
                    "The provided entity does not have a valid timerange type: " + e.getMessage());
        }
    }

    private WidgetConfigDTO defaultWidgetConfig() {
        return AggregationConfigDTO.builder()
                .series(ImmutableList.of(
                        SeriesDTO.builder()
                                .config(SeriesConfigDTO.empty())
                                .function("count()").build()
                ))
                .rowPivots(ImmutableList.of(PivotDTO.builder()
                        .type("time")
                        .field("timestamp")
                        .config(TimeHistogramConfigDTO.builder()
                                .interval(AutoIntervalDTO.builder().build()).build())
                        .build()))
                .visualization("bar")
                .columnPivots(Collections.emptyList())
                .sort(Collections.emptyList())
                .build();
    }

    private WidgetEntity.Builder aggregationWidgetBuilder() throws InvalidRangeParametersException {
        final WidgetEntity.Builder widgetEntityBuilder = WidgetEntity.builder()
                .type(AggregationConfigDTO.NAME)
                .id(id().asString(parameters))
                .timerange(timeRange().convert(parameters));

        final ImmutableSet.Builder<String> streams = new ImmutableSet.Builder<>();
        final Object streamId = config().get("stream_id");
        if (streamId instanceof String) {
            streams.add((String) streamId);
            widgetEntityBuilder.streams(streams.build());
        }

        return widgetEntityBuilder;
    }

    private List<WidgetEntity> createHistogramWidget() throws InvalidRangeParametersException {
        final WidgetConfigDTO widgetConfigDTO = defaultWidgetConfig();
        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder()
                .config(widgetConfigDTO);
        final Object query = config().get("query");
        if (query instanceof String) {
            widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString((String) query).build());
        }


        return ImmutableList.of(widgetEntityBuilder.build());
    }

    private String mapRendererToVisualization(String renderer) {
        switch (renderer) {
            case "bar":
            case "line":
            case "area":
                return renderer;
            case "scatterplot": return "scatter";
        }
        throw new RuntimeException("Unable to map renderer to visualization: " + renderer);
    }

    private Interpolation fromLegacyInterpolation(String legacyValue) {
        switch (legacyValue) {
            case "linear": return Interpolation.linear;
            case "step-after": return Interpolation.stepAfter;
            case "cardinal":
            case "basis":
            case "bundle":
            case "monotone": return Interpolation.spline;
        }
        throw new RuntimeException("Invalid interpolation value: " + legacyValue);
    }

    private Optional<VisualizationConfigDTO> createVisualizationConfig() {
        final String renderer = (String) config().get("renderer");
        final String interpolation = (String) config().get("interpolation");
        switch (renderer) {
            case "line":
                return Optional.of(
                        LineVisualizationConfigDTO.builder()
                                .interpolation(fromLegacyInterpolation(interpolation))
                                .build()
                );
            case "area":
                return Optional.of(
                        AreaVisualizationConfigDTO.builder()
                                .interpolation(fromLegacyInterpolation(interpolation))
                                .build()
                );
        }
        return Optional.empty();
    }

    private WidgetEntity fieldChartWidget(String renderer, String valueType, String field, String query) throws InvalidRangeParametersException {
        final AggregationConfigDTO.Builder configBuilder = AggregationConfigDTO.builder()
                .series(ImmutableList.of(createSeriesDTO(valueType, field)))
                .visualization(mapRendererToVisualization(renderer))
                .columnPivots(Collections.emptyList())
                .sort(Collections.emptyList())
                .rowPivots(Collections.singletonList(
                        PivotDTO.builder()
                                .field("timestamp")
                                .type("time")
                                .config(TimeHistogramConfigDTO.builder().interval(AutoIntervalDTO.builder().build()).build())
                                .build()
                ));

        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder();
        if (query != null) {
            widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString((String) query).build());
        }
        return widgetEntityBuilder
                .config(createVisualizationConfig().map(configBuilder::visualizationConfig).orElse(configBuilder).build())
                .build();
    }

    private List<WidgetEntity> createFieldChartWidget() throws InvalidRangeParametersException {
        final String renderer = (String) config().get("renderer");
        final String valueType = (String) config().get("valuetype");
        final String field = (String) config().get("field");
        final String query = (String) config().get("query");
        return ImmutableList.of(fieldChartWidget(renderer, valueType, field, query));
    }

    private List<WidgetEntity> createStackedChartWidget() {
        final String renderer = (String) config().get("renderer");
        final List<Map <String, Object>> series = (List) config().get("series");

        return series.stream().map(seriesConfig -> {
            final String valueType = (String) seriesConfig.get("statistical_function");
            final String field = (String) seriesConfig.get("field");
            final String query = (String) seriesConfig.get("query");
            try {
                return fieldChartWidget(renderer, valueType, field, query);
            } catch (InvalidRangeParametersException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<WidgetEntity> createStatsCountWidget() throws InvalidRangeParametersException {
        final String function = (String) config().get("stats_function");
        final String field = (String) config().get("field");
        final boolean trend = (boolean) config().get("trend");
        final boolean lowerIsBetter = (boolean) config().get("lower_is_better");
        final AggregationConfigDTO widgetConfig = AggregationConfigDTO.builder()
                .series(ImmutableList.of(createSeriesDTO(function, field)))
                .visualization("numeric")
                .visualizationConfig(NumberVisualizationConfigDTO.builder()
                        .trend(trend)
                        .trendPreference(
                                lowerIsBetter
                                        ? NumberVisualizationConfigDTO.TrendPreference.LOWER
                                        : NumberVisualizationConfigDTO.TrendPreference.HIGHER)
                        .build()
                )
                .rowPivots(Collections.emptyList())
                .columnPivots(Collections.emptyList())
                .sort(Collections.emptyList())
                .build();
        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder()
                .config(widgetConfig);
        final String query = (String) config().get("query");
        if (query != null) {
            widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString(query).build());
        }

        return ImmutableList.of(widgetEntityBuilder.build());
    }

    private List<WidgetEntity> createQuickValueWidgets() throws InvalidRangeParametersException {
        final List<WidgetEntity> result = new ArrayList<>(2);
        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder();

        final boolean showChart = (boolean) config().get("show_pie_chart");
        final boolean showTable = (boolean) config().get("show_data_table");
        final String stackedFields = (String) config().get("stacked_fields");
        final String query = (String) config().get("query");
        final String field = (String) config().get("field");
        final int limit = (int) config().get("limit");
        final int dataTableLimit = (int) config().get("data_table_limit");
        final String sortOrder = (String) config().get("sort_order");

        SortConfigDTO.Direction dir = sortOrder.matches("desc")
                ? SortConfigDTO.Direction.Descending
                : SortConfigDTO.Direction.Ascending;

        AggregationConfigDTO.Builder aggregationConfigBuilder = AggregationConfigDTO.builder()
                .columnPivots(Collections.emptyList())
                .series(ImmutableList.of(
                        SeriesDTO.builder()
                                .config(SeriesConfigDTO.empty())
                                .function("count()").build()
                ))
                .sort(ImmutableList.of(PivotSortConfig.create(field, dir)));

        if (query != null) {
            widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString(query).build());
        }
        if (showChart) {
            result.add(widgetEntityBuilder
                    .config(aggregationConfigBuilder
                            .rowPivots(genPivotForPie(field, stackedFields, limit))
                            .visualization("pie").build())
                    .build());

        }
        if (showTable) {
            result.add(widgetEntityBuilder.config(
                    aggregationConfigBuilder.visualization("table")
                            .rowPivots(genPivotForPie(field, stackedFields, dataTableLimit))
                            .build()).build());
        }
        return result;
    }

    private List<PivotDTO> genPivotForPie(String field, String stackedFields, int limit) {
        final PivotDTO fieldPivot = PivotDTO.builder()
                .type("values")
                .field(field)
                .config(ValueConfigDTO.builder().limit(limit).build())
                .build();
        final List<PivotDTO> rowPivots = new ArrayList<>();
        rowPivots.addAll(stackedFieldPivots(stackedFields));
        rowPivots.add(fieldPivot);
        return rowPivots;
    }

    private List<PivotDTO> stackedFieldPivots(String fieldNames) {
        return Strings.isNullOrEmpty(fieldNames)
                ? Collections.emptyList()
                : Splitter.on(",")
                .splitToList(fieldNames)
                .stream()
                .map(fieldName -> PivotDTO.builder()
                        .field(fieldName)
                        .config(ValueConfigDTO.builder().limit(15).build())
                        .build())
                .collect(Collectors.toList());
    }

    private SeriesDTO createSeriesDTO(String valueType, String field) {
        String function;
        switch (valueType) {
            case "cardinality": {
                function = "card";
                break;
            }
            case "mean":
                function = "avg";
                break;
            case "total":
                function = "sum";
                break;
            case "std_dev":
                function = "stddev";
                break;
            default: {
                function = valueType;
                break;
            }
        }
        return SeriesDTO.builder().config(SeriesConfigDTO.empty()).function(function + "(" + field + ")").build();
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    public static abstract class Position {
        @JsonProperty("width")
        @PositiveOrZero
        public abstract ValueReference width();

        @JsonProperty("height")
        @PositiveOrZero
        public abstract ValueReference height();

        @JsonProperty("row")
        @PositiveOrZero
        public abstract ValueReference row();

        @JsonProperty("col")
        @PositiveOrZero
        public abstract ValueReference col();

        @JsonCreator
        public static Position create(@JsonProperty("width") @PositiveOrZero ValueReference width,
                                      @JsonProperty("height") @PositiveOrZero ValueReference height,
                                      @JsonProperty("row") @PositiveOrZero ValueReference row,
                                      @JsonProperty("col") @PositiveOrZero ValueReference col) {
            return new AutoValue_DashboardWidgetEntity_Position(width, height, row, col);
        }

        public WidgetPositionDTO convert(Map<String, ValueReference> parameters) {
            return WidgetPositionDTO.builder()
                    .col(org.graylog.plugins.views.search.views.Position.fromInt(col().asInteger(parameters)))
                    .row(org.graylog.plugins.views.search.views.Position.fromInt(row().asInteger(parameters)))
                    .height(org.graylog.plugins.views.search.views.Position.fromInt(height().asInteger(parameters)))
                    .width(org.graylog.plugins.views.search.views.Position.fromInt(width().asInteger(parameters)))
                    .build();
        }
    }
}
