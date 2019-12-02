package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SeriesConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeHistogramConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class SearchResultChartConfig extends WidgetConfigBase implements WidgetConfigWithQueryAndStreams {
    public abstract String interval();

    private String visualization() { return "bar"; }

    private Series series() {
        return Series.buildFromString("count()").config(SeriesConfig.builder().name("Messages").build()).build();
    }

    @Override
    public Set<ViewWidget> toViewWidgets(RandomUUIDProvider randomUUIDProvider) {
        return Collections.singleton(
                createViewWidget(randomUUIDProvider.get())
                        .config(
                                AggregationConfig.builder()
                                        .rowPivots(Collections.singletonList(
                                                Pivot.timeBuilder()
                                                        .field(TIMESTAMP_FIELD)
                                                        .config(TimeHistogramConfig.builder()
                                                                .interval(ApproximatedAutoInterval.of(interval(), timerange()))
                                                                .build())
                                                        .build()
                                        ))
                                        .series(Collections.singletonList(series()))
                                        .visualization(visualization())
                                        .build()
                        )
                        .build()
        );
    }

    @JsonCreator
    static SearchResultChartConfig create(
            @JsonProperty("interval") String interval,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("stream_id") @Nullable String streamId
    ) {
        return new AutoValue_SearchResultChartConfig(
                timerange,
                query,
                Optional.ofNullable(streamId),
                interval
        );
    }
}
