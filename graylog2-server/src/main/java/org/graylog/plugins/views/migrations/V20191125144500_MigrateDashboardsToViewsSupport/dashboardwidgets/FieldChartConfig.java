package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ElasticsearchQueryString;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Interval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeHistogramConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeUnitInterval;

import java.util.Collections;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties({"rangeType", "relative", "from", "to", "keyword"})
public abstract class FieldChartConfig implements WidgetConfig {
    private static String TIMESTAMP_FIELD = "timestamp";

    public abstract String valuetype();

    public abstract String renderer();

    public abstract String interpolation();

    public abstract String field();

    public abstract String interval();

    private String mapFunction(String function) {
        switch (function) {
            case "total": return "sum";
            case "mean": return "avg";
            case "std_deviation": return "stddev";
            case "cardinality": return "card";
            case "count":
            case "variance":
            case "min":
            case "max":
            case "sum":
                return function;
        }
        throw new RuntimeException("Unable to map function from field chart widget: " + function);
    }

    private String visualization() {
        switch (renderer()) {
            case "bar":
            case "line":
                return renderer();
            case "area":
                // TODO: Do something about
                throw new RuntimeException("Area chart is unsupported");
            case "scatterplot": return "scatter";
        }
        throw new RuntimeException("Unable to map renderer to visualization: " + renderer());
    }

    private Series series() {
        return Series.createFromString(mapFunction(valuetype()) + "(" + field() + ")").build();
    }

    private Interval timestampInterval() {
        switch (interval()) {
            case "minute": return TimeUnitInterval.builder().unit(TimeUnitInterval.IntervalUnit.MINUTES).value(1).build();
        }
        throw new RuntimeException("Unable to map interval: " + interval());
    }

    public Set<ViewWidget> toViewWidgets() {
        return Collections.singleton(
                ViewWidget.builder()
                        .query(ElasticsearchQueryString.create(query()))
                        .timerange(timerange())
                        .config(
                                AggregationConfig.builder()
                                        .rowPivots(Collections.singletonList(
                                                Pivot.timeBuilder()
                                                .field(TIMESTAMP_FIELD)
                                                .config(TimeHistogramConfig.builder().interval(timestampInterval()).build())
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
    static FieldChartConfig create(
            @JsonProperty("valuetype") String valuetype,
            @JsonProperty("renderer") String renderer,
            @JsonProperty("interpolation") String interpolation,
            @JsonProperty("field") String field,
            @JsonProperty("interval") String interval,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_FieldChartConfig(
                query,
                timerange,
                valuetype,
                renderer,
                interpolation,
                field,
                interval
        );
    }
}
