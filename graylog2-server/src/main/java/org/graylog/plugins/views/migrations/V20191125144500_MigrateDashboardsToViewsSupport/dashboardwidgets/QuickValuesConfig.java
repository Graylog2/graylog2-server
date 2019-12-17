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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidgetPosition;
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

@AutoValue
@JsonAutoDetect
public abstract class QuickValuesConfig extends WidgetConfigBase implements WidgetConfigWithQueryAndStreams {
    private static final String VISUALIZATION_PIE = "pie";
    private static final String VISUALIZATION_TABLE = "table";

    public abstract String field();
    public abstract Boolean showDataTable();
    public abstract Boolean showPieChart();
    public abstract Integer limit();
    public abstract Integer dataTableLimit();
    public abstract String sortOrder();
    public abstract String stackedFields();

    private Series series() {
        return countSeries();
    }

    private List<Pivot> stackedFieldPivots() {
        return Strings.isNullOrEmpty(stackedFields())
                ? Collections.emptyList()
                : Splitter.on(",")
                .splitToList(stackedFields())
                .stream()
                .map(fieldName -> valuesPivotForField(fieldName, 15))
                .collect(Collectors.toList());
    }

    private Pivot piePivot() {
        return valuesPivotForField(field(), limit());
    }

    private Pivot dataTablePivot() {
        return valuesPivotForField(field(), dataTableLimit());
    }

    private SortConfig.Direction order() {
        return sortDirection(sortOrder());
    }

    private SortConfig sort() {
        return SeriesSortConfig.create(field(), order());
    }

    @Override
    public Set<ViewWidget> toViewWidgets(RandomUUIDProvider randomUUIDProvider) {
        final ImmutableSet.Builder<ViewWidget> viewWidgets = ImmutableSet.builder();
        final AggregationConfig.Builder baseConfigBuilder = AggregationConfig.builder()
                .sort(Collections.singletonList(sort()))
                .series(Collections.singletonList(series()));
        if (showPieChart()) {
            final ViewWidget pieChart = createViewWidget(randomUUIDProvider.get())
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
            final ViewWidget dataTable = createViewWidget(randomUUIDProvider.get())
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

        final ViewWidget pieWidget = viewWidgets.stream()
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

        final ViewWidget tableWidget = viewWidgets.stream()
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
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("field") String field,
            @JsonProperty("show_data_table") Boolean showDataTable,
            @JsonProperty("show_pie_chart") Boolean showPieChart,
            @JsonProperty("limit") Integer limit,
            @JsonProperty("data_table_limit") Integer dataTableLimit,
            @JsonProperty("sort_order") String sortOrder,
            @JsonProperty("stacked_fields") String stackedFields,
            @JsonProperty("stream_id") @Nullable String streamId
    ) {
        return new AutoValue_QuickValuesConfig(
                timerange,
                query,
                Optional.ofNullable(streamId),
                field,
                showDataTable,
                showPieChart,
                limit,
                dataTableLimit,
                sortOrder,
                stackedFields
        );
    }
}
