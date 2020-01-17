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
package org.graylog2.contentpacks.facades.dashboardV1;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AreaVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.BarVisualizationConfigDTO;
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
import org.graylog2.contentpacks.model.entities.DashboardWidgetEntity;
import org.graylog2.contentpacks.model.entities.WidgetConfig;
import org.graylog2.contentpacks.model.entities.WidgetEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DashboardWidgetConverter {
    private Map<String, ValueReference> parameters;
    private RandomUUIDProvider randomUUIDProvider;
    private DashboardWidgetEntity dashboardWidgetEntity;
    private WidgetConfig config;

    public DashboardWidgetConverter(DashboardWidgetEntity dashboardWidgetEntity,
                                    Map<String, ValueReference> parameters,
                                    RandomUUIDProvider randomUUIDProvider) {
        this.dashboardWidgetEntity = dashboardWidgetEntity;
        this.parameters = parameters;
        this.randomUUIDProvider = randomUUIDProvider;
        this.config = new WidgetConfig(dashboardWidgetEntity.configuration(), parameters);
    }

    public static List<WidgetEntity> convert(DashboardWidgetEntity dashboardWidgetEntity,
                                             Map<String, ValueReference> parameters,
                                             RandomUUIDProvider randomUUIDProvider) {
        final DashboardWidgetConverter converter = new DashboardWidgetConverter(
                dashboardWidgetEntity, parameters, randomUUIDProvider);
        return converter.convert();
    }

    public List<WidgetEntity> convert() {
        final String type = dashboardWidgetEntity.type().asString(parameters);

        try {
            switch (type) {
                case "SEARCH_RESULT_CHART":
                    return createHistogramWidget();
                case "FIELD_CHART":
                    return createFieldChartWidget();
                case "STACKED_CHART":
                    return createStackedChartWidget();
                case "STATS_COUNT":
                    return createStatsCountWidget();
                case "QUICKVALUES":
                    return createQuickValueWidgets();
                case "STREAM_SEARCH_RESULT_COUNT":
                case "SEARCH_RESULT_COUNT":
                    return createSearchResultCount();
                case "QUICKVALUES_HISTOGRAM":
                    return createQuickValueHistogramWidgets();
                case "org.graylog.plugins.map.widget.strategy.MapWidgetStrategy":
                    return createMapWidget();
                default: {
                    throw new IllegalArgumentException(
                            "The provided entity does not have a valid Widget type: " + type);
                }
            }
        } catch (
                InvalidRangeParametersException e) {
            throw new IllegalArgumentException(
                    "The provided entity does not have a valid timerange type: " + e.getMessage());
        }
    }

    private WidgetConfigDTO defaultWidgetConfig() {
        return AggregationConfigDTO.Builder.builder()
                .series(ImmutableList.of(
                        SeriesDTO.Builder.create()
                                .config(SeriesConfigDTO.empty())
                                .function("count()").build()
                ))
                .rowPivots(ImmutableList.of(PivotDTO.Builder.builder()
                        .type("time")
                        .field("timestamp")
                        .config(TimeHistogramConfigDTO.Builder.builder()
                                .interval(AutoIntervalDTO.Builder.builder().build()).build())
                        .build()))
                .visualization("bar")
                .columnPivots(Collections.emptyList())
                .sort(Collections.emptyList())
                .build();
    }

    private WidgetEntity.Builder aggregationWidgetBuilder() throws InvalidRangeParametersException {
        final WidgetEntity.Builder widgetEntityBuilder = WidgetEntity.builder()
                .type(AggregationConfigDTO.NAME)
                .id(randomUUIDProvider.get())
                .timerange(dashboardWidgetEntity.timeRange().convert(parameters));

        final Optional<String> streamId = config.getOptionalString("stream_id");
        if (streamId.isPresent()) {
            final ImmutableSet.Builder<String> streams = new ImmutableSet.Builder<>();
            streams.add(streamId.get());
            widgetEntityBuilder.streams(streams.build());
        }

        return widgetEntityBuilder;
    }

    private List<WidgetEntity> createHistogramWidget() throws InvalidRangeParametersException {
        final WidgetConfigDTO widgetConfigDTO = defaultWidgetConfig();
        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder()
                .config(widgetConfigDTO);
        final Optional<String> query = config.getOptionalString("query");
        query.ifPresent(s -> widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString(s).build()));


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
        final String renderer = config.getString("renderer");
        final String interpolation = config.getString("interpolation");
        switch (renderer) {
            case "line":
                return Optional.of(
                        LineVisualizationConfigDTO.Builder.builder()
                                .interpolation(fromLegacyInterpolation(interpolation))
                                .build()
                );
            case "area":
                return Optional.of(
                        AreaVisualizationConfigDTO.Builder.builder()
                                .interpolation(fromLegacyInterpolation(interpolation))
                                .build()
                );
        }
        return Optional.empty();
    }

    private WidgetEntity fieldChartWidget(String renderer,
                                          String valueType,
                                          String field,
                                          String query) throws InvalidRangeParametersException {
        final AggregationConfigDTO.Builder configBuilder = AggregationConfigDTO.Builder.builder()
                .series(ImmutableList.of(createSeriesDTO(valueType, field)))
                .visualization(mapRendererToVisualization(renderer))
                .columnPivots(Collections.emptyList())
                .sort(Collections.emptyList())
                .rowPivots(Collections.singletonList(
                        PivotDTO.Builder.builder()
                                .field("timestamp")
                                .type("time")
                                .config(TimeHistogramConfigDTO.Builder.builder().interval(AutoIntervalDTO.Builder.builder().build()).build())
                                .build()
                ));

        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder();
        widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString(query).build());
        return widgetEntityBuilder
                .config(createVisualizationConfig().map(configBuilder::visualizationConfig).orElse(configBuilder).build())
                .build();
    }

    private List<WidgetEntity> createFieldChartWidget() throws InvalidRangeParametersException {
        final String renderer = config.getString("renderer");
        final String valueType = config.getString("valuetype");
        final String field = config.getString("field");
        final String query = config.getOptionalString("query").orElse("");
        return ImmutableList.of(fieldChartWidget(renderer, valueType, field, query));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<WidgetEntity> createStackedChartWidget() {
        final String renderer = config.getString("renderer");
        final List<Map <String, Object>> series = (List) config.getList("series");

        return series.stream().map(seriesConfig -> {
            final String valueType = (String) seriesConfig.get("statistical_function");
            final String field = (String) seriesConfig.get("field");
            final String query = (String) seriesConfig.getOrDefault("query", "");
            try {
                return fieldChartWidget(renderer, valueType, field, query);
            } catch (InvalidRangeParametersException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<WidgetEntity> createStatsCountWidget() throws InvalidRangeParametersException {
        final String function = config.getString("stats_function");
        final String field = config.getString("field");
        final boolean trend = config.getBoolean("trend");
        final boolean lowerIsBetter = config.getBoolean("lower_is_better");
        final AggregationConfigDTO widgetConfig = AggregationConfigDTO.Builder.builder()
                .series(ImmutableList.of(createSeriesDTO(function, field)))
                .visualization("numeric")
                .visualizationConfig(NumberVisualizationConfigDTO.Builder.builder()
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
        final Optional<String> query = config.getOptionalString("query");
        query.ifPresent(s -> widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString(s).build()));

        return ImmutableList.of(widgetEntityBuilder.build());
    }

    private List<WidgetEntity> createQuickValueWidgets() throws InvalidRangeParametersException {
        final List<WidgetEntity> result = new ArrayList<>(2);
        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder();

        final boolean showChart = config.getBoolean("show_pie_chart");
        final boolean showTable = config.getBoolean("show_data_table");
        final String field = config.getString("field");
        final String stackedFields = config.getOptionalString("stacked_fields").orElse("");
        final Integer limit = config.getOptionalInteger("limit").orElse(5);
        final Integer dataTableLimit = config.getOptionalInteger("data_table_limit").orElse(15);
        final String sortOrder = config.getOptionalString("sort_order").orElse("desc");

        SortConfigDTO.Direction dir = sortOrder.matches("desc")
                ? SortConfigDTO.Direction.Descending
                : SortConfigDTO.Direction.Ascending;

        AggregationConfigDTO.Builder aggregationConfigBuilder = AggregationConfigDTO.Builder.builder()
                .columnPivots(Collections.emptyList())
                .series(ImmutableList.of(
                        SeriesDTO.Builder.create()
                                .config(SeriesConfigDTO.empty())
                                .function("count()").build()
                ))
                .sort(ImmutableList.of(PivotSortConfig.create(field, dir)));

        final Optional<String> query = config.getOptionalString("query");
        query.ifPresent(s -> widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString(s).build()));
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
                            .build())
                    .id(randomUUIDProvider.get())
                    .build());
        }
        return result;
    }

    private List<WidgetEntity> createSearchResultCount() throws InvalidRangeParametersException {
        final boolean trend = config.getBoolean("trend");
        final boolean lowerIsBetter = config.getBoolean("lower_is_better");
        final AggregationConfigDTO widgetConfig = AggregationConfigDTO.Builder.builder()
                .series(ImmutableList.of(createSeriesDTO("count", "")))
                .visualization("numeric")
                .visualizationConfig(NumberVisualizationConfigDTO.Builder.builder()
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

        return ImmutableList.of(widgetEntityBuilder.build());
    }

    private List<WidgetEntity> createQuickValueHistogramWidgets() throws InvalidRangeParametersException {
        final List<WidgetEntity> result = new ArrayList<>(2);
        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder();

        final String stackedFields = config.getOptionalString("stacked_fields").orElse("");
        final String field = config.getString("field");
        final int limit = config.getOptionalInteger("limit").orElse(5);
        final String sortOrder = config.getOptionalString("sort_order").orElse("desc");

        SortConfigDTO.Direction dir = sortOrder.matches("desc")
                ? SortConfigDTO.Direction.Descending
                : SortConfigDTO.Direction.Ascending;

        AggregationConfigDTO.Builder aggregationConfigBuilder = AggregationConfigDTO.Builder.builder()
                .columnPivots(Collections.emptyList())
                .series(ImmutableList.of(
                        SeriesDTO.Builder.create()
                                .config(SeriesConfigDTO.empty())
                                .function("count()").build()
                ))
                .sort(ImmutableList.of(PivotSortConfig.create(field, dir)));

        final Optional<String> query = config.getOptionalString("query");
        query.ifPresent(s -> widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString(s).build()));

        result.add(widgetEntityBuilder.config(
                aggregationConfigBuilder.visualization("bar")
                        .visualizationConfig(BarVisualizationConfigDTO.Builder.builder()
                                .barmode(BarVisualizationConfigDTO.BarMode.stack)
                                .build())
                        .rollup(false)
                        .rowPivots(Collections.singletonList(
                                PivotDTO.Builder.builder()
                                        .field("timestamp")
                                        .type("time")
                                        .config(TimeHistogramConfigDTO.Builder.builder().interval(AutoIntervalDTO.Builder.builder().build()).build())
                                        .build()
                        ))
                        .columnPivots(genPivotForPie(field, stackedFields, limit))
                        .build())
                .id(randomUUIDProvider.get())
                .build());
        return result;
    }

    private List<WidgetEntity> createMapWidget() throws InvalidRangeParametersException {
        final String field = config.getString("field");
        final PivotDTO fieldPivot = PivotDTO.Builder.builder()
                .type("values")
                .config(ValueConfigDTO.Builder.builder().build())
                .field(field)
                .build();
        final AggregationConfigDTO widgetConfig = AggregationConfigDTO.Builder.builder()
                .series(ImmutableList.of(createSeriesDTO("count", "")))
                .visualization("map")
                .rowPivots(ImmutableList.of(fieldPivot))
                .columnPivots(Collections.emptyList())
                .sort(Collections.emptyList())
                .build();
        final WidgetEntity.Builder widgetEntityBuilder = aggregationWidgetBuilder()
                .config(widgetConfig);

        final Optional<String> query = config.getOptionalString("query");
        query.ifPresent(s -> widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString(s).build()));

        return ImmutableList.of(widgetEntityBuilder.build());
    }

    private List<PivotDTO> genPivotForPie(String field, String stackedFields, int limit) {
        final PivotDTO fieldPivot = PivotDTO.Builder.builder()
                .type("values")
                .field(field)
                .config(ValueConfigDTO.Builder.builder().limit(limit).build())
                .build();
        final List<PivotDTO> rowPivots = new ArrayList<>(stackedFieldPivots(stackedFields));
        rowPivots.add(fieldPivot);
        return rowPivots;
    }

    @SuppressWarnings("UnstableApiUsage")
    private List<PivotDTO> stackedFieldPivots(String fieldNames) {
        return Strings.isNullOrEmpty(fieldNames)
                ? Collections.emptyList()
                : Splitter.on(",")
                .splitToList(fieldNames)
                .stream()
                .map(fieldName -> PivotDTO.Builder.builder()
                        .field(fieldName)
                        .type("values")
                        .config(ValueConfigDTO.Builder.builder().limit(15).build())
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
            case "std_deviation":
                function = "stddev";
                break;
            default: {
                function = valueType;
                break;
            }
        }
        return SeriesDTO.Builder.create().config(SeriesConfigDTO.empty()).function(function + "(" + field + ")").build();
    }
}
