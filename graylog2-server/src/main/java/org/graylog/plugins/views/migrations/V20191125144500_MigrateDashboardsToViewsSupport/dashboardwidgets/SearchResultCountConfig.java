package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ElasticsearchQueryString;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.NumberVisualizationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;

import java.util.Collections;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class SearchResultCountConfig implements WidgetConfig {
    public abstract Boolean lowerIsBetter();

    public abstract Boolean trend();

    private Series series() {
        return Series.createFromString("count()").build();
    }

    private String visualization() {
        return "numeric";
    }

    @Override
    public Set<ViewWidget> toViewWidgets() {
        return Collections.singleton(
                ViewWidget.builder()
                        .query(ElasticsearchQueryString.create(query()))
                        .timerange(timerange())
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
                        )
                        .build()
        );
    }

    @JsonCreator
    static SearchResultCountConfig create(
            @JsonProperty("lower_is_better") Boolean lowerIsBetter,
            @JsonProperty("trend") Boolean trend,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_SearchResultCountConfig(
                query,
                timerange,
                lowerIsBetter,
                trend
        );
    }
}
