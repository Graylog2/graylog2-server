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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.AggregationWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidgetPosition;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Widget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.WidgetPosition;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SeriesSortConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SortConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class QuickValuesConfig extends WidgetConfigBase implements WidgetConfigWithQueryAndStreams {
    private static final String VISUALIZATION_PIE = "pie";
    private static final String VISUALIZATION_TABLE = "table";
    private static final int DEFAULT_LIMIT = 15;
    private static final String DEFAULT_SORT_ORDER = "desc";

    public abstract String field();
    public abstract Boolean showDataTable();
    public abstract Boolean showPieChart();
    public abstract Optional<Integer> limit();
    public abstract Optional<Integer> dataTableLimit();
    public abstract Optional<String> sortOrder();
    public abstract Optional<String> stackedFields();

    private Series series() {
        return countSeries();
    }

    private List<Pivot> stackedFieldPivots() {
        final String fieldNames = stackedFields().orElse("");
        return Strings.isNullOrEmpty(fieldNames)
                ? Collections.emptyList()
                : Splitter.on(",")
                .splitToList(fieldNames)
                .stream()
                .map(fieldName -> valuesPivotForField(fieldName, 15))
                .collect(Collectors.toList());
    }

    private Pivot piePivot() {
        return valuesPivotForField(field(), limit().orElse(DEFAULT_LIMIT));
    }

    private Pivot dataTablePivot() {
        return valuesPivotForField(field(), dataTableLimit().orElse(DEFAULT_LIMIT));
    }

    private SortConfig.Direction order() {
        return sortDirection(sortOrder().orElse(DEFAULT_SORT_ORDER));
    }

    private SortConfig sort() {
        return SeriesSortConfig.create(series().function(), order());
    }

    @Override
    public Set<ViewWidget> toViewWidgets(Widget widget, RandomUUIDProvider randomUUIDProvider) {
        final ImmutableSet.Builder<ViewWidget> viewWidgets = ImmutableSet.builder();
        final AggregationConfig.Builder baseConfigBuilder = AggregationConfig.builder()
                .sort(Collections.singletonList(sort()))
                .series(Collections.singletonList(series()));
        if (showPieChart()) {
            final ViewWidget pieChart = createAggregationWidget(randomUUIDProvider.get())
                    .config(
                            baseConfigBuilder
                                    .rowPivots(ImmutableList.<Pivot>builder().add(piePivot()).addAll(stackedFieldPivots()).build())
                                    .visualization(VISUALIZATION_PIE)
                                    .build()
                    )
                    .build();
            viewWidgets.add(pieChart);
        }
        if (showDataTable()) {
            final ViewWidget dataTable = createAggregationWidget(randomUUIDProvider.get())
                    .config(
                            baseConfigBuilder
                                    .rowPivots(ImmutableList.<Pivot>builder().add(dataTablePivot()).addAll(stackedFieldPivots()).build())
                                    .visualization(VISUALIZATION_TABLE)
                                    .build()
                    )
                    .build();
            viewWidgets.add(dataTable);
        }

        return viewWidgets.build();
    }

    @Override
    public Map<String, ViewWidgetPosition> toViewWidgetPositions(Set<ViewWidget> viewWidgets, WidgetPosition widgetPosition) {
        if (viewWidgets.size() == 1) {
            return super.toViewWidgetPositions(viewWidgets, widgetPosition);
        }

        final AggregationWidget pieWidget = viewWidgets.stream()
                .filter(viewWidget -> viewWidget instanceof AggregationWidget)
                .map(viewWidget -> (AggregationWidget)viewWidget)
                .filter(viewWidget -> viewWidget.config().visualization().equals(VISUALIZATION_PIE))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to retrieve pie widget again."));

        final int newPieHeight = (int) Math.ceil(widgetPosition.height() / 2d);
        final ViewWidgetPosition piePosition = ViewWidgetPosition.builder()
                .col(widgetPosition.col())
                .row(widgetPosition.row())
                .height(newPieHeight)
                .width(widgetPosition.width())
                .build();

        final AggregationWidget tableWidget = viewWidgets.stream()
                .filter(viewWidget -> viewWidget instanceof AggregationWidget)
                .map(viewWidget -> (AggregationWidget)viewWidget)
                .filter(viewWidget -> viewWidget.config().visualization().equals(VISUALIZATION_TABLE))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to retrieve table widget again."));

        final ViewWidgetPosition tablePosition = ViewWidgetPosition.builder()
                .col(widgetPosition.col())
                .row(widgetPosition.row() + newPieHeight)
                .height(widgetPosition.height() - newPieHeight)
                .width(widgetPosition.width())
                .build();

        return ImmutableMap.of(
                pieWidget.id(), piePosition,
                tableWidget.id(), tablePosition
        );
    }

    @JsonCreator
    static QuickValuesConfig create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("field") String field,
            @JsonProperty("show_data_table") @Nullable Boolean showDataTable,
            @JsonProperty("show_pie_chart") @Nullable Boolean showPieChart,
            @JsonProperty("limit") Integer limit,
            @JsonProperty("data_table_limit") Integer dataTableLimit,
            @JsonProperty("sort_order") String sortOrder,
            @JsonProperty("stacked_fields") @Nullable String stackedFields,
            @JsonProperty("stream_id") @Nullable String streamId
    ) {
        return new AutoValue_QuickValuesConfig(
                timerange,
                Strings.nullToEmpty(query),
                Optional.ofNullable(streamId),
                field,
                (showDataTable == null || !showDataTable) && (showPieChart == null || !showPieChart)
                        ? true
                        : firstNonNull(showDataTable, false),
                firstNonNull(showPieChart, false),
                Optional.ofNullable(limit),
                Optional.ofNullable(dataTableLimit),
                Optional.ofNullable(sortOrder),
                Optional.ofNullable(stackedFields)
        );
    }
}
