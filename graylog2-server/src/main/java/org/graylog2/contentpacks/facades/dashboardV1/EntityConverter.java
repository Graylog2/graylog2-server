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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.views.Titles;
import org.graylog.plugins.views.search.views.WidgetPositionDTO;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.DashboardWidgetEntity;
import org.graylog2.contentpacks.model.entities.QueryEntity;
import org.graylog2.contentpacks.model.entities.SearchEntity;
import org.graylog2.contentpacks.model.entities.SearchTypeEntity;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.ViewStateEntity;
import org.graylog2.contentpacks.model.entities.WidgetEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
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

    private DashboardEntity dashboardEntity;
    private Map<String, ValueReference> parameters;
    private RandomUUIDProvider randomUUIDProvider;

    public EntityConverter(DashboardEntity dashboardEntity,
                           Map<String, ValueReference> parameters,
                           RandomUUIDProvider randomUUIDProvider) {
       this.dashboardEntity = dashboardEntity;
       this.parameters = parameters;
       this.randomUUIDProvider = randomUUIDProvider;
    }

    public ViewEntity convert() {
        final String queryId = randomUUIDProvider.get();

        final Map<DashboardWidgetEntity, List<WidgetEntity>> widgets = new HashMap<>();
        for (DashboardWidgetEntity widgetEntity : dashboardEntity.widgets()) {
            widgets.put(widgetEntity, DashboardWidgetConverter.convert(widgetEntity, parameters,
                    randomUUIDProvider));
        }
        final Map<String, WidgetPositionDTO> widgetPositionMap = DashboardEntity.positionMap(parameters, widgets);
        final  Titles titles = DashboardEntity.widgetTitles(widgets, parameters);

        final Map<String, Set<String>> widgetMapping = new HashMap<>();
        final Set<SearchTypeEntity> searchTypes = new HashSet<>();
        for (Map.Entry<DashboardWidgetEntity, List<WidgetEntity>> widgetEntityListEntry: widgets.entrySet()) {
            widgetEntityListEntry.getValue().forEach(widgetEntity -> {
                final List<SearchTypeEntity> currentSearchTypes;
                currentSearchTypes = widgetEntity.createSearchTypeEntity(randomUUIDProvider);
                searchTypes.addAll(currentSearchTypes);
                widgetMapping.put(widgetEntity.id(),
                        currentSearchTypes.stream().map(SearchTypeEntity::id).collect(Collectors.toSet()));
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
                .titles(titles)
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

    private SearchEntity createSearchEntity(String queryId, Set<SearchTypeEntity> searchTypes)
            throws InvalidRangeParametersException {
        final QueryEntity query = QueryEntity.builder()
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
