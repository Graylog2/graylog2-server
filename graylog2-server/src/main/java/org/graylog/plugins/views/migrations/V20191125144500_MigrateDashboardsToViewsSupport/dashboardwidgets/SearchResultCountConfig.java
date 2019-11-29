package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
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
public abstract class SearchResultCountConfig extends WidgetConfigBase implements WidgetConfigWithQueryAndStreams {
    private static final String NUMERIC_VISUALIZATION = "numeric";

    public abstract Boolean lowerIsBetter();

    public abstract Boolean trend();

    public abstract Optional<String> streamId();

    private Series series() {
        return countSeries();
    }

    private String visualization() {
        return NUMERIC_VISUALIZATION;
    }

    @Override
    public Set<ViewWidget> toViewWidgets(RandomUUIDProvider randomUUIDProvider) {
        return Collections.singleton(
                createViewWidget(randomUUIDProvider.get())
                        .config(
                                AggregationConfig.builder()
                                        .series(Collections.singletonList(series()))
                                        .visualization(visualization())
                                        .visualizationConfig(
                                                NumberVisualizationConfig.builder()
                                                        .trend(true)
                                                        .trendPreference(lowerIsBetter()
                                                                ? NumberVisualizationConfig.TrendPreference.LOWER
                                                                : NumberVisualizationConfig.TrendPreference.HIGHER)
                                                        .build()
                                        )
                                        .build()
                        ).build()
        );
    }

    @JsonCreator
    static SearchResultCountConfig create(
            @JsonProperty("lower_is_better") Boolean lowerIsBetter,
            @JsonProperty("trend") Boolean trend,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("stream_id") @Nullable String streamId
    ) {
        return new AutoValue_SearchResultCountConfig(
                timerange,
                query,
                lowerIsBetter,
                trend,
                Optional.ofNullable(streamId)
        );
    }
}
