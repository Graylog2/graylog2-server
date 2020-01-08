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

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;
import org.graylog.plugins.views.search.views.WidgetPositionDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AreaVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.Interpolation;
import org.graylog.plugins.views.search.views.widgets.aggregation.LineVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.PivotDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.SeriesConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.SeriesDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.VisualizationConfigDTO;
import org.graylog2.contentpacks.facades.dashboardV1.TimeIntervalMapper;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

                case "STACKED_CHART":
                case "STATS_COUNT":
                case "QUICKVALUES":
                    return ImmutableList.of();
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

        final Object query = config().get("query");
        if (query instanceof String) {
            widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString((String) query).build());
        }

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

    private List<WidgetEntity> createFieldChartWidget() throws InvalidRangeParametersException {
        final String renderer = (String) config().get("renderer");

        final AggregationConfigDTO.Builder configBuilder = AggregationConfigDTO.builder()
                .series(ImmutableList.of(createSeriesDTO()))
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
        return ImmutableList.of(aggregationWidgetBuilder()
                .config(createVisualizationConfig().map(configBuilder::visualizationConfig).orElse(configBuilder).build())
                .build());
    }

    private SeriesDTO createSeriesDTO() {
        final String valueType = (String) config().get("valuetype");
        final String field = (String) config().get("field");
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
                throw new IllegalArgumentException(
                        "The provided entity does not have a valid TimeRange type: " + valueType);
            }
        }
        return SeriesDTO.builder().config(SeriesConfigDTO.empty()).function(function + "(" + field + ")").build();
    }

    private SeriesSpec createSeries(String valueType, String field) {
        switch (valueType) {
            case "cardinality": {
                return Cardinality.builder().field(field).id("card(" + field + ")").build();
            }
            case "mean": {
                return Average.builder().field(field).id("avg(" + field + ")").build();
            }
            case "max": {
                return Max.builder().field(field).id("max(" + field + ")").build();
            }
            case "min": {
                return Min.builder().field(field).id("min(" + field + ")").build();
            }
            case "sum":
            case "total": {
                return Sum.builder().field(field).id("sum(" + field + ")").build();
            }
            case "variance": {
                return Variance.builder().field(field).id("variance(" + field + ")").build();
            }
            case "std_deviation": {
                return StdDev.builder().field(field).id("stddev(" + field + ")").build();
            }
            case "count": {
                return Count.builder().field(field).id("count(" + field + ")").build();
            }
            default: {
                throw new IllegalArgumentException(
                        "The provided entity does not have a valid TimeRange type: " + valueType);
            }
        }
    }

    public List<SearchType> createSearchType() throws InvalidRangeParametersException {
        final Map<String, Object> config = ReferenceMapUtils.toValueMap(configuration(), parameters);
        final String type = type().asString(parameters);

        List<Pivot.Builder> pivotBuilder;
        switch(type) {
            case "SEARCH_RESULT_CHART": {
                pivotBuilder = createHistogram();
                break;
            }
            case "FIELD_CHART": {
                pivotBuilder = createFieldChart();
                break;
            }
            case "STACKED_CHART":
            case "STATS_COUNT":
            case "QUICKVALUES":
                return ImmutableList.of();
            default: {
                throw new IllegalArgumentException(
                        "The provided entity does not have a valid Widget type: " + type);
            }
        }

        final TimeRangeEntity timeRangeEntity = timeRange();
        final TimeRange timeRange = timeRangeEntity.convert(parameters);
        pivotBuilder = pivotBuilder.stream().map(p ->p.timerange(timeRange)).collect(Collectors.toList());

        final Object query = config.get("query");
        if (query instanceof String) {
            pivotBuilder = pivotBuilder.stream().map(p ->
                    p.query(ElasticsearchQueryString.builder()
                            .queryString((String) query).build()))
                    .collect(Collectors.toList());
        }
        return pivotBuilder.stream().map(Pivot.Builder::build).collect(Collectors.toList());
    }

    private Pivot.Builder createPivotBuilder() {
        final ImmutableSet.Builder<String> streams = new ImmutableSet.Builder<>();
        final Pivot.Builder pivotBuilder = Pivot.builder()
                .id(new UUID().toString())
                .rollup(true);

        final Object streamId = config().get("stream_id");
        if (streamId instanceof String) {
            streams.add((String) streamId);
            pivotBuilder.streams(streams.build());
        }
        return pivotBuilder;
    }

    private List<Pivot.Builder> createHistogram() {
        Pivot.Builder pivotBuilder = createPivotBuilder();
        final String interval = (String) config().get("interval");
        pivotBuilder.series(ImmutableList.of(Count.builder().id("count()").build()));
        pivotBuilder.rowGroups(ImmutableList.of(Time.builder().field("timestamp").interval(TimeIntervalMapper.map(interval)).build()))
                .id(new UUID().toString())
                .rollup(true);
        return ImmutableList.of(pivotBuilder);
    }

    private List<Pivot.Builder> createFieldChart() {
        Pivot.Builder pivotBuilder = createPivotBuilder();
        final String interval = (String) config().get("interval");
        String valueType = (String) config().get("valuetype");
        String field = (String) config().get("field");
        pivotBuilder.series(ImmutableList.of(createSeries(valueType, field)));
        pivotBuilder.rowGroups(ImmutableList.of(Time.builder().field("timestamp").interval(TimeIntervalMapper.map(interval)).build()))
                .id(new UUID().toString())
                .rollup(true);
        return ImmutableList.of(pivotBuilder);
    }

    private List<Pivot.Builder> createStackedChart(Map<String, Object> config) {
        return ImmutableList.of();
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
