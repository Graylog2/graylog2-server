package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
public abstract class StackedChartConfig extends WidgetConfigBase implements WidgetConfig {
    public abstract String interval();
    public abstract String renderer();
    public abstract String interpolation();
    public abstract List<StackedSeries> series();

    @Override
    public Set<ViewWidget> toViewWidgets() {
        final List<Series> series = series().stream()
                .map(s -> Series.create(mapStatsFunction(s.statisticalFunction()), s.field()))
                .collect(Collectors.toList());

        if (series().stream().map(StackedSeries::query).distinct().count() > 1) {
            throw new RuntimeException("Stacked charts with differing queries are not yet supported!");
        }
        return Collections.singleton(ViewWidget.builder()
                .timerange(timerange())
                .config(AggregationConfig.builder()
                        .rowPivots(timestampPivot(interval()))
                        .series(series)
                        .visualization(mapRendererToVisualization(renderer()))
                        .build())
                .build()
        );
    }

    @JsonCreator
    public static StackedChartConfig create(
            @JsonProperty("timerange") TimeRange timeRange,
            @JsonProperty("interval") String interval,
            @JsonProperty("renderer") String renderer,
            @JsonProperty("interpolation") String interpolation,
            @JsonProperty("series") List<StackedSeries> series
    ) {
        return new AutoValue_StackedChartConfig(timeRange, interval, renderer, interpolation, series);
    }
}
