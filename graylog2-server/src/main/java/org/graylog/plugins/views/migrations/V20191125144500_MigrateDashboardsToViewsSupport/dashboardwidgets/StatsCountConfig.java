package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.NumberVisualizationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class StatsCountConfig extends WidgetConfigBase implements WidgetConfig {
    private static final String NUMERIC_VISUALIZATION = "numeric";

    public abstract String field();
    public abstract String statsFunction();
    public abstract Boolean lowerIsBetter();
    public abstract Boolean trend();
    public abstract Optional<String> streamId();

    private Series series() {
        return Series.create(mapStatsFunction(statsFunction()), field());
    }

    @Override
    public Set<ViewWidget> toViewWidgets() {
        final ViewWidget.Builder viewWidgetBuilder = createViewWidget()
                .config(AggregationConfig.builder()
                        .series(Collections.singletonList(series()))
                        .visualization(NUMERIC_VISUALIZATION)
                        .visualizationConfig(
                                NumberVisualizationConfig.builder()
                                        .trend(true)
                                        .trendPreference(lowerIsBetter()
                                                ? NumberVisualizationConfig.TrendPreference.LOWER
                                                : NumberVisualizationConfig.TrendPreference.HIGHER)
                                        .build()
                        )
                        .build());
        return Collections.singleton(
                streamId()
                        .map(Collections::singleton)
                        .map(viewWidgetBuilder::streams)
                        .orElse(viewWidgetBuilder)
                        .build()
        );
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
                query,
                timerange,
                field,
                statsFunction,
                lowerIsBetter,
                trend,
                Optional.ofNullable(streamId)
        );
    }
}
