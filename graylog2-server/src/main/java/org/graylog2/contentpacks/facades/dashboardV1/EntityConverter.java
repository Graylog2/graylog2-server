package org.graylog2.contentpacks.facades.dashboardV1;

import com.eaio.uuid.UUID;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.TimeUnitInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;
import org.graylog.plugins.views.search.views.Position;
import org.graylog.plugins.views.search.views.Titles;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;
import org.graylog.plugins.views.search.views.WidgetPositionDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.PivotDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.SeriesConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.SeriesDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog2.contentpacks.model.entities.AbsoluteRangeEntity;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.DashboardWidgetEntity;
import org.graylog2.contentpacks.model.entities.KeywordRangeEntity;
import org.graylog2.contentpacks.model.entities.RelativeRangeEntity;
import org.graylog2.contentpacks.model.entities.SearchEntity;
import org.graylog2.contentpacks.model.entities.TimeRangeEntity;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.ViewStateEntity;
import org.graylog2.contentpacks.model.entities.WidgetEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityConverter {
    final Map<String, TimeUnitInterval> intervalMap = new ImmutableMap.Builder<String, TimeUnitInterval>()
            .put("minute", TimeUnitInterval.builder().timeunit("1m").build())
            .put("hour", TimeUnitInterval.builder().timeunit("1h").build())
            .put("day", TimeUnitInterval.builder().timeunit("1d").build())
            .put("week", TimeUnitInterval.builder().timeunit("1w").build())
            .put("month", TimeUnitInterval.builder().timeunit("1M").build())
            .put("quarter", TimeUnitInterval.builder().timeunit("3M").build())
            .put("year", TimeUnitInterval.builder().timeunit("1y").build())
            .build();

    private DashboardEntity dashboardEntity;
    private Map<String, ValueReference> parameters;

    public EntityConverter(DashboardEntity dashboardEntity,
                           Map<String, ValueReference> parameters) {
       this.dashboardEntity = dashboardEntity;
       this.parameters = parameters;
    }

    public ViewEntity convert() {
        final String queryId = new UUID().toString();

        final Map<String, WidgetPositionDTO> widgetPositionMap = new HashMap<>();
        final Map<DashboardWidgetEntity, List<WidgetEntity>> widgets = new HashMap<>();
        for (DashboardWidgetEntity widgetEntity : dashboardEntity.widgets()) {
            widgets.put(widgetEntity, createWidget(widgetEntity, parameters));
            if (widgetEntity.position().isPresent()) {
                DashboardWidgetEntity.Position position = widgetEntity.position().get();
                widgetPositionMap.put(widgetEntity.id().asString(parameters),
                        WidgetPositionDTO.builder()
                                .col(Position.fromInt(position.col().asInteger(parameters)))
                                .row(Position.fromInt(position.row().asInteger(parameters)))
                                .height(Position.fromInt(position.height().asInteger(parameters)))
                                .width(Position.fromInt(position.width().asInteger(parameters)))
                                .build());
            }
        }

        final Map<String, String> widgetTitles = new HashMap<>();
        final Map<String, Map<String, String>> titlesMap = new HashMap<>(1);
        titlesMap.put(Titles.KEY_WIDGETS, widgetTitles);
        final Map<String, Set<String>> widgetMapping = new HashMap<>();
        final Set<SearchType> searchTypes = new HashSet<>();
        for (Map.Entry<DashboardWidgetEntity, List<WidgetEntity>> widgetEntityListEntry: widgets.entrySet()) {
            DashboardWidgetEntity dashboardWidgetEntity = widgetEntityListEntry.getKey();
            widgetEntityListEntry.getValue().forEach(widgetEntity -> {
                widgetTitles.put(widgetEntity.id(), dashboardWidgetEntity.description().asString(parameters));
                final List<SearchType> currentSearchTypes;
                try {
                    currentSearchTypes = createSearchType(dashboardWidgetEntity, widgetEntity, parameters);
                } catch (InvalidRangeParametersException e) {
                    throw new IllegalArgumentException("The provided entity does not have a valid TimeRange", e);
                }
                searchTypes.addAll(currentSearchTypes);
                widgetMapping.put(widgetEntity.id(),
                        currentSearchTypes.stream().map(SearchType::id).collect(Collectors.toSet()));
            });
        }

        SearchEntity searchEntity;
        try {
            searchEntity = createSearchEntity(queryId, searchTypes);
        } catch (InvalidRangeParametersException e) {
            throw new IllegalArgumentException("The provided entity does not have a valid TimeRange", e);
        }

        final ViewStateEntity viewStateEntity = ViewStateEntity.builder()
                .widgets(widgets.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()))
                .titles(Titles.of(titlesMap))
                .widgetMapping(widgetMapping)
                .widgetPositions(widgetPositionMap)
                .build();
        final Map<String, ViewStateEntity> viewStateEntityMap = new HashMap<>(1);
        viewStateEntityMap.put(queryId, viewStateEntity);

        return ViewEntity.builder()
                .search(searchEntity)
                .state(viewStateEntityMap)
                .title(dashboardEntity.title())
                .properties(Collections.emptySet())
                .description(dashboardEntity.description())
                .requires(Collections.emptyMap())
                .summary(ValueReference.of("Converted Dashboard"))
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .type(ViewEntity.Type.DASHBOARD)
                .build();
    }

    private List<WidgetEntity> createWidget(DashboardWidgetEntity widget,
                                            Map<String, ValueReference> parameters) {
        final Map<String, Object> config = ReferenceMapUtils.toValueMap(widget.configuration(), parameters);
        final String type = widget.type().asString(parameters);

        try {
            switch (type) {
                case "SEARCH_RESULT_CHART": {
                    return createHistogramWidget(widget, config);
                }
                case "FIELD_CHART": {
                    return createFieldChartWidget(widget, config);
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

    private List<WidgetEntity> createFieldChartWidget(DashboardWidgetEntity widget, Map<String, Object> config) {

    }

    private List<WidgetEntity> createHistogramWidget(DashboardWidgetEntity widget, Map<String, Object> config) throws InvalidRangeParametersException {
        final WidgetConfigDTO widgetConfigDTO = AggregationConfigDTO.builder()
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
        final WidgetEntity.Builder widgetEntityBuilder = WidgetEntity.builder()
                .type(AggregationConfigDTO.NAME)
                .config(widgetConfigDTO);
            widgetEntityBuilder.id(widget.id().asString(parameters));

        final TimeRangeEntity timeRangeEntity = widget.timeRange();
        final TimeRange timeRange = convertTimeRange(timeRangeEntity, parameters);
        widgetEntityBuilder.timerange(timeRange);

        final Object query = config.get("query");
        if (query instanceof String) {
            widgetEntityBuilder.query(ElasticsearchQueryString.builder().queryString((String) query).build());
        }

        final ImmutableSet.Builder<String> streams = new ImmutableSet.Builder<>();
        final Object streamId = config.get("stream_id");
        if (streamId instanceof String) {
            streams.add((String) streamId);
            widgetEntityBuilder.streams(streams.build());
        }
        return ImmutableList.of(widgetEntityBuilder.build());
    }

    private SearchEntity createSearchEntity(String queryId, Set<SearchType> searchTypes)
            throws InvalidRangeParametersException {
        final Query query = Query.builder()
                .id(queryId)
                .searchTypes(searchTypes)
                .timerange(RelativeRange.create(300))
                .query(ElasticsearchQueryString.builder().queryString("").build())
                .build();
        return SearchEntity.builder()
                .requires(ImmutableMap.of())
                .parameters(ImmutableSet.of())
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .queries(ImmutableSet.of(query))
                .build();
    }

    private List<SearchType> createSearchType(DashboardWidgetEntity widget,
                                              WidgetEntity widgetEntity,
                                              Map<String, ValueReference> parameters) throws InvalidRangeParametersException {
        final Map<String, Object> config = ReferenceMapUtils.toValueMap(widget.configuration(), parameters);
        final String type = widget.type().asString(parameters);

        List<Pivot.Builder> pivotBuilder;
        switch(type) {
            case "SEARCH_RESULT_CHART": {
                pivotBuilder = createHistogram(config);
                break;
            }
            case "FIELD_CHART": {
                pivotBuilder = createFieldChart(config);
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

        final TimeRangeEntity timeRangeEntity = widget.timeRange();
        final TimeRange timeRange = convertTimeRange(timeRangeEntity, parameters);
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

    private Pivot.Builder createPivotBuilder(Map<String, Object> config) {
        final ImmutableSet.Builder<String> streams = new ImmutableSet.Builder<>();
        final Pivot.Builder pivotBuilder = Pivot.builder()
                .id(new UUID().toString())
                .rollup(true);

        final Object streamId = config.get("stream_id");
        if (streamId instanceof String) {
            streams.add((String) streamId);
            pivotBuilder.streams(streams.build());
        }
        return pivotBuilder;
    }

    final TimeRange convertTimeRange(TimeRangeEntity timeRangeEntity, Map<String, ValueReference> parameters) throws InvalidRangeParametersException {
        final String type = timeRangeEntity.type().type().asString(parameters);
        switch (type) {
            case "absolute": {
                final AbsoluteRangeEntity absoluteRangeEntity = (AbsoluteRangeEntity) timeRangeEntity;
                final String from = absoluteRangeEntity.from().asString(parameters);
                final String to = absoluteRangeEntity.to().asString(parameters);
                return AbsoluteRange.create(from, to);
            }
            case "relative": {
                final RelativeRangeEntity relativeRangeEntity = (RelativeRangeEntity) timeRangeEntity;
                final int range = relativeRangeEntity.range().asInteger(parameters);
                return RelativeRange.create(range);
            }
            case "keyword": {
                final KeywordRangeEntity keywordRangeEntity = (KeywordRangeEntity) timeRangeEntity;
                final String keyword = keywordRangeEntity.keyword().asString(parameters);
                return KeywordRange.create(keyword);
            }
            default: {
                throw new IllegalArgumentException(
                        "The provided entity does not have a valid TimeRange type: " + type);
            }
        }
    }

    private List<Pivot.Builder> createHistogram(Map<String, Object> config) {
        Pivot.Builder pivotBuilder = createPivotBuilder(config);
        final String interval = (String) config.get("interval");
        pivotBuilder.series(ImmutableList.of(Count.builder().id("count()").build()));
        pivotBuilder.rowGroups(ImmutableList.of(Time.builder().field("timestamp").interval(intervalMap.get(interval)).build()))
                .id(new UUID().toString())
                .rollup(true);
        return ImmutableList.of(pivotBuilder);
    }

    private List<Pivot.Builder> createFieldChart(Map<String, Object> config) {
        Pivot.Builder pivotBuilder = createPivotBuilder(config);
        final String interval = (String) config.get("interval");
        String valueType = (String) config.get("valuetype");
        String field = (String) config.get("field");
        pivotBuilder.series(ImmutableList.of(createSeries(valueType, field)));
        pivotBuilder.rowGroups(ImmutableList.of(Time.builder().field("timestamp").interval(intervalMap.get(interval)).build()))
                .id(new UUID().toString())
                .rollup(true);
        return ImmutableList.of(pivotBuilder);
    }

    private List<Pivot.Builder> createStackedChart(Map<String, Object> config) {
        return ImmutableList.of();
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
}
