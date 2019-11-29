package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeHistogramConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.VisualizationConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties({"rangeType", "relative", "from", "to", "keyword"})
public abstract class FieldChartConfig extends WidgetConfigBase implements WidgetConfigWithQueryAndStreams {
    public abstract String valuetype();

    public abstract String renderer();

    public abstract String interpolation();

    public abstract String field();

    public abstract String interval();

    private String visualization() {
        return mapRendererToVisualization(renderer());
    }

    private Series series() {
        return Series.create(mapStatsFunction(valuetype()), field());
    }

    public Set<ViewWidget> toViewWidgets(RandomUUIDProvider randomUUIDProvider) {
        final AggregationConfig.Builder configBuilder = AggregationConfig.builder()
                .rowPivots(Collections.singletonList(
                        Pivot.timeBuilder()
                                .field(TIMESTAMP_FIELD)
                                .config(TimeHistogramConfig.builder().interval(timestampInterval(interval())).build())
                                .build()
                ))
                .series(Collections.singletonList(series()))
                .visualization(visualization());
        return Collections.singleton(
                ViewWidget.builder()
                        .id(randomUUIDProvider.get())
                        .query(query())
                        .timerange(timerange())
                        .config(visualizationConfig().map(configBuilder::visualizationConfig).orElse(configBuilder).build())
                        .build()
        );
    }

    private Optional<VisualizationConfig> visualizationConfig() {
        return createVisualizationConfig(renderer(), interpolation());
    }

    @JsonCreator
    static FieldChartConfig create(
            @JsonProperty("valuetype") String valuetype,
            @JsonProperty("renderer") String renderer,
            @JsonProperty("interpolation") String interpolation,
            @JsonProperty("field") String field,
            @JsonProperty("interval") String interval,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("stream_id") @Nullable String streamId
    ) {
        return new AutoValue_FieldChartConfig(
                timerange,
                query,
                Optional.ofNullable(streamId),
                valuetype,
                renderer,
                interpolation,
                field,
                interval
        );
    }
}
