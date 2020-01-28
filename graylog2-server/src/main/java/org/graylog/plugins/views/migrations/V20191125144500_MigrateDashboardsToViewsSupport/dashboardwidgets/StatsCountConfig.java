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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Widget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.NumberVisualizationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StatsCountConfig extends WidgetConfigBase implements WidgetConfigWithQueryAndStreams {
    private static final String NUMERIC_VISUALIZATION = "numeric";

    public abstract String field();
    public abstract String statsFunction();
    public abstract Optional<Boolean> lowerIsBetter();
    public abstract Optional<Boolean> trend();
    public abstract Optional<String> streamId();

    private Series series() {
        return Series.create(mapStatsFunction(statsFunction()), field());
    }

    @Override
    public Set<ViewWidget> toViewWidgets(Widget widget, RandomUUIDProvider randomUUIDProvider) {
        return Collections.singleton(createAggregationWidget(randomUUIDProvider.get())
                .config(AggregationConfig.builder()
                        .series(Collections.singletonList(series()))
                        .visualization(NUMERIC_VISUALIZATION)
                        .visualizationConfig(
                                NumberVisualizationConfig.builder()
                                        .trend(trend().orElse(false))
                                        .trendPreference(lowerIsBetter().orElse(false)
                                                ? NumberVisualizationConfig.TrendPreference.LOWER
                                                : NumberVisualizationConfig.TrendPreference.HIGHER)
                                        .build()
                        )
                        .build())
                .build());
    }

    @JsonCreator
    static StatsCountConfig create(
            @JsonProperty("field") String field,
            @JsonProperty("stats_function") String statsFunction,
            @JsonProperty("lower_is_better") Boolean lowerIsBetter,
            @JsonProperty("trend") Boolean trend,
            @JsonProperty("stream_id") @Nullable String streamId,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_StatsCountConfig(
                timerange,
                query,
                field,
                statsFunction,
                Optional.ofNullable(lowerIsBetter),
                Optional.ofNullable(trend),
                Optional.ofNullable(streamId)
        );
    }
}
