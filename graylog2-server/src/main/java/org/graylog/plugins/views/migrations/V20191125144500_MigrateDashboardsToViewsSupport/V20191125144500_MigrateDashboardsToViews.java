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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.google.common.collect.Sets;
import org.bson.types.ObjectId;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class V20191125144500_MigrateDashboardsToViews extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20191125144500_MigrateDashboardsToViews.class);
    private final DashboardsService dashboardsService;
    private final SearchService searchService;
    private final ViewService viewService;
    private final ClusterConfigService clusterConfigService;
    private final RandomObjectIdProvider randomObjectIdProvider;
    private final RandomUUIDProvider randomUUIDProvider;

    @Inject
    public V20191125144500_MigrateDashboardsToViews(
            DashboardsService dashboardsService,
            SearchService searchService,
            ViewService viewService,
            ClusterConfigService clusterConfigService,
            RandomObjectIdProvider randomObjectIdProvider,
            RandomUUIDProvider randomUUIDProvider) {
        this.dashboardsService = dashboardsService;
        this.searchService = searchService;
        this.viewService = viewService;
        this.clusterConfigService = clusterConfigService;
        this.randomObjectIdProvider = randomObjectIdProvider;
        this.randomUUIDProvider = randomUUIDProvider;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-11-25T14:45:00Z");
    }

    @Override
    public void upgrade() {
        if (hasBeenRunSuccessfully()) {
            LOG.debug("Migration already completed.");
            return;
        }

        final Set<String> dashboardIdToViewId = new HashSet<>();
        final Consumer<String> recordMigratedDashboardIds = dashboardIdToViewId::add;
        final Map<String, Set<String>> widgetIdMigrationMapping = new HashMap<>();
        final Consumer<Map<String, Set<String>>> recordMigratedWidgetIds = widgetIdMigrationMapping::putAll;

        final Map<View, Search> newViews = this.dashboardsService.streamAll()
                .sorted(Comparator.comparing(Dashboard::id))
                .map(dashboard -> migrateDashboard(dashboard, recordMigratedDashboardIds, recordMigratedWidgetIds))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        writeViews(newViews);

        final MigrationCompleted migrationCompleted = MigrationCompleted.create(dashboardIdToViewId, widgetIdMigrationMapping);

        writeMigrationCompleted(migrationCompleted);
    }

    private void writeViews(Map<View, Search> newViews) {
        final List<ObjectId> writtenSearches = new ArrayList<>(newViews.size());
        final List<ObjectId> writtenViews = new ArrayList<>(newViews.size());

        try {
            newViews.forEach((view, search) -> {
                writtenSearches.add(searchService.save(search));
                writtenViews.add(viewService.save(view));
            });
        } catch (Exception e) {
            LOG.warn("Exception caught while writing new views to database, rolling back: ", e);
            writtenSearches.forEach(searchService::remove);
            writtenViews.forEach(viewService::remove);
            throw e;
        }
    }

    private void writeMigrationCompleted(MigrationCompleted migrationCompleted) {
        this.clusterConfigService.write(migrationCompleted);
    }

    private boolean hasBeenRunSuccessfully() {
        return clusterConfigService.get(MigrationCompleted.class) != null;
    }

    private Map.Entry<View, Search> migrateDashboard(Dashboard dashboard,
                                  Consumer<String> recordMigratedDashboardIds,
                                  Consumer<Map<String, Set<String>>> recordMigratedWidgetMap) {
        final Map<String, Set<String>> migratedWidgetIds = new HashMap<>(dashboard.widgets().size());
        final BiConsumer<String, String> recordMigratedWidgetIds = (String before, String after) -> migratedWidgetIds
                .merge(before, Collections.singleton(after), Sets::union);

        final Map<String, String> newWidgetTitles = new HashMap<>(dashboard.widgets().size());
        final BiConsumer<String, String> recordWidgetTitle = newWidgetTitles::put;

        final Set<ViewWidget> newViewWidgets = dashboard.widgets().stream()
                .sorted(Comparator.comparing(Widget::id))
                .flatMap(widget -> migrateWidget(widget, recordMigratedWidgetIds, recordWidgetTitle)
                        .stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

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
        final Query newQuery = Query.create(randomUUIDProvider.get(), RelativeRange.create(300),"", newSearchTypes);
        final Set<Query> newQueries = Collections.singleton(newQuery);
        final Search newSearch = Search.create(randomObjectIdProvider.get(), newQueries, dashboard.creatorUserId(), createdAt);

        final ViewState newViewState = ViewState.create(
                Titles.ofWidgetTitles(newWidgetTitles).withQueryTitle(dashboard.title()),
                newViewWidgets,
                newWidgetMapping,
                newViewWidgetPositions
        );

        final View newView = View.create(
                dashboard.id(),
                View.Type.DASHBOARD,
                dashboard.title(),
                "This dashboard was migrated automatically.",
                dashboard.description(),
                newSearch.id(),
                Collections.singletonMap(newQuery.id(), newViewState),
                Optional.ofNullable(dashboard.creatorUserId()),
                createdAt
        );

        recordMigratedDashboardIds.accept(dashboard.id());
        recordMigratedWidgetMap.accept(migratedWidgetIds);

        return new AbstractMap.SimpleEntry<>(newView, newSearch);
    }

    private Set<SearchType> createSearchType(ViewWidget viewWidget, BiConsumer<String, String> recordWidgetMapping) {
        final Set<SearchType> searchTypes = viewWidget.toSearchTypes(randomUUIDProvider);
        searchTypes.forEach(searchType -> recordWidgetMapping.accept(viewWidget.id(), searchType.id()));
        return searchTypes;
    }

    private Set<ViewWidget> migrateWidget(Widget widget,
                                     BiConsumer<String, String> recordMigratedWidgetIds,
                                     BiConsumer<String, String> recordWidgetTitle) {
        final Set<ViewWidget> viewWidgets = widget.toViewWidgets(this.randomUUIDProvider);

        viewWidgets.forEach(viewWidget -> {
            recordWidgetTitle.accept(viewWidget.id(), widget.description());
            recordMigratedWidgetIds.accept(widget.id(), viewWidget.id());
        });

        return viewWidgets;
    }

    private Map<String, ViewWidgetPosition> migrateWidgetPositions(Dashboard dashboard, Map<String, Set<String>> migratedWidgetIds, Set<ViewWidget> viewWidgets) {
        return dashboard.widgetPositions().entrySet().stream()
                .flatMap(entry -> {
                    final WidgetPosition widgetPosition = entry.getValue();
                    final Set<String> viewWidgetIds = migratedWidgetIds.get(entry.getKey());
                    if (viewWidgetIds == null) {
                        return Stream.empty();
                    }
                    final Set<ViewWidget> newViewWidgets = viewWidgetIds.stream()
                            .map(viewWidgetId -> viewWidgets
                                .stream()
                                .filter(viewWidget -> viewWidget.id().equals(viewWidgetId))
                                .findFirst()
                                    .orElse(null)
                            ).filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    final Optional<Widget> dashboardWidget = dashboard.widgets().stream()
                            .filter(widget -> widget.id().equals(entry.getKey()))
                            .findFirst();
                    return dashboardWidget.map(widget -> widget.config().toViewWidgetPositions(newViewWidgets, widgetPosition)
                            .entrySet()
                            .stream())
                            .orElse(Stream.empty());
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
