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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.eaio.uuid.UUID;
import com.google.common.collect.Sets;
import org.graylog2.migrations.Migration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class V20191125144500_MigrateDashboardsToViews extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20191125144500_MigrateDashboardsToViews.class);
    private final DashboardsService dashboardsService;

    @Inject
    public V20191125144500_MigrateDashboardsToViews(DashboardsService dashboardsService) {
        this.dashboardsService = dashboardsService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-11-25T14:45:00Z");
    }

    @Override
    public void upgrade() {
        final Map<String, String> dashboardIdToViewId = new HashMap<>();
        final BiFunction<String, String, String> recordMigratedDashboardIds = dashboardIdToViewId::put;
        final Map<String, Set<String>> widgetIdMigrationMapping = new HashMap<>();
        final Consumer<Map<String, Set<String>>> recordMigratedWidgetIds = widgetIdMigrationMapping::putAll;

        final Set<View> newViews = this.dashboardsService.streamAll()
                .map(dashboard -> migrateDashboard(dashboard, recordMigratedDashboardIds, recordMigratedWidgetIds))
                .collect(Collectors.toSet());

        final MigrationCompleted migrationCompleted = MigrationCompleted.create(dashboardIdToViewId, widgetIdMigrationMapping);
    }

    private View migrateDashboard(Dashboard dashboard,
                                  BiFunction<String, String, String> recordMigratedDashboardIds,
                                  Consumer<Map<String, Set<String>>> recordMigratedWidgetMap) {
        final Map<String, Set<String>> migratedWidgetIds = new HashMap<>(dashboard.widgets().size());
        final BiConsumer<String, String> recordMigratedWidgetIds = (String before, String after) -> migratedWidgetIds
                .merge(before, Collections.singleton(after), Sets::union);

        final Map<String, String> newWidgetTitles = new HashMap<>(dashboard.widgets().size());
        final BiConsumer<String, String> recordWidgetTitle = newWidgetTitles::put;

        final Set<ViewWidget> newViewWidgets = dashboard.widgets().stream()
                .flatMap(widget -> migrateWidget(widget, recordMigratedWidgetIds, recordWidgetTitle).stream())
                .collect(Collectors.toSet());

        final Map<String, ViewWidgetPosition> newViewWidgetPositions = migrateWidgetPositions(
                dashboard,
                Collections.unmodifiableMap(migratedWidgetIds),
                Collections.unmodifiableSet(newViewWidgets)
        );

        final Map<String, Set<String>> newWidgetMapping = new HashMap<>(newViewWidgets.size());
        final BiConsumer<String, String> recordWidgetMapping = (String viewWidgetId, String searchTypeId) -> newWidgetMapping
                .merge(viewWidgetId, Collections.singleton(searchTypeId), Sets::union);

        final DateTime createdAt = dashboard.createdAt();

        final Set<SearchType> newSearchTypes = newViewWidgets.stream()
                .flatMap(viewWidget -> createSearchType(viewWidget, recordWidgetMapping).stream())
                .collect(Collectors.toSet());
        final Query newQuery = Query.create(newId(), RelativeRange.create(300),"", newSearchTypes);
        final Set<Query> newQueries = Collections.singleton(newQuery);
        final Search newSearch = Search.create(newId(), newQueries, dashboard.creatorUserId(), createdAt);

        final ViewState newViewState = ViewState.create(
                Titles.ofWidgetTitles(newWidgetTitles).withQueryTitle(dashboard.title()),
                newViewWidgets,
                newWidgetMapping,
                newViewWidgetPositions
        );

        final View newView = View.create(
                newId(),
                View.Type.DASHBOARD,
                dashboard.title(),
                "",
                dashboard.description(),
                newSearch.id(),
                Collections.singletonMap(newId(), newViewState),
                Optional.ofNullable(dashboard.creatorUserId()),
                createdAt
        );

        recordMigratedDashboardIds.apply(dashboard.id(), newView.id());
        recordMigratedWidgetMap.accept(migratedWidgetIds);

        return newView;
    }

    private Set<SearchType> createSearchType(ViewWidget viewWidget, BiConsumer<String, String> recordWidgetMapping) {
        final Set<SearchType> searchTypes = viewWidget.toSearchTypes();
        searchTypes.forEach(searchType -> recordWidgetMapping.accept(viewWidget.id(), searchType.id()));
        return searchTypes;
    }

    private Set<ViewWidget> migrateWidget(Widget widget,
                                     BiConsumer<String, String> recordMigratedWidgetIds,
                                     BiConsumer<String, String> recordWidgetTitle) {
        final Set<ViewWidget> viewWidgets = widget.toViewWidgets();

        viewWidgets.forEach(viewWidget -> {
            recordWidgetTitle.accept(viewWidget.id(), widget.description());
            recordMigratedWidgetIds.accept(widget.id(), viewWidget.id());
        });

        return viewWidgets;
    }

    private Stream<Map.Entry<String, ViewWidgetPosition>> migrateWidgetPosition(WidgetPosition widgetPosition, Widget widget, Set<ViewWidget> viewWidgets) {
        final ViewWidgetPosition newPosition = ViewWidgetPosition.builder()
                .col(widgetPosition.col())
                .row(widgetPosition.row())
                .height(widgetPosition.height())
                .width(widgetPosition.width())
                .build();
        return Stream.of(new AbstractMap.SimpleEntry<>(viewWidgets.iterator().next().id(), newPosition));
    }

    private Map<String, ViewWidgetPosition> migrateWidgetPositions(Dashboard dashboard, Map<String, Set<String>> migratedWidgetIds, Set<ViewWidget> viewWidgets) {
        return dashboard.widgetPositions().entrySet().stream()
                .flatMap(entry -> {
                    // TODO: Also handle Quickvalues widget being destructured into two widgets
                    final WidgetPosition widgetPosition = entry.getValue();
                    final Set<String> viewWidgetIds = migratedWidgetIds.get(entry.getKey());
                    final Set<ViewWidget> newViewWidgets = viewWidgetIds.stream()
                            .map(viewWidgetId -> viewWidgets
                                .stream()
                                .filter(viewWidget -> viewWidget.id().equals(viewWidgetId))
                                .findFirst()
                                    .orElse(null)
                            ).filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    final Widget dashboardWidget = dashboard.widgets().stream()
                            .filter(widget -> widget.id().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Unable to find widget with id <" + entry.getKey()));
                    return migrateWidgetPosition(widgetPosition, dashboardWidget, newViewWidgets);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String newId() {
        return new UUID().toString();
    }
}
