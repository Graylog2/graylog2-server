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
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Widget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AutoInterval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.BarVisualizationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SeriesSortConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SortConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeHistogramConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class QuickValuesHistogramConfig extends WidgetConfigBase implements WidgetConfigWithQueryAndStreams {
    private static final String BAR_VISUALIZATION = "bar";

    public abstract String field();
    public abstract Integer limit();
    public abstract String sortOrder();
    public abstract String stackedFields();
    public abstract Optional<String> interval();

    private Series series() {
        return countSeries();
    }

    private SortConfig.Direction order() {
        return sortDirection(sortOrder());
    }

    private SortConfig sort() {
        return SeriesSortConfig.create(field(), order());
    }


    private List<Pivot> stackedFieldPivots() {
        final Pivot fieldPivot = valuesPivotForField(field(), limit());
        final List<Pivot> stackedFieldsPivots = Strings.isNullOrEmpty(stackedFields())
                ? Collections.emptyList()
                : Splitter.on(",")
                .splitToList(stackedFields())
                .stream()
                .map(fieldName -> valuesPivotForField(fieldName, limit()))
                .collect(Collectors.toList());
        return ImmutableList.<Pivot>builder()
                .add(fieldPivot)
                .addAll(stackedFieldsPivots)
                .build();
    }

    @Override
    public Set<ViewWidget> toViewWidgets(Widget widget, RandomUUIDProvider randomUUIDProvider) {
        return Collections.singleton(
                createAggregationWidget(randomUUIDProvider.get())
                        .config(
                                AggregationConfig.builder()
                                        .rowPivots(Collections.singletonList(
                                                Pivot.timeBuilder()
                                                        .field(TIMESTAMP_FIELD)
                                                        .config(TimeHistogramConfig.builder()
                                                                .interval(
                                                                        interval()
                                                                                .map(interval -> ApproximatedAutoIntervalFactory.of(interval, timerange()))
                                                                                .orElse(AutoInterval.create())
                                                                ).build())
                                                        .build()
                                        ))
                                        .columnPivots(stackedFieldPivots())
                                        .series(Collections.singletonList(series()))
                                        .sort(Collections.singletonList(sort()))
                                        .visualization(BAR_VISUALIZATION)
                                        .visualizationConfig(
                                                BarVisualizationConfig.builder()
                                                        .barmode(BarVisualizationConfig.BarMode.stack)
                                                        .build()
                                        )
                                .build()
                        ).build()
        );
    }

    @JsonCreator
    static QuickValuesHistogramConfig create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("field") String field,
            @JsonProperty("limit") Integer limit,
            @JsonProperty("sort_order") String sortOrder,
            @JsonProperty("stacked_fields") String stackedFields,
            @JsonProperty("interval") @Nullable String interval,
            @JsonProperty("stream_id") @Nullable String streamId
    ) {
        return new AutoValue_QuickValuesHistogramConfig(
                timerange,
                Strings.nullToEmpty(query),
                Optional.ofNullable(streamId),
                field,
                limit,
                sortOrder,
                stackedFields,
                Optional.ofNullable(interval)
        );
    }
}
