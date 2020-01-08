package org.graylog2.contentpacks.facades.dashboardV1;

import com.eaio.uuid.UUID;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.ApproximatedAutoIntervalFactory;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AreaVisualizationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Interpolation;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.LineVisualizationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeHistogramConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.VisualizationConfig;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityConverter {

    private DashboardEntity dashboardEntity;
    private Map<String, ValueReference> parameters;

    public EntityConverter(DashboardEntity dashboardEntity,
                           Map<String, ValueReference> parameters) {
       this.dashboardEntity = dashboardEntity;
       this.parameters = parameters;
    }

    public ViewEntity convert() {
        final String queryId = new UUID().toString();

        final Map<String, WidgetPositionDTO> widgetPositionMap = dashboardEntity.positionMap(parameters);
        final Map<DashboardWidgetEntity, List<WidgetEntity>> widgets = new HashMap<>();
        for (DashboardWidgetEntity widgetEntity : dashboardEntity.widgets()) {
            widgets.put(widgetEntity, widgetEntity.convert(parameters));
        }
        final Map<String, Map<String, String>> titles = DashboardEntity.widetTitles(widgets, parameters);

        final Map<String, Set<String>> widgetMapping = new HashMap<>();
        final Set<SearchType> searchTypes = new HashSet<>();
        for (Map.Entry<DashboardWidgetEntity, List<WidgetEntity>> widgetEntityListEntry: widgets.entrySet()) {
            DashboardWidgetEntity dashboardWidgetEntity = widgetEntityListEntry.getKey();
            widgetEntityListEntry.getValue().forEach(widgetEntity -> {
                final List<SearchType> currentSearchTypes;
                try {
                    currentSearchTypes = dashboardWidgetEntity.createSearchType();
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
                .titles(Titles.of(titles))
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
}
