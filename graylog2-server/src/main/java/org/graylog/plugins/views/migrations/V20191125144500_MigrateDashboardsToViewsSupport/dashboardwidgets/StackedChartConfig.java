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
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidgetPosition;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.WidgetPosition;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.VisualizationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
public abstract class StackedChartConfig extends WidgetConfigBase implements WidgetConfig {
    private static final Logger LOG = LoggerFactory.getLogger(StackedChartConfig.class);

    public abstract String interval();
    public abstract String renderer();
    public abstract String interpolation();
    public abstract List<StackedSeries> series();
    public abstract Optional<String> streamId();

    @Override
    public Set<ViewWidget> toViewWidgets(RandomUUIDProvider randomUUIDProvider) {
        final Map<String, List<StackedSeries>> seriesByQuery = series().stream()
                .collect(Collectors.groupingBy(StackedSeries::query));
        final AggregationConfig.Builder configBuilderTemplate = AggregationConfig.builder()
                .rowPivots(timestampPivot(interval()))
                .visualization(mapRendererToVisualization(renderer()));

        if (seriesByQuery.size() > 1) {
            // TODO: Insert URL to changelog/upgrade notes with helpful advice
            LOG.warn("Migrating dashboards to views: Encountered a stacked chart widget containing multiple distinct " +
                    "queries, splitting up into separate widgets. Read more about it here: " +
                    "https://github.com/Graylog2/graylog2-server/blob/master/UPGRADING.rst#upgrading-to-graylog-32x");
        }

        return seriesByQuery.entrySet().stream()
                .map(entry -> {
                    final String query = entry.getKey();
                    final List<Series> series = entry.getValue().stream()
                            .map(s -> Series.create(mapStatsFunction(s.statisticalFunction()), s.field()))
                            .collect(Collectors.toList());

                    final AggregationConfig.Builder configBuilder = configBuilderTemplate.series(series);

                    final ViewWidget.Builder viewWidgetBuilder = ViewWidget.builder()
                            .id(randomUUIDProvider.get())
                            .timerange(timerange())
                            .query(query)
                            .config(visualizationConfig().map(configBuilder::visualizationConfig).orElse(configBuilder).build());
                    return streamId().map(streamId -> viewWidgetBuilder.streams(Collections.singleton(streamId))).orElse(viewWidgetBuilder).build();
                }).collect(Collectors.toSet());
    }

    @Override
    public Map<String, ViewWidgetPosition> toViewWidgetPositions(Set<ViewWidget> viewWidgets, WidgetPosition widgetPosition) {
        if (viewWidgets.size() == 1) {
            return super.toViewWidgetPositions(viewWidgets, widgetPosition);
        }

        final List<ViewWidget> widgetList = new ArrayList<>(viewWidgets);
        final ViewWidget first = widgetList.get(0);

        final ViewWidgetPosition firstPosition = ViewWidgetPosition.builder()
                .col(widgetPosition.col())
                .row(widgetPosition.row())
                .height(widgetPosition.height())
                .width(widgetPosition.width())
                .build();

        return Collections.singletonMap(first.id(), firstPosition);
    }

    private Optional<VisualizationConfig> visualizationConfig() {
        return createVisualizationConfig(renderer(), interpolation());
    }

    @JsonCreator
    public static StackedChartConfig create(
            @JsonProperty("timerange") TimeRange timeRange,
            @JsonProperty("interval") String interval,
            @JsonProperty("renderer") String renderer,
            @JsonProperty("interpolation") String interpolation,
            @JsonProperty("series") List<StackedSeries> series,
            @JsonProperty("stream_id") @Nullable String streamId
    ) {
        return new AutoValue_StackedChartConfig(timeRange, interval, renderer, interpolation, series, Optional.ofNullable(streamId));
    }
}
